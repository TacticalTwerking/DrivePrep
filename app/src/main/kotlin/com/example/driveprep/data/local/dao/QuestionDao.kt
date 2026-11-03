package com.example.driveprep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.driveprep.data.local.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE subject = 1")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE subject = 1")
    suspend fun getQuestionCount(): Int

    @Query("SELECT * FROM questions WHERE subject = 1 AND chapterId = :chapterId")
    suspend fun getQuestionsByChapter(chapterId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = 1 AND errorProneFlag = 1")
    suspend fun getErrorProneQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = 1 AND newRuleFlag = 1")
    suspend fun getNewRuleQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)
}
