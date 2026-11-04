package com.example.driveprep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.driveprep.data.local.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Query("SELECT COUNT(*) FROM questions WHERE subject = :subject")
    suspend fun getQuestionCount(subject: Int): Int

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = :subject ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(subject: Int, limit: Int): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)
}
