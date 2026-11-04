package com.example.driveprep.presentation.exam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.driveprep.domain.model.ExamScore

@Composable
fun ExamResultScreen(
    score: ExamScore,
    hasReviewableWrong: Boolean,
    onReviewWrongQuestions: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Pass/Fail text
        Text(
            text = if (score.passed) "恭喜通过！" else "未通过",
            style = MaterialTheme.typography.displaySmall,
            color = if (score.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        // Score card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 分数:通过用主色,未通过用红色强调
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${score.score}",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (score.passed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "分",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                HorizontalDivider()

                ScoreDetailRow(label = "正确题数", value = "${score.correct}/${score.total}")
                // 答错与未答分开:未答题只扣分,不计入错题本
                val answeredWrong = score.wrongQuestions.count { it.userAnswer.isNotEmpty() }
                val unanswered = score.wrongQuestions.count { it.userAnswer.isEmpty() }
                ScoreDetailRow(label = "答错题数", value = "$answeredWrong")
                if (unanswered > 0) {
                    ScoreDetailRow(label = "未答题数", value = "$unanswered")
                }
                ScoreDetailRow(label = "用时", value = formatTime(score.timeUsed))
                ScoreDetailRow(
                    label = "正确率",
                    value = "${(score.correct.toFloat() / score.total * 100).toInt()}%"
                )
            }
        }

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasReviewableWrong) {
                Button(
                    onClick = onReviewWrongQuestions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("查看错题", style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("再来一次", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("返回首页", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun ScoreDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


private fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}分${remainingSeconds}秒"
}
