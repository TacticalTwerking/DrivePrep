package com.example.driveprep.domain.model

data class ExamConfig(
    val totalQuestions: Int = 100,
    val duration: Long = 45 * 60 * 1000,
    val passingScore: Int = 90,
    val allowReview: Boolean = true
)
