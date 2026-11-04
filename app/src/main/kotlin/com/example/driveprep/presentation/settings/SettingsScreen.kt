package com.example.driveprep.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.driveprep.domain.model.DarkMode
import com.example.driveprep.domain.model.QuestionFontSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val imagePack by viewModel.imagePack.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒绝也不影响下载,只是收不到进度/完成通知 */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "选择应用的深色模式；“跟随系统”时与系统显示设置保持一致。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    DarkMode.SYSTEM to "跟随系统",
                    DarkMode.LIGHT to "白天模式",
                    DarkMode.DARK to "夜间模式"
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.darkMode == mode,
                        onClick = { viewModel.setDarkMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = modes.size
                        ),
                        // 选中态用主题浅蓝主色,不用默认的绿色 secondaryContainer
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            activeBorderColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            Text(
                text = "题目字号",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val sizes = listOf(
                    QuestionFontSize.LARGE to "大",
                    QuestionFontSize.MEDIUM to "中",
                    QuestionFontSize.SMALL to "小"
                )
                sizes.forEachIndexed { index, (size, label) ->
                    SegmentedButton(
                        selected = settings.questionFontSize == size,
                        onClick = { viewModel.setQuestionFontSize(size) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = sizes.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            activeBorderColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            Text(
                text = "答题反馈",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "这些选项只影响练习界面的答案揭晓方式，不会改变判分规则。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingSwitchRow(
                title = "立即显示对错",
                description = "单选和判断题选择后立即揭晓；多选题确认答案后揭晓。",
                checked = settings.showImmediateFeedback,
                onCheckedChange = viewModel::setImmediateFeedback
            )
            SettingSwitchRow(
                title = "显示答题技巧",
                description = "揭晓答案后显示题库中的答题技巧。",
                checked = settings.showAnswerSkill,
                onCheckedChange = viewModel::setShowAnswerSkill
            )
            SettingSwitchRow(
                title = "显示答案解析",
                description = "揭晓答案后显示详细解析和法规说明。",
                checked = settings.showAnswerExplain,
                onCheckedChange = viewModel::setShowAnswerExplain
            )

            Text(
                text = "离线图片包",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "把全部题目图片下载到应用缓存目录(约 35 MB),完成后做题不再需要联网。图片只存放在应用私有目录,不会出现在系统相册。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                imagePack.isDownloading -> {
                    LinearProgressIndicator(
                        progress = {
                            if (imagePack.total > 0) {
                                imagePack.ready.toFloat() / imagePack.total
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        // 底色与未激活 Switch 边框同源;去掉 M3 默认的末端小圆点
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    Text(
                        text = buildString {
                            append("下载中 ${imagePack.ready}/${imagePack.total}")
                            if (imagePack.failed > 0) append(" · 失败 ${imagePack.failed} 张")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::cancelImageDownload) {
                        Text("取消下载")
                    }
                }

                imagePack.isReady -> {
                    Text(
                        text = "已离线可用 · ${imagePack.ready} 张 · ${imagePack.sizeBytes / 1024 / 1024} MB",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = viewModel::clearImages) {
                        Text("清除图片包")
                    }
                }

                else -> {
                    if (imagePack.failed > 0) {
                        Text(
                            text = "上次有 ${imagePack.failed} 张下载失败,重新下载会自动跳过已完成的图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(onClick = {
                        // Android 13+ 需要运行时通知权限才能看到进度/完成通知
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                        viewModel.downloadImages()
                    }) {
                        Text(
                            if (imagePack.ready > 0) {
                                "继续下载(${imagePack.ready}/${imagePack.total})"
                            } else {
                                "下载图片包"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // 未选中:蓝滑块 + 灰轨道线框;选中:默认白滑块 + 蓝轨道
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
