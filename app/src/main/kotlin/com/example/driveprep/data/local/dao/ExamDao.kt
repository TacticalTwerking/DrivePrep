package com.example.driveprep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.driveprep.data.local.entity.ExamProgressEntity
import com.example.driveprep.data.local.entity.ExamRecordEntity
import com.example.driveprep.data.local.entity.WrongQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamProgressDao {
    @Query("SELECT * FROM exam_progress WHERE id = 'current_exam'")
    suspend fun getCurrentProgress(): ExamProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ExamProgressEntity)

    @Query("DELETE FROM exam_progress")
    suspend fun clearProgress()
}

@Dao
interface ExamRecordDao {
    @Query("SELECT * FROM exam_records ORDER BY completedAt DESC")
    fun getAllRecordsFlow(): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE mode = :mode ORDER BY completedAt DESC LIMIT 10")
    suspend fun getRecordsByMode(mode: String): List<ExamRecordEntity>

    @Insert
    suspend fun insertRecord(record: ExamRecordEntity)
}

@Dao
interface WrongQuestionDao {
    @Query("SELECT * FROM wrong_questions WHERE isMastered = 0 ORDER BY wrongCount DESC, lastWrongAt DESC")
    fun getWrongQuestionsFlow(): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE questionId = :questionId")
    suspend fun getWrongQuestion(questionId: String): WrongQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(wrongQuestion: WrongQuestionEntity)

    @Query("UPDATE wrong_questions SET isMastered = 1 WHERE questionId = :questionId")
    suspend fun markAsMastered(questionId: String)

    @Query("SELECT * FROM wrong_questions WHERE isMastered = 0 ORDER BY wrongCount DESC, lastWrongAt DESC")
    suspend fun getWrongQuestionEntities(): List<WrongQuestionEntity>

    @Query("SELECT questionId FROM wrong_questions WHERE isMastered = 0")
    suspend fun getWrongQuestionIds(): List<String>
}
