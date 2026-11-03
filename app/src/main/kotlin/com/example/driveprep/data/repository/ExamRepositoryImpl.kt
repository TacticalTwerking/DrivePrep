package com.example.driveprep.data.repository

import android.content.Context
import com.example.driveprep.data.local.dao.ExamProgressDao
import com.example.driveprep.data.local.dao.ExamRecordDao
import com.example.driveprep.data.local.dao.QuestionDao
import com.example.driveprep.data.local.dao.WrongQuestionDao
import com.example.driveprep.data.local.entity.ExamProgressEntity
import com.example.driveprep.data.local.entity.ExamRecordEntity
import com.example.driveprep.data.local.entity.QuestionEntity
import com.example.driveprep.data.local.entity.WrongQuestionEntity
import com.example.driveprep.data.mapper.QuestionMapper
import com.example.driveprep.domain.model.ExamMode
import com.example.driveprep.domain.model.ExamProgressData
import com.example.driveprep.domain.model.ExamRecordData
import com.example.driveprep.domain.model.ExamScore
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.WrongQuestionItem
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionDao: QuestionDao,
    private val progressDao: ExamProgressDao,
    private val recordDao: ExamRecordDao,
    private val wrongQuestionDao: WrongQuestionDao,
    private val json: Json
) : ExamRepository {

    override suspend fun loadQuestionsFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val count = questionDao.getQuestionCount()
            if (count > 0) {
                return@withContext Result.success(Unit)
            }

            val jsonString = context.assets.open("q.json").bufferedReader().use { it.readText() }
            val questions = json.decodeFromString<List<QuestionEntity>>(jsonString)
                .filter { it.subject == 1 }
            questionDao.insertAll(questions)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRandomExamQuestions(count: Int): Result<List<Question>> {
        return try {
            if (questionDao.getQuestionCount() == 0) {
                loadQuestionsFromAssets()
            }

            val entities = questionDao.getRandomQuestions(count)
            Result.success(QuestionMapper.toDomainList(entities))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuestionsByChapter(chapterId: String): Result<List<Question>> {
        return try {
            val entities = questionDao.getQuestionsByChapter(chapterId)
            Result.success(QuestionMapper.toDomainList(entities))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuestionsByIds(ids: List<String>): Result<List<Question>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val entities = questionDao.getQuestionsByIds(ids)
            val questionsById = QuestionMapper.toDomainList(entities).associateBy { it.id }
            Result.success(ids.mapNotNull(questionsById::get))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getErrorProneQuestions(): Result<List<Question>> {
        return try {
            val entities = questionDao.getErrorProneQuestions()
            Result.success(QuestionMapper.toDomainList(entities))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWrongQuestions(): Flow<List<Question>> {
        return wrongQuestionDao.getWrongQuestionsFlow().map { wrongEntities ->
            val ids = wrongEntities.map { it.questionId }
            val entities = questionDao.getQuestionsByIds(ids)
            QuestionMapper.toDomainList(entities)
        }
    }

    override fun getWrongQuestionItems(): Flow<List<WrongQuestionItem>> {
        return wrongQuestionDao.getWrongQuestionsFlow().map { wrongEntities ->
            if (wrongEntities.isEmpty()) return@map emptyList()
            val questionsById = questionDao
                .getQuestionsByIds(wrongEntities.map { it.questionId })
                .let(QuestionMapper::toDomainList)
                .associateBy { it.id }

            wrongEntities.mapNotNull { wrong ->
                questionsById[wrong.questionId]?.let { question ->
                    WrongQuestionItem(
                        question = question,
                        wrongCount = wrong.wrongCount,
                        lastWrongAt = wrong.lastWrongAt
                    )
                }
            }
        }
    }

    override suspend fun saveExamProgress(data: ExamProgressData): Result<Unit> {
        return try {
            val entity = ExamProgressEntity(
                mode = data.mode,
                questionIds = json.encodeToString(data.questionIds),
                answersJson = json.encodeToString(data.answers),
                currentIndex = data.currentIndex,
                timeRemaining = data.timeRemaining,
                startTime = data.startTime,
                isPaused = data.isPaused
            )

            progressDao.saveProgress(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreExamProgress(): Result<ExamProgressData?> {
        return try {
            val entity = progressDao.getCurrentProgress()
            if (entity == null) {
                Result.success(null)
            } else {
                val questionIds = json.decodeFromString<List<String>>(entity.questionIds)
                val answers = json.decodeFromString<Map<String, Set<String>>>(entity.answersJson)

                val data = ExamProgressData(
                    mode = entity.mode,
                    questionIds = questionIds,
                    currentIndex = entity.currentIndex,
                    answers = answers,
                    timeRemaining = entity.timeRemaining,
                    startTime = entity.startTime,
                    isPaused = entity.isPaused
                )
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveExamRecord(score: ExamScore, mode: ExamMode): Result<Unit> {
        return try {
            val modeString = when (mode) {
                is ExamMode.MockExam -> "mock_exam"
                is ExamMode.Practice -> "practice"
                is ExamMode.ErrorReview -> "error_review"
                else -> "unknown"
            }

            val wrongQuestionIds = score.wrongQuestions.map { it.questionId }
            val wrongQuestionsJson = json.encodeToString(wrongQuestionIds)

            val entity = ExamRecordEntity(
                mode = modeString,
                score = score.score,
                correct = score.correct,
                total = score.total,
                passed = score.passed,
                timeUsed = score.timeUsed,
                wrongQuestionsJson = wrongQuestionsJson
            )

            recordDao.insertRecord(entity)
            progressDao.clearProgress()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordWrongAnswer(questionId: String, userAnswer: Set<String>) {
        try {
            val existing = wrongQuestionDao.getWrongQuestion(questionId)
            if (existing != null) {
                val updatedEntity = existing.copy(
                    wrongCount = existing.wrongCount + 1,
                    lastWrongAt = System.currentTimeMillis(),
                    userAnswers = userAnswer.joinToString(","),
                    isMastered = false
                )
                wrongQuestionDao.insertOrUpdate(updatedEntity)
            } else {
                val newEntity = WrongQuestionEntity(
                    questionId = questionId,
                    wrongCount = 1,
                    userAnswers = userAnswer.joinToString(",")
                )
                wrongQuestionDao.insertOrUpdate(newEntity)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun markWrongQuestionMastered(questionId: String) {
        wrongQuestionDao.markAsMastered(questionId)
    }

    override fun getExamRecords(): Flow<List<ExamRecordData>> {
        return recordDao.getAllRecordsFlow().map { entities ->
            entities.map { entity ->
                val wrongQuestionIds = json.decodeFromString<List<String>>(entity.wrongQuestionsJson)
                ExamRecordData(
                    id = entity.id,
                    mode = entity.mode,
                    score = entity.score,
                    correct = entity.correct,
                    total = entity.total,
                    passed = entity.passed,
                    timeUsed = entity.timeUsed,
                    wrongQuestionIds = wrongQuestionIds,
                    completedAt = entity.completedAt
                )
            }
        }
    }
}

