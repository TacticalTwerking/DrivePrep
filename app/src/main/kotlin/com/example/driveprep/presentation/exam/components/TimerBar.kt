package com.example.driveprep.presentation.exam.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimerBar(
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    val totalTime = 45 * 60 * 1000L
    val progress = (timeRemaining.toFloat() / totalTime).coerceIn(0f, 1f)
    val minutes = (timeRemaining / 1000 / 60).toInt()
    val seconds = (timeRemaining / 1000 % 60).toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "剩余时间",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    minutes < 5 -> MaterialTheme.colorScheme.error
                    minutes < 15 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                minutes < 5 -> MaterialTheme.colorScheme.error
                minutes < 15 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}
