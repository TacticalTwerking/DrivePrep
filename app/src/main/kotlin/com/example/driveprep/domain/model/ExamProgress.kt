package com.example.driveprep.domain.model

/**
 * 已保存的考试进度,由 presentation 层从 UI 状态转换而来,Repository 负责持久化。
 */
data class ExamProgressData(
    val mode: String,
    val questionIds: List<String>,
    val currentIndex: Int,
    val answers: Map<String, Set<String>>,
    val timeRemaining: Long,
    val startTime: Long,
    val isPaused: Boolean = false
)

/**
 * 一条已完成的考试/练习记录。
 */
data class ExamRecordData(
    val id: Long,
    val mode: String,
    /** 1 = 科目一,4 = 科目四;v6 之前的存量记录回填为科目一 */
    val subject: Int,
    val score: Int,
    val correct: Int,
    val total: Int,
    val passed: Boolean,
    val timeUsed: Long,
    val wrongQuestionIds: List<String>,
    val completedAt: Long
)
