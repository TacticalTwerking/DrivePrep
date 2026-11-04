package com.example.driveprep.presentation.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.data.preferences.UserSettingsStore
import com.example.driveprep.domain.model.ExamConfig
import com.example.driveprep.domain.model.ExamMode
import com.example.driveprep.domain.model.ExamProgressData
import com.example.driveprep.domain.model.ExamScore
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.QuestionType
import com.example.driveprep.domain.model.WrongQuestion
import com.example.driveprep.domain.model.isCorrect
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository,
    private val settingsStore: UserSettingsStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 当前科目(来自导航参数):1 = 科目一,4 = 科目四 */
    val subject: Int = savedStateHandle["subject"] ?: 1

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var saveJob: Job? = null
    private var isSubmitting = false
    private var currentModeKey: String = "mock_exam"

    /** "直接退出"后置位:阻止任何残留的自动保存把刚清掉的进度又写回去 */
    private var discardRequested = false

    /**
     * 独立于 viewModelScope 的写库作用域:
     * 提交成绩/错题累计/退出清理等关键操作,不能被页面销毁时的 onCleared 取消
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 串行化“保存进度 / 交卷清理 / 丢弃进度”,避免旧写入在清理后把进度写回来。 */
    private val progressMutex = Mutex()

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        showImmediateFeedback = settings.showImmediateFeedback,
                        showAnswerSkill = settings.showAnswerSkill,
                        showAnswerExplain = settings.showAnswerExplain,
                        questionFontSizeSp = settings.questionFontSize.sp
                    )
                }
            }
        }
    }

    fun onEvent(event: ExamEvent) {
        when (event) {
            is ExamEvent.StartExam -> startFromMode(event.mode)
            is ExamEvent.StartQuestionReview -> startQuestionReview(event.questionIds, force = true)
            is ExamEvent.SelectAnswer -> selectAnswer(event.questionId, event.optionId)
            is ExamEvent.RevealAnswer -> revealAnswer(event.questionId)
            is ExamEvent.NavigateToQuestion -> navigateToQuestion(event.index)
            ExamEvent.SubmitExam -> submitExam()
            ExamEvent.RestartExam -> restartCurrentExam()
            ExamEvent.PauseExam -> pauseExam()
            ExamEvent.ResumeExam -> resumeExam()
            ExamEvent.NextQuestion -> navigateToQuestion(_uiState.value.currentIndex + 1)
            ExamEvent.PreviousQuestion -> navigateToQuestion(_uiState.value.currentIndex - 1)
            ExamEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun saveAndExit(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.isFinished) {
            onSaved()
            return
        }

        timerJob?.cancel()
        timerJob = null
        saveJob?.cancel()
        _uiState.update {
            it.copy(isPaused = it.hasTimer || it.isPaused)
        }
        viewModelScope.launch {
            progressMutex.withLock {
                val latest = _uiState.value
                if (latest.questions.isNotEmpty() && !latest.isFinished &&
                    !discardRequested && !isSubmitting
                ) {
                    repository.saveExamProgress(toProgressData(latest))
                }
            }
            onSaved()
        }
    }

    /** 不保存退出:丢弃该科目的进度,下次进入重新抽题 */
    fun discardAndExit(onExited: () -> Unit) {
        val state = _uiState.value
        timerJob?.cancel()
        timerJob = null
        saveJob?.cancel()
        discardRequested = true
        appScope.launch {
            progressMutex.withLock {
                repository.clearExamProgress(progressKey(currentModeKey, state.subject))
            }
            // appScope 运行在 IO 线程,导航回调必须切回主线程。
            withContext(Dispatchers.Main.immediate) { onExited() }
        }
    }

    private fun startFromMode(mode: String, force: Boolean = false) {
        viewModelScope.launch {
            if (isSubmitting) return@launch
            val modeKey = normalizeModeKey(mode)
            // 同模式会话进行中:直接复用当前状态(避免重复加载/重置)
            if (!force && hasActiveExam() && currentModeKey == modeKey) return@launch
            currentModeKey = modeKey

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 恢复该科目+模式的进度;
            // 错题复习跨科目且题数不固定,不做题数与科目一致性校验
            val expectedCount = if (modeKey == "wrong_book") null else standardQuestionCount()
            if (!force && restoreSession(modeKey, expectedCount, enforceSubject = modeKey != "wrong_book")) {
                return@launch
            }

            val result = when (modeKey) {
                "wrong_book" -> try {
                    Result.success(
                        repository.getWrongQuestionItems().first().map { it.question }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
                else -> repository.getRandomExamQuestions(subject, standardQuestionCount())
            }

            result.onSuccess { questions ->
                val examMode = when (modeKey) {
                    "wrong_book" -> ExamMode.ErrorReview(questions.map { it.id })
                    "chapter" -> ExamMode.Practice(chapterId = null)
                    else -> ExamMode.MockExam(isStarted = true)
                }
                startQuestions(questions, examMode)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "题目加载失败"
                    )
                }
            }
        }
    }

    /** 进度行主键:科目一/四各自独立(错题本跨科目共用一份) */
    private fun progressKey(modeKey: String, questionSubject: Int): String {
        return if (modeKey == "wrong_book") modeKey else "${modeKey}_s$questionSubject"
    }

    /** 恢复指定科目+模式的进度,成功返回 true */
    private suspend fun restoreSession(
        modeKey: String,
        expectedCount: Int? = null,
        enforceSubject: Boolean = true
    ): Boolean {
        val progress = repository.restoreExamProgress(progressKey(modeKey, subject)).getOrNull()
            ?: return false
        val questions = repository.getQuestionsByIds(progress.questionIds).getOrNull().orEmpty()
        if (questions.isEmpty()) return false
        // 科目不符(如进度是科目一的,本次进的是科目四)→ 不恢复
        if (enforceSubject && (questions.firstOrNull()?.subject ?: subject) != subject) return false
        // 题数与当前设置不符 → 不恢复,让新设置立即生效
        if (expectedCount != null && (
                progress.questionIds.size != expectedCount || questions.size != expectedCount
            )
        ) return false

        // progress.mode 存的是带科目的进度 key(chapter_s1 等),必须用 modeKey 还原。
        val mode = restoreMode(modeKey, progress.questionIds)
        val sessionSubject = questions.firstOrNull()?.subject ?: subject
        discardRequested = false
        _uiState.update {
            it.copy(
                mode = mode,
                questions = questions,
                currentIndex = progress.currentIndex.coerceIn(0, questions.lastIndex),
                answers = progress.answers,
                revealedQuestionIds = emptySet(),
                timeRemaining = if (mode is ExamMode.MockExam) {
                    progress.timeRemaining.coerceAtLeast(0L)
                } else {
                    0L
                },
                startTime = progress.startTime,
                examConfig = configForMode(mode, questions.size, sessionSubject),
                score = null,
                isLoading = false,
                isPaused = progress.isPaused,
                errorMessage = null,
                interactedThisSession = false,
                subject = sessionSubject
            )
        }

        if (mode is ExamMode.MockExam) {
            when {
                progress.timeRemaining <= 0L -> submitExam()
                !progress.isPaused -> startTimer()
            }
        }
        return true
    }

    private fun startQuestionReview(questionIds: List<String>, force: Boolean) {
        if (questionIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "没有可复习的错题") }
            return
        }
        viewModelScope.launch {
            if (isSubmitting) return@launch
            currentModeKey = "wrong_book"
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getQuestionsByIds(questionIds.distinct())
                .onSuccess { questions ->
                    startQuestions(questions, ExamMode.ErrorReview(questionIds))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "错题加载失败"
                        )
                    }
                }
        }
    }

    private fun startQuestions(loadedQuestions: List<Question>, mode: ExamMode) {
        if (loadedQuestions.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "没有可练习的题目")
            }
            return
        }

        timerJob?.cancel()
        timerJob = null
        saveJob?.cancel()
        isSubmitting = false
        discardRequested = false

        val questions = loadedQuestions
        val config = configForMode(mode, questions.size, subjectOf(questions))
        _uiState.update {
            it.copy(
                mode = mode,
                questions = questions,
                currentIndex = 0,
                answers = emptyMap(),
                revealedQuestionIds = emptySet(),
                timeRemaining = config.duration,
                startTime = System.currentTimeMillis(),
                examConfig = config,
                score = null,
                isLoading = false,
                isPaused = false,
                errorMessage = null,
                interactedThisSession = false,
                subject = subjectOf(questions)
            )
        }
        saveProgressNow()
        if (mode is ExamMode.MockExam) {
            startTimer()
        }
    }

    private fun restartCurrentExam() {
        when (currentModeKey) {
            "wrong_book" -> {
                val ids = _uiState.value.questions.map { it.id }
                if (ids.isEmpty()) {
                    startFromMode("wrong_book", force = true)
                } else {
                    startQuestionReview(ids, force = true)
                }
            }
            "chapter" -> startFromMode("chapter", force = true)
            else -> startFromMode("mock_exam", force = true)
        }
    }

    private fun selectAnswer(questionId: String, optionId: String) {
        val state = _uiState.value
        if (state.isPaused || state.isFinished || state.isLoading) return
        if (questionId in state.revealedQuestionIds) return

        val question = state.questions.firstOrNull { it.id == questionId } ?: return
        val currentAnswers = state.answers[questionId].orEmpty()
        val newAnswers = if (question.type == QuestionType.MULTIPLE_CHOICE) {
            if (optionId in currentAnswers) currentAnswers - optionId else currentAnswers + optionId
        } else {
            setOf(optionId)
        }

        val canRevealInPractice = state.mode is ExamMode.Practice ||
            state.mode is ExamMode.ErrorReview
        _uiState.update {
            it.copy(
                answers = it.answers + (questionId to newAnswers),
                interactedThisSession = true,
                revealedQuestionIds = if (
                    state.showImmediateFeedback && canRevealInPractice &&
                        question.type != QuestionType.MULTIPLE_CHOICE
                ) {
                    it.revealedQuestionIds + questionId
                } else {
                    it.revealedQuestionIds
                }
            )
        }
        scheduleSave()
    }

    private fun revealAnswer(questionId: String) {
        val state = _uiState.value
        if (state.isPaused || state.isFinished || state.mode is ExamMode.MockExam) return
        if (state.answers[questionId].isNullOrEmpty()) return
        _uiState.update { it.copy(revealedQuestionIds = it.revealedQuestionIds + questionId) }
    }

    private fun navigateToQuestion(index: Int) {
        val state = _uiState.value
        if (state.isPaused || state.isFinished) return
        if (index !in state.questions.indices) return
        _uiState.update { it.copy(currentIndex = index) }
        scheduleSave()
    }

    private fun submitExam(fromTimer: Boolean = false) {
        val state = _uiState.value
        if (isSubmitting || state.isFinished || state.questions.isEmpty()) return
        isSubmitting = true
        if (!fromTimer) timerJob?.cancel()
        timerJob = null
        saveJob?.cancel()

        val score = calculateScore(state)
        // 关键写库放独立作用域:交卷后立即返回首页也不会丢成绩记录/错题累计
        appScope.launch {
            progressMutex.withLock {
                repository.saveExamRecord(
                    score,
                    state.mode,
                    progressKey(currentModeKey, state.subject)
                )
            }
            // 未作答的题只扣分,不计入错题本
            score.wrongQuestions.forEach { wrong ->
                if (wrong.userAnswer.isNotEmpty()) {
                    repository.recordWrongAnswer(wrong.questionId, wrong.userAnswer)
                }
            }
            _uiState.update {
                it.copy(
                    mode = ExamMode.Completed(score),
                    score = score,
                    isLoading = false,
                    isPaused = false
                )
            }
            isSubmitting = false
        }
    }

    private fun calculateScore(state: ExamUiState): ExamScore {
        var correct = 0
        val wrongQuestions = mutableListOf<WrongQuestion>()

        state.questions.forEach { question ->
            val userAnswer = state.answers[question.id].orEmpty()
            if (question.isCorrect(userAnswer)) {
                correct++
            } else {
                wrongQuestions.add(
                    WrongQuestion(
                        questionId = question.id,
                        userAnswer = userAnswer,
                        correctAnswer = question.correctAnswer,
                        question = question
                    )
                )
            }
        }

        val total = state.questions.size
        val score = if (total == 0) 0 else correct * 100 / total
        val duration = state.examConfig?.duration ?: 0L
        val timeUsed = if (duration > 0) {
            (duration - state.timeRemaining).coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - state.startTime).coerceAtLeast(0L)
        }

        return ExamScore(
            correct = correct,
            total = total,
            score = score,
            passed = score >= (state.examConfig?.passingScore ?: 90),
            timeUsed = timeUsed,
            wrongQuestions = wrongQuestions
        )
    }

    private fun pauseExam() {
        val state = _uiState.value
        if (!state.hasTimer || state.isPaused || state.isFinished) return
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isPaused = true) }
        saveProgressNow()
    }

    private fun resumeExam() {
        val state = _uiState.value
        if (!state.isPaused || state.isFinished) return
        if (state.hasTimer && state.timeRemaining <= 0) {
            submitExam()
            return
        }
        _uiState.update { it.copy(isPaused = false) }
        if (state.hasTimer) startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        val state = _uiState.value
        if (state.isPaused || !state.hasTimer) return
        if ((state.examConfig?.duration ?: 0L) <= 0L) return

        timerJob = viewModelScope.launch {
            while (true) {
                val current = _uiState.value
                if (current.isPaused || current.isFinished || current.timeRemaining <= 0L) break
                delay(1000)
                _uiState.update {
                    if (it.isPaused || it.isFinished) {
                        it
                    } else {
                        it.copy(timeRemaining = (it.timeRemaining - 1000L).coerceAtLeast(0L))
                    }
                }
                val updated = _uiState.value
                if (updated.timeRemaining <= 0L && !updated.isFinished) {
                    submitExam(fromTimer = true)
                    break
                }
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            saveProgressNow()
        }
    }

    private fun saveProgressNow() {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.isFinished) return
        if (discardRequested) return
        viewModelScope.launch {
            progressMutex.withLock {
                if (discardRequested || isSubmitting) return@withLock
                val latest = _uiState.value
                if (latest.questions.isEmpty() || latest.isFinished) return@withLock
                repository.saveExamProgress(toProgressData(latest))
            }
        }
    }

    private fun hasActiveExam(): Boolean {
        val state = _uiState.value
        return state.questions.isNotEmpty() &&
            state.mode !is ExamMode.NotStarted &&
            state.mode !is ExamMode.Completed
    }

    private fun restoreMode(mode: String, ids: List<String>): ExamMode {
        return when (mode) {
            "practice", "chapter" -> ExamMode.Practice(chapterId = null)
            "error_review", "wrong_book" -> ExamMode.ErrorReview(ids)
            else -> ExamMode.MockExam(isStarted = true)
        }
    }

    private fun normalizeModeKey(mode: String): String {
        return when (mode) {
            "practice", "chapter" -> "chapter"
            "error_review", "wrong_book" -> "wrong_book"
            else -> "mock_exam"
        }
    }

    /** 标准考试题数:科目一 100 题,科目四 50 题(模拟考试与随机练习统一) */
    private fun standardQuestionCount(): Int {
        return if (subject == 4) STANDARD_QUESTION_COUNT_SUBJECT_4 else STANDARD_QUESTION_COUNT_SUBJECT_1
    }

    private fun configForMode(mode: ExamMode, totalQuestions: Int, questionSubject: Int): ExamConfig {
        return if (mode is ExamMode.MockExam) {
            if (questionSubject == 4) {
                // 科目四考试标准:50 题 · 30 分钟 · 90 分及格
                ExamConfig(
                    totalQuestions = totalQuestions,
                    duration = 30 * 60 * 1000L
                )
            } else {
                // 科目一考试标准:100 题 · 45 分钟 · 90 分及格
                ExamConfig(totalQuestions = totalQuestions)
            }
        } else {
            ExamConfig(totalQuestions = totalQuestions, duration = 0L)
        }
    }

    private fun subjectOf(questions: List<Question>): Int {
        return questions.firstOrNull()?.subject ?: subject
    }

    /** 把当前 UI 状态转换为可持久化的进度数据(mode key 同时作为进度行的主键) */
    private fun toProgressData(state: ExamUiState): ExamProgressData {
        return ExamProgressData(
            mode = progressKey(currentModeKey, state.subject),
            questionIds = state.questions.map { it.id },
            currentIndex = state.currentIndex,
            answers = state.answers,
            timeRemaining = state.timeRemaining,
            startTime = state.startTime,
            isPaused = state.isPaused
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        saveJob?.cancel()
    }

    private companion object {
        const val STANDARD_QUESTION_COUNT_SUBJECT_1 = 100
        const val STANDARD_QUESTION_COUNT_SUBJECT_4 = 50
    }
}
