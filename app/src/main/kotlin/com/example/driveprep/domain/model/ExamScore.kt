package com.example.driveprep.domain.model

data class ExamScore(
    val correct: Int,
    val total: Int,
    val score: Int,
    val passed: Boolean,
    val timeUsed: Long,
    val wrongQuestions: List<WrongQuestion>
)

data class WrongQuestion(
    val questionId: String,
    val userAnswer: Set<String>,
    val correctAnswer: String,
    val question: Question
)
