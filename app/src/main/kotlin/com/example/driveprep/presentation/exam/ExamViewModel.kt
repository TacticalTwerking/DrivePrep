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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository,
    private val settingsStore: UserSettingsStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var saveJob: Job? = null
    private var restoreJob: Job? = null
    private var isSubmitting = false
    private var currentModeKey: String = "mock_exam"

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        showImmediateFeedback = settings.showImmediateFeedback,
                        showAnswerSkill = settings.showAnswerSkill,
                        showAnswerExplain = settings.showAnswerExplain
                    )
                }
            }
        }
        restoreJob = viewModelScope.launch { restoreProgress() }
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
            repository.saveExamProgress(toProgressData(_uiState.value))
            onSaved()
        }
    }

    private suspend fun restoreProgress() {
        val result = repository.restoreExamProgress()
        val progress = result.getOrNull()
        if (progress == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val questions = repository.getQuestionsByIds(progress.questionIds).getOrNull().orEmpty()
        if (questions.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val mode = restoreMode(progress.mode, progress.questionIds)
        currentModeKey = normalizeModeKey(progress.mode)
        _uiState.update {
            it.copy(
                mode = mode,
                questions = prepareQuestions(questions),
                currentIndex = progress.currentIndex.coerceIn(0, questions.lastIndex),
                answers = progress.answers,
                timeRemaining = if (mode is ExamMode.MockExam) {
                    progress.timeRemaining.coerceAtLeast(0L)
                } else {
                    0L
                },
                startTime = progress.startTime,
                examConfig = configForMode(mode, questions.size),
                isPaused = progress.isPaused,
                isLoading = false,
                errorMessage = null
            )
        }

        if (mode is ExamMode.MockExam && !progress.isPaused) {
            startTimer()
        }
    }

    private fun startFromMode(mode: String, force: Boolean = false) {
        viewModelScope.launch {
            restoreJob?.join()
            if (isSubmitting) return@launch
            if (!force && hasActiveExam()) return@launch
            currentModeKey = normalizeModeKey(mode)

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = when (mode) {
                "wrong_book" -> repository.getWrongQuestionItems().first()
                    .map { it.question }
                    .let { Result.success(it) }
                "chapter" -> repository.getRandomExamQuestions(PRACTICE_QUESTION_COUNT)
                else -> repository.getRandomExamQuestions(MOCK_EXAM_QUESTION_COUNT)
            }

            result.onSuccess { questions ->
                val examMode = when (mode) {
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

    private fun startQuestionReview(questionIds: List<String>, force: Boolean) {
        if (questionIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "没有可复习的错题") }
            return
        }
        viewModelScope.launch {
            restoreJob?.join()
            if (!force && hasActiveExam()) return@launch
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

        val questions = prepareQuestions(loadedQuestions)
        val config = configForMode(mode, questions.size)
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
                errorMessage = null
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

        _uiState.update {
            it.copy(
                answers = it.answers + (questionId to newAnswers),
                revealedQuestionIds = if (
                    state.showImmediateFeedback && question.type != QuestionType.MULTIPLE_CHOICE
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
        if (state.isPaused || state.isFinished) return
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
        viewModelScope.launch {
            repository.saveExamRecord(score, state.mode)
            score.wrongQuestions.forEach { wrong ->
                repository.recordWrongAnswer(wrong.questionId, wrong.userAnswer)
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
        viewModelScope.launch {
            repository.saveExamProgress(toProgressData(state))
        }
    }

    /** 把当前 UI 状态转换为可持久化的进度数据(domain 层模型) */
    private fun toProgressData(state: ExamUiState): ExamProgressData {
        val modeString = when (state.mode) {
            is ExamMode.MockExam -> "mock_exam"
            is ExamMode.Practice -> "practice"
            is ExamMode.ErrorReview -> "error_review"
            else -> "unknown"
        }
        return ExamProgressData(
            mode = modeString,
            questionIds = state.questions.map { it.id },
            currentIndex = state.currentIndex,
            answers = state.answers,
            timeRemaining = state.timeRemaining,
            startTime = state.startTime,
            isPaused = state.isPaused
        )
    }

    private fun hasActiveExam(): Boolean {
        val state = _uiState.value
        return state.questions.isNotEmpty() &&
            state.mode !is ExamMode.NotStarted &&
            state.mode !is ExamMode.Completed &&
            state.mode !is ExamMode.TimeUp
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

    private fun configForMode(mode: ExamMode, totalQuestions: Int): ExamConfig {
        return if (mode is ExamMode.MockExam) {
            ExamConfig(totalQuestions = totalQuestions)
        } else {
            ExamConfig(totalQuestions = totalQuestions, duration = 0L)
        }
    }

    private fun prepareQuestions(questions: List<Question>): List<Question> {
        return questions.map { question ->
            if (question.type == QuestionType.MULTIPLE_CHOICE && question.options.size > 1) {
                question.copy(options = question.options.shuffled())
            } else {
                question
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        saveJob?.cancel()
    }

    private companion object {
        const val MOCK_EXAM_QUESTION_COUNT = 100
        const val PRACTICE_QUESTION_COUNT = 50
    }
}
