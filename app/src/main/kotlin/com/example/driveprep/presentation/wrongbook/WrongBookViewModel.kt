package com.example.driveprep.presentation.wrongbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.domain.model.WrongQuestionItem
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrongBookViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    val wrongQuestions: StateFlow<List<WrongQuestionItem>> =
        repository.getWrongQuestionItems().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun markAsMastered(questionId: String) {
        viewModelScope.launch {
            repository.markWrongQuestionMastered(questionId)
        }
    }
}
