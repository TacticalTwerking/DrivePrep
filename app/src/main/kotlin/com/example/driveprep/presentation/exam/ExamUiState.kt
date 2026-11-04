package com.example.driveprep.presentation.exam

import com.example.driveprep.domain.model.ExamConfig
import com.example.driveprep.domain.model.ExamMode
import com.example.driveprep.domain.model.ExamScore
import com.example.driveprep.domain.model.Question

data class ExamUiState(
    val mode: ExamMode = ExamMode.NotStarted,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, Set<String>> = emptyMap(),
    val revealedQuestionIds: Set<String> = emptySet(),
    val timeRemaining: Long = 0L,
    val startTime: Long = 0L,
    val examConfig: ExamConfig? = null,
    val score: ExamScore? = null,
    val isLoading: Boolean = false,
    val isPaused: Boolean = false,
    val errorMessage: String? = null,
    val showImmediateFeedback: Boolean = false,
    val showAnswerSkill: Boolean = false,
    val showAnswerExplain: Boolean = false,
    /** 当前科目:1 = 科目一,4 = 科目四 */
    val subject: Int = 1,
    /** 本次进入会话后用户是否作答过(决定按返回时是否弹确认框) */
    val interactedThisSession: Boolean = false,
    /** 题目字号(sp),来自设置的三档:24/20/16 */
    val questionFontSizeSp: Int = 20
) {
    val isFinished: Boolean
        get() = mode is ExamMode.Completed

    val hasTimer: Boolean
        get() = mode is ExamMode.MockExam
}

sealed class ExamEvent {
    data class StartExam(val mode: String) : ExamEvent()
    data class StartQuestionReview(val questionIds: List<String>) : ExamEvent()
    data class SelectAnswer(val questionId: String, val optionId: String) : ExamEvent()
    data class RevealAnswer(val questionId: String) : ExamEvent()
    data class NavigateToQuestion(val index: Int) : ExamEvent()
    object SubmitExam : ExamEvent()
    object RestartExam : ExamEvent()
    object PauseExam : ExamEvent()
    object ResumeExam : ExamEvent()
    object NextQuestion : ExamEvent()
    object PreviousQuestion : ExamEvent()
    object DismissError : ExamEvent()
}
