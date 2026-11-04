package com.example.driveprep.domain.model

/** 应用深色模式三态 */
enum class DarkMode {
    SYSTEM, LIGHT, DARK
}

/** 题目字号三档 */
enum class QuestionFontSize(val sp: Int) {
    LARGE(24), MEDIUM(20), SMALL(16)
}

data class UserSettings(
    val showImmediateFeedback: Boolean = false,
    val showAnswerSkill: Boolean = false,
    val showAnswerExplain: Boolean = false,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val questionFontSize: QuestionFontSize = QuestionFontSize.MEDIUM,
    /** 首页上次选择的科目:1 = 科目一,4 = 科目四 */
    val lastSubject: Int = 1
)
