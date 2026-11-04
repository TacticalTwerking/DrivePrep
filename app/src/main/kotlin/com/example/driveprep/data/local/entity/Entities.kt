package com.example.driveprep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val type: Int,
    val question: String,
    @SerialName("url") val imageUrl: String? = null,
    val answer: String,
    val itemsTitleArray: List<String>,
    val itemsDescArray: List<String>,
    val chapterId: String? = null,
    val difficulty: Int = 1,
    val errorRate: String? = null,
    val answerSkill: String? = null,
    val answerSkillExplain: String? = null,
    val remark: String? = null,
    @SerialName("aliyVid") val aliyunVid: String? = null,
    val coverUrl: String? = null,
    val newRuleFlag: Boolean = false,
    val errorProneFlag: Boolean = false,
    val subject: Int = 1,
    val style: Int = 1,
    val score: Int = 1
)

@Entity(tableName = "exam_progress")
data class ExamProgressEntity(
    @PrimaryKey val id: String = "current_exam",
    val mode: String,
    val questionIds: String,
    val currentIndex: Int,
    val answersJson: String,
    val timeRemaining: Long,
    val startTime: Long,
    val isPaused: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_records")
data class ExamRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    /** 1 = 科目一,4 = 科目四;v6 之前的存量记录无法判别,统一回填为科目一 */
    @ColumnInfo(defaultValue = "1") val subject: Int = 1,
    val score: Int,
    val correct: Int,
    val total: Int,
    val passed: Boolean,
    val timeUsed: Long,
    val wrongQuestionsJson: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wrong_questions")
data class WrongQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val wrongCount: Int = 1,
    val lastWrongAt: Long = System.currentTimeMillis(),
    val userAnswers: String,
    val isMastered: Boolean = false
)
