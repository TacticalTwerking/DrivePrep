package com.example.driveprep.domain.model

sealed class ExamMode {
    object NotStarted : ExamMode()
    data class Practice(val chapterId: String? = null) : ExamMode()
    data class MockExam(val isStarted: Boolean = false) : ExamMode()
    data class ErrorReview(val errorIds: List<String>) : ExamMode()
    data class Completed(val finalScore: ExamScore) : ExamMode()
    data class TimeUp(val finalScore: ExamScore? = null) : ExamMode()
}
