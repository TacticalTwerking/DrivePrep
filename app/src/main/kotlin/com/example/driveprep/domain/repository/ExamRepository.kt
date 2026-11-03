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
    suspend fun getRandomExamQuestions(count: Int): Result<List<Question>>
    suspend fun getQuestionsByChapter(chapterId: String): Result<List<Question>>
    suspend fun getQuestionsByIds(ids: List<String>): Result<List<Question>>
    suspend fun getErrorProneQuestions(): Result<List<Question>>
    suspend fun getWrongQuestions(): Flow<List<Question>>
    fun getWrongQuestionItems(): Flow<List<WrongQuestionItem>>
    suspend fun saveExamProgress(data: ExamProgressData): Result<Unit>
    suspend fun restoreExamProgress(): Result<ExamProgressData?>
    suspend fun saveExamRecord(score: ExamScore, mode: ExamMode): Result<Unit>
    suspend fun recordWrongAnswer(questionId: String, userAnswer: Set<String>)
    suspend fun markWrongQuestionMastered(questionId: String)
    fun getExamRecords(): Flow<List<ExamRecordData>>
}
