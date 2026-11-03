package com.example.driveprep.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.domain.model.WrongQuestionItem
import com.example.driveprep.domain.model.ExamRecordData
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val records: List<ExamRecordData> = emptyList(),
    val wrongQuestions: List<WrongQuestionItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalExams: Int get() = records.size
    val averageScore: Int
        get() = if (records.isEmpty()) 0 else records.sumOf { it.score } / records.size
    val bestScore: Int get() = records.maxOfOrNull { it.score } ?: 0
    val passRate: Int
        get() = if (records.isEmpty()) 0 else records.count { it.passed } * 100 / records.size
    val totalQuestions: Int get() = records.sumOf { it.total }
    val totalCorrect: Int get() = records.sumOf { it.correct }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: ExamRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getExamRecords(),
        repository.getWrongQuestionItems()
    ) { records, wrongQuestions ->
        StatisticsUiState(
            records = records,
            wrongQuestions = wrongQuestions,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
    )
}
