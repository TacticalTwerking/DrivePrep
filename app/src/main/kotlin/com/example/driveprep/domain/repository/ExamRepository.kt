package com.example.driveprep.domain.repository

import com.example.driveprep.domain.model.ExamMode
import com.example.driveprep.domain.model.ExamProgressData
import com.example.driveprep.domain.model.ExamRecordData
import com.example.driveprep.domain.model.ExamScore
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.WrongQuestionItem
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    suspend fun loadQuestionsFromAssets(): Result<Unit>
    suspend fun getRandomExamQuestions(subject: Int, count: Int): Result<List<Question>>
    suspend fun getQuestionsByIds(ids: List<String>): Result<List<Question>>
    fun getWrongQuestionItems(): Flow<List<WrongQuestionItem>>
    suspend fun saveExamProgress(data: ExamProgressData): Result<Unit>
    /** 按模式恢复进度(每种模式各自一份) */
    suspend fun restoreExamProgress(mode: String): Result<ExamProgressData?>
    /** 丢弃指定 key 的进度(不保存退出) */
    suspend fun clearExamProgress(progressKey: String)
    suspend fun saveExamRecord(score: ExamScore, mode: ExamMode, progressKey: String): Result<Unit>
    suspend fun recordWrongAnswer(questionId: String, userAnswer: Set<String>)
    suspend fun markWrongQuestionMastered(questionId: String)
    fun getExamRecords(): Flow<List<ExamRecordData>>
}
