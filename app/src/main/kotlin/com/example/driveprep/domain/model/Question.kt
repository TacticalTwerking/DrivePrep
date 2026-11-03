package com.example.driveprep.domain.model

data class Question(
    val id: String,
    val type: QuestionType,
    val question: String,
    val imageUrl: String?,
    val options: List<Option>,
    val correctAnswer: String,
    val chapterId: String,
    val difficulty: Int,
    val errorRate: Float,
    val answerSkill: String?,
    val answerSkillExplain: String?,
    val remark: String?,
    val videoExplain: VideoExplain?,
    val isNewRule: Boolean,
    val isErrorProne: Boolean
)

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE
}

data class Option(
    val id: String,
    val text: String
)

data class VideoExplain(
    val aliyunVideoId: String?,
    val coverUrl: String?
)

/**
 * 把答案串("A"、"A,B"、"A，B"、"A B" 等)解析为选项 ID 集合,
 * 中英文逗号和空白符均可作为分隔符。
 */
fun parseAnswerSet(answer: String): Set<String> {
    return answer.split(Regex("[,，\\s]+"))
        .filter { it.isNotBlank() }
        .toSet()
}

/**
 * 判断用户作答是否正确(单选/判断:集合完全匹配;多选:全对才算对)。
 */
fun Question.isCorrect(userAnswer: Set<String>): Boolean {
    return userAnswer == parseAnswerSet(correctAnswer)
}
