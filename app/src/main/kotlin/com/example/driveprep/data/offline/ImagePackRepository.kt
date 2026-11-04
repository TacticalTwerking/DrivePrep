package com.example.driveprep.data.offline

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.driveprep.data.local.dao.QuestionDao
import com.example.driveprep.data.mapper.cdnUrlToHttps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class ImagePackStatus {
    /** 初始/仅刷新过库存 */
    IDLE,

    /** 下载中 */
    DOWNLOADING,

    /** 全部就绪 */
    COMPLETED,

    /** 结束但有失败张数,重新下载自动续传 */
    PARTIAL,

    /** 手动取消,已下载部分保留 */
    CANCELLED
}

/** 离线图片包状态 */
data class ImagePackState(
    val status: ImagePackStatus = ImagePackStatus.IDLE,
    /** 已就绪(本地已有文件)张数 */
    val ready: Int = 0,
    /** 图片总张数 */
    val total: Int = 0,
    /** 失败张数(下载中为本次实时值,结束后为 total - 磁盘实有) */
    val failed: Int = 0,
    /** 图片包当前占用体积 */
    val sizeBytes: Long = 0
) {
    val isDownloading: Boolean get() = status == ImagePackStatus.DOWNLOADING

    val isReady: Boolean get() = status == ImagePackStatus.COMPLETED ||
        (!isDownloading && total > 0 && ready >= total && failed == 0)
}

/**
 * 离线图片包:把题库全部图片下载到应用缓存目录(cacheDir,不出现在系统相册),
 * 加载时优先用本地文件,没有才走网络。
 *
 * - 幂等:已下载的图片自动跳过,中断/失败后重新点下载即续传
 * - 临时文件以 .tmp 落盘、完成后原子改名,半成品不会被当作已下载
 * - 下载通过前台服务(ImageDownloadService)护航:切后台/锁屏不被杀,并显示进度与完成通知
 * - cacheDir 被系统/清理工具清空后,回设置重新下载即可
 */
@Singleton
class ImagePackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionDao: QuestionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startMutex = Mutex()
    private var downloadJob: Job? = null

    private val _state = MutableStateFlow(ImagePackState())
    val state: StateFlow<ImagePackState> = _state.asStateFlow()

    private val packDir: File get() = File(context.cacheDir, PACK_DIR_NAME)

    /** 刷新库存状态(不启动下载);下载中不覆盖进度 */
    suspend fun refreshStatus() = withContext(Dispatchers.IO) {
        if (_state.value.isDownloading) return@withContext
        val total = questionDao.getAllImageUrls().size
        val files = listPackFiles()
        _state.value = ImagePackState(
            ready = files.size,
            total = total,
            sizeBytes = files.sumOf { it.length() }
        )
    }

    /** 开始下载,已在下载中返回 false */
    fun startDownload(): Boolean {
        if (downloadJob?.isActive == true) return false
        // 先切到下载态,避免服务启动时观察到 IDLE 而立刻误判结束
        _state.value = _state.value.copy(status = ImagePackStatus.DOWNLOADING)
        // 前台服务:保证切后台/锁屏后下载继续,并提供进度与完成通知
        val serviceIntent = Intent(context, ImageDownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        downloadJob = scope.launch { runDownload() }
        return true
    }

    /** 取消下载;进行中的请求最多还有 4 个在途,完成后状态自动回落为 CANCELLED */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    /** 清除图片包 */
    fun clearPack() {
        cancelDownload()
        scope.launch {
            withContext(Dispatchers.IO) {
                packDir.deleteRecursively()
            }
            refreshStatus()
        }
    }

    /** 本地已有该图则返回文件绝对路径,否则返回 null(加载侧回退到原 URL) */
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return fileFor(url).takeIf { it.exists() }?.absolutePath
    }

    private suspend fun runDownload() {
        startMutex.withLock {
            val urls = questionDao.getAllImageUrls()
                .filterNotNull()
                .map { cdnUrlToHttps(it) }
            if (urls.isEmpty()) {
                _state.value = ImagePackState()
                return
            }

            packDir.mkdirs()
            val semaphore = Semaphore(DOWNLOAD_CONCURRENCY)
            val ready = AtomicInteger(0)
            val failed = AtomicInteger(0)
            _state.value = ImagePackState(
                status = ImagePackStatus.DOWNLOADING,
                total = urls.size
            )

            var wasCancelled = false
            try {
                coroutineScope {
                    urls.map { url ->
                        launch {
                            semaphore.withPermit {
                                val file = fileFor(url)
                                if (file.exists()) {
                                    ready.incrementAndGet()
                                } else {
                                    // 瞬时网络抖动自动重试一次
                                    val ok = fetchToFile(url, file) || fetchToFile(url, file)
                                    if (ok) {
                                        ready.incrementAndGet()
                                    } else {
                                        failed.incrementAndGet()
                                    }
                                }
                                _state.update {
                                    it.copy(ready = ready.get(), failed = failed.get())
                                }
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                wasCancelled = true
            } finally {
                // 取消/完成都以磁盘为准回填最终状态
                val files = listPackFiles()
                val missing = (urls.size - files.size).coerceAtLeast(0)
                _state.value = ImagePackState(
                    status = when {
                        wasCancelled -> ImagePackStatus.CANCELLED
                        missing == 0 -> ImagePackStatus.COMPLETED
                        else -> ImagePackStatus.PARTIAL
                    },
                    ready = files.size,
                    total = urls.size,
                    failed = missing,
                    sizeBytes = files.sumOf { it.length() }
                )
            }
        }
    }

    private fun fetchToFile(url: String, target: File): Boolean {
        val tmp = File(packDir, target.name + TMP_SUFFIX)
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) return false
            conn.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (tmp.length() > 0) tmp.renameTo(target) else false
        } catch (_: Exception) {
            false
        } finally {
            tmp.delete()
            conn?.disconnect()
        }
    }

    private fun listPackFiles(): List<File> {
        return packDir.listFiles()?.filter { !it.name.endsWith(TMP_SUFFIX) }.orEmpty()
    }

    private fun fileFor(url: String): File {
        return File(packDir, sha1Hex(cdnUrlToHttps(url)) + ".img")
    }

    companion object {
        fun sha1Hex(input: String): String {
            return MessageDigest.getInstance("SHA-1")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }

        private const val PACK_DIR_NAME = "question_images"
        private const val TMP_SUFFIX = ".tmp"
        private const val DOWNLOAD_CONCURRENCY = 4
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}
