package com.example.driveprep.data.offline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.driveprep.MainActivity
import com.example.driveprep.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 离线图片包下载的前台服务:
 * - 护航下载不被系统/清理工具杀掉(切后台、锁屏都继续)
 * - 常驻通知展示下载进度(低优先级、静音,带取消按钮)
 * - 结束(完成/部分失败/取消)时在"结果"通道发一条有声通知并自停
 */
@AndroidEntryPoint
class ImageDownloadService : Service() {

    @Inject
    lateinit var imagePackRepository: ImagePackRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startInForeground(buildProgressNotification(imagePackRepository.state.value))
        serviceScope.launch {
            imagePackRepository.state.collect { state ->
                if (state.isDownloading) {
                    updateNotification(buildProgressNotification(state))
                } else {
                    notifyFinished(state)
                    ServiceCompat.stopForeground(
                        this@ImageDownloadService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE
                    )
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            imagePackRepository.cancelDownload()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildProgressNotification(state: ImagePackState): Notification {
        val percent = if (state.total > 0) state.ready * 100 / state.total else 0
        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NOTIFICATION_ACCENT)
            .setContentTitle("正在下载离线图片包")
            .setContentText("${state.ready} / ${state.total} · $percent%")
            .setProgress(state.total, state.ready, state.total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .addAction(
                R.drawable.ic_notification,
                "取消",
                cancelPendingIntent()
            )
            .build()
    }

    private fun notifyFinished(state: ImagePackState) {
        val (title, text) = when (state.status) {
            ImagePackStatus.CANCELLED ->
                "已取消下载" to "已下载的部分会保留,回设置随时可继续"
            ImagePackStatus.PARTIAL ->
                "离线图片包下载完成" to "失败 ${state.failed} 张,已就绪 ${state.ready}/${state.total};回设置重新下载即可续传"
            else ->
                "离线图片包已就绪" to "${state.ready} 张 · ${state.sizeBytes / 1024 / 1024} MB,做题已可完全离线"
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NOTIFICATION_ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    private fun updateNotification(notification: Notification) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingIntent(): PendingIntent {
        val intent = Intent(this, ImageDownloadService::class.java).setAction(ACTION_CANCEL)
        return PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // 进度通道:低优先级静音,不打扰做题
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROGRESS,
                "下载进度",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        // 结果通道:默认优先级,完成时有提示音
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RESULT,
                "下载结果",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    companion object {
        private const val CHANNEL_PROGRESS = "image_pack_progress"
        private const val CHANNEL_RESULT = "image_pack_result"
        private const val NOTIFICATION_ID = 1
        private const val FINISHED_NOTIFICATION_ID = 2
        private const val ACTION_CANCEL = "com.example.driveprep.action.CANCEL_IMAGE_PACK"
        private val NOTIFICATION_ACCENT = 0xFF38BDF8.toInt()
    }
}
