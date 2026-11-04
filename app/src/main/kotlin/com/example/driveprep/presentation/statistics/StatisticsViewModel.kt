package com.example.driveprep.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.domain.model.WrongQuestionItem
import com.example.driveprep.domain.model.ExamRecordData
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val records: List<ExamRecordData> = emptyList(),
    val wrongQuestions: List<WrongQuestionItem> = emptyList(),
    val isLoading: Boolean = true,
    /** null = 全部科目;1/4 只看对应科目的成绩 */
    val selectedSubject: Int? = null
) {
    /** 指标全部按当前筛选后的记录计算,避免科一/科四混算 */
    val visibleRecords: List<ExamRecordData>
        get() = selectedSubject?.let { s -> records.filter { it.subject == s } } ?: records

    val visibleWrongQuestions: List<WrongQuestionItem>
        get() = selectedSubject?.let { s ->
            wrongQuestions.filter { it.question.subject == s }
        } ?: wrongQuestions

    val totalExams: Int get() = visibleRecords.size
    val averageScore: Int
        get() = if (visibleRecords.isEmpty()) 0 else visibleRecords.sumOf { it.score } / visibleRecords.size
    val bestScore: Int get() = visibleRecords.maxOfOrNull { it.score } ?: 0
    val passRate: Int
        get() = if (visibleRecords.isEmpty()) 0 else visibleRecords.count { it.passed } * 100 / visibleRecords.size
    val totalQuestions: Int get() = visibleRecords.sumOf { it.total }
    val totalCorrect: Int get() = visibleRecords.sumOf { it.correct }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: ExamRepository
) : ViewModel() {

    private val selectedSubject = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getExamRecords(),
        repository.getWrongQuestionItems(),
        selectedSubject
    ) { records, wrongQuestions, subject ->
        StatisticsUiState(
            records = records,
            wrongQuestions = wrongQuestions,
            isLoading = false,
            selectedSubject = subject
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
    )

    fun selectSubject(subject: Int?) {
        selectedSubject.value = subject
    }
}
