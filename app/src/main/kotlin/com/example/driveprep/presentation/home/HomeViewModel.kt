package com.example.driveprep.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.data.preferences.UserSettingsStore
import com.example.driveprep.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsStore: UserSettingsStore
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsStore.settings.value
    )

    fun setLastSubject(subject: Int) {
        settingsStore.setLastSubject(subject)
    }
}
