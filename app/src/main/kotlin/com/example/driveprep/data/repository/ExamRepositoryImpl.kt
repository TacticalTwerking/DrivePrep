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
import com.example.driveprep.data.offline.ImagePackRepository
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val json: Json,
    private val imagePack: ImagePackRepository
) : ExamRepository {

    private val assetImportMutex = Mutex()

    override suspend fun loadQuestionsFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        assetImportMutex.withLock {
            try {
                // 旧版本只导入了科目一,升级后必须补导科目四。
                val subjectOneCount = questionDao.getQuestionCount(1)
                val subjectFourCount = questionDao.getQuestionCount(4)
                if (subjectOneCount > 0 && subjectFourCount > 0) {
                    return@withLock Result.success(Unit)
                }

                val jsonString = context.assets.open("q.json").bufferedReader().use { it.readText() }
                val questions = json.decodeFromString<List<QuestionEntity>>(jsonString)
                questionDao.insertAll(questions)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getRandomExamQuestions(subject: Int, count: Int): Result<List<Question>> {
        return try {
            if (questionDao.getQuestionCount(subject) == 0) {
                loadQuestionsFromAssets()
            }

            val entities = questionDao.getRandomQuestions(subject, count)
            if (entities.isEmpty()) {
                Result.failure(IllegalStateException("题库中没有科目$subject 的题目"))
            } else {
                Result.success(mapWithOfflineImages(entities))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuestionsByIds(ids: List<String>): Result<List<Question>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val entities = loadQuestionEntitiesByIds(ids)
            val questionsById = mapWithOfflineImages(entities).associateBy { it.id }
            Result.success(ids.mapNotNull(questionsById::get))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWrongQuestionItems(): Flow<List<WrongQuestionItem>> {
        return wrongQuestionDao.getWrongQuestionsFlow().map { wrongEntities ->
            if (wrongEntities.isEmpty()) return@map emptyList()
            val questionsById = loadQuestionEntitiesByIds(wrongEntities.map { it.questionId })
                .let { mapWithOfflineImages(it) }
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

    /** SQLite 单条语句的绑定参数有上限,错题较多时必须分批查询。 */
    private suspend fun loadQuestionEntitiesByIds(ids: List<String>): List<QuestionEntity> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(SQLITE_MAX_VARIABLES).flatMap { chunk ->
            questionDao.getQuestionsByIds(chunk)
        }
    }

    /** 实体转领域模型,并把已离线下载的图片替换为本地文件路径(未下载的保留原 URL) */
    private suspend fun mapWithOfflineImages(entities: List<QuestionEntity>): List<Question> {
        return withContext(Dispatchers.IO) {
            QuestionMapper.toDomainList(entities).map { question ->
                question.copy(imageUrl = imagePack.resolve(question.imageUrl) ?: question.imageUrl)
            }
        }
    }

    override suspend fun saveExamProgress(data: ExamProgressData): Result<Unit> {
        return try {
            val entity = ExamProgressEntity(
                // id 即模式 key,每种模式各自一份进度
                id = data.mode,
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

    override suspend fun restoreExamProgress(mode: String): Result<ExamProgressData?> {
        return try {
            val entity = progressDao.getProgress(mode)
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

    override suspend fun clearExamProgress(progressKey: String) {
        try {
            progressDao.clearProgress(progressKey)
        } catch (_: Exception) {
            // 清理失败可忽略:下次进入按旧进度恢复
        }
    }

    override suspend fun saveExamRecord(
        score: ExamScore,
        mode: ExamMode,
        progressKey: String,
        subject: Int
    ): Result<Unit> {
        return try {
            val modeString = when (mode) {
                is ExamMode.MockExam -> "mock_exam"
                is ExamMode.Practice -> "chapter"
                is ExamMode.ErrorReview -> "wrong_book"
                else -> "unknown"
            }

            val wrongQuestionIds = score.wrongQuestions.map { it.questionId }
            val wrongQuestionsJson = json.encodeToString(wrongQuestionIds)

            val entity = ExamRecordEntity(
                mode = modeString,
                subject = subject,
                score = score.score,
                correct = score.correct,
                total = score.total,
                passed = score.passed,
                timeUsed = score.timeUsed,
                wrongQuestionsJson = wrongQuestionsJson
            )

            recordDao.insertRecord(entity)
            progressDao.clearProgress(progressKey)
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
                    subject = entity.subject,
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

    private companion object {
        const val SQLITE_MAX_VARIABLES = 900
    }
}

