package com.example.driveprep.presentation.exam.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OptionItem(
    optionId: String,
    optionText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCorrect: Boolean? = null,
    showResult: Boolean = false
) {
    val showAsWrong = showResult && isSelected && isCorrect == false

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = tween(100)
    )
    
    val containerColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect == true -> MaterialTheme.colorScheme.secondaryContainer
            showAsWrong -> MaterialTheme.colorScheme.errorContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200)
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect == true -> MaterialTheme.colorScheme.secondary
            showAsWrong -> MaterialTheme.colorScheme.error
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(200)
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            showResult && isCorrect == true -> MaterialTheme.colorScheme.onSecondaryContainer
            showAsWrong -> MaterialTheme.colorScheme.onErrorContainer
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200)
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = if (isSelected || showResult) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 选项标识
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (showResult && isCorrect == true) {
                    MaterialTheme.colorScheme.secondary
                } else if (showAsWrong) {
                    MaterialTheme.colorScheme.error
                } else if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = optionId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (showResult && isCorrect == true) {
                            MaterialTheme.colorScheme.onSecondary
                        } else if (showAsWrong) {
                            MaterialTheme.colorScheme.onError
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // 选项文字
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (showResult && isCorrect == true) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "正确答案",
                    tint = MaterialTheme.colorScheme.secondary
                )
            } else if (showAsWrong) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "错误答案",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
