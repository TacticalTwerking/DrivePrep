package com.example.driveprep.presentation.exam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.QuestionType

private val IMAGE_MAX_HEIGHT = 240.dp

@Composable
fun QuestionCard(
    question: Question,
    selectedAnswers: Set<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isReviewMode: Boolean = false,
    revealed: Boolean = false,
    allowReveal: Boolean = false,
    showAnswerSkill: Boolean = false,
    showAnswerExplain: Boolean = false,
    onRevealAnswer: () -> Unit = {}
) {
    val correctAnswers = question.correctAnswer
        .split(Regex("[,，\\s]+"))
        .filter { it.isNotBlank() }
        .toSet()
    val showResult = revealed || isReviewMode

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuestionTypeLabel(type = question.type, question = question.question)

        QuestionMetadata(question = question)

        question.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = "题目图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = IMAGE_MAX_HEIGHT)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEach { option ->
                val isCorrect = option.id in correctAnswers
                OptionItem(
                    optionId = option.id,
                    optionText = option.text,
                    isSelected = option.id in selectedAnswers,
                    onClick = { if (!showResult) onOptionSelected(option.id) },
                    isCorrect = isCorrect.takeIf { showResult },
                    showResult = showResult
                )
            }
        }

        when {
            showResult && selectedAnswers != correctAnswers -> {
                AnswerBanner(
                    title = if (selectedAnswers.isEmpty()) "未作答" else "回答错误",
                    text = "正确答案：${correctAnswers.sorted().joinToString("、")}",
                    isCorrect = false
                )
            }

            showResult -> {
                AnswerBanner(
                    title = "回答正确",
                    text = "正确答案：${correctAnswers.sorted().joinToString("、")}",
                    isCorrect = true
                )
            }
        }

        if (
            question.type == QuestionType.MULTIPLE_CHOICE &&
            allowReveal &&
            !showResult &&
            selectedAnswers.isNotEmpty()
        ) {
            Button(
                onClick = onRevealAnswer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认答案")
            }
        } else if (!showResult && allowReveal && selectedAnswers.isNotEmpty()) {
            OutlinedButton(
                onClick = onRevealAnswer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看答案")
            }
        }

        if (showResult && showAnswerSkill && !question.answerSkill.isNullOrBlank()) {
            ExplanationCard(
                title = "答题技巧",
                text = question.answerSkill
            )
        }

        if (showResult && showAnswerExplain) {
            if (!question.answerSkillExplain.isNullOrBlank()) {
                ExplanationCard(
                    title = "答案解析",
                    text = question.answerSkillExplain
                )
            }
            if (!question.remark.isNullOrBlank()) {
                ExplanationCard(
                    title = "法规说明",
                    text = question.remark.toPlainText()
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionTypeLabel(type: QuestionType, question: String) {
    val tagId = "questionTypeTag"
    val annotatedString = buildAnnotatedString {
        appendInlineContent(tagId, "[Tag]")
        append("  ")
        append(question)
    }
    val inlineContent = mapOf(
        tagId to InlineTextContent(
            Placeholder(
                width = 68.sp,
                height = 28.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when (type) {
                    QuestionType.SINGLE_CHOICE -> MaterialTheme.colorScheme.primaryContainer
                    QuestionType.MULTIPLE_CHOICE -> MaterialTheme.colorScheme.secondaryContainer
                    QuestionType.TRUE_FALSE -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = when (type) {
                            QuestionType.SINGLE_CHOICE -> "单选题"
                            QuestionType.MULTIPLE_CHOICE -> "多选题"
                            QuestionType.TRUE_FALSE -> "判断题"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = MaterialTheme.typography.headlineSmall.lineHeight.times(1.3f)
    )
}

@Composable
private fun QuestionMetadata(question: Question) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetadataChip("难度 ${question.difficulty}")
        if (question.errorRate > 0f) {
            MetadataChip("易错率 ${question.errorRate.toInt()}%")
        }
        if (question.isNewRule) MetadataChip("新规")
        if (question.isErrorProne) MetadataChip("高频易错")
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AnswerBanner(title: String, text: String, isCorrect: Boolean) {
    val container = if (isCorrect) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExplanationCard(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
            )
        }
    }
}

private fun String.toPlainText(): String {
    return replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .trim()
}
