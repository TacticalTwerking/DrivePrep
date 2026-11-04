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
    /** 每种模式(模拟考试/章节练习/错题复习)各自保存一份进度,id 即模式 key */
    @Query("SELECT * FROM exam_progress WHERE id = :mode")
    suspend fun getProgress(mode: String): ExamProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ExamProgressEntity)

    @Query("DELETE FROM exam_progress WHERE id = :mode")
    suspend fun clearProgress(mode: String)
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
}
