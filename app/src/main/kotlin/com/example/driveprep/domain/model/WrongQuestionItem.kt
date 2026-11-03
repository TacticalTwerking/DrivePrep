package com.example.driveprep.domain.model

data class WrongQuestionItem(
    val question: Question,
    val wrongCount: Int,
    val lastWrongAt: Long
)
