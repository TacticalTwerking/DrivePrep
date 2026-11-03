package com.example.driveprep.domain.model

data class UserSettings(
    val showImmediateFeedback: Boolean = false,
    val showAnswerSkill: Boolean = false,
    val showAnswerExplain: Boolean = false
)
