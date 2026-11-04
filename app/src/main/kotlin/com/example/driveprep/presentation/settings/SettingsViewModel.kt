package com.example.driveprep.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driveprep.data.offline.ImagePackRepository
import com.example.driveprep.data.offline.ImagePackState
import com.example.driveprep.data.preferences.UserSettingsStore
import com.example.driveprep.domain.model.DarkMode
import com.example.driveprep.domain.model.QuestionFontSize
import com.example.driveprep.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: UserSettingsStore,
    private val imagePackRepository: ImagePackRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsStore.settings.value
    )

    val imagePack: StateFlow<ImagePackState> = imagePackRepository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = imagePackRepository.state.value
    )

    init {
        // 进入设置页时刷新图片包库存(已下载张数/体积)
        viewModelScope.launch {
            imagePackRepository.refreshStatus()
        }
    }

    fun setImmediateFeedback(enabled: Boolean) {
        settingsStore.setImmediateFeedback(enabled)
    }

    fun setShowAnswerSkill(enabled: Boolean) {
        settingsStore.setShowAnswerSkill(enabled)
    }

    fun setShowAnswerExplain(enabled: Boolean) {
        settingsStore.setShowAnswerExplain(enabled)
    }

    fun setDarkMode(mode: DarkMode) {
        settingsStore.setDarkMode(mode)
    }

    fun setQuestionFontSize(size: QuestionFontSize) {
        settingsStore.setQuestionFontSize(size)
    }

    fun downloadImages() {
        imagePackRepository.startDownload()
    }

    fun cancelImageDownload() {
        imagePackRepository.cancelDownload()
    }

    fun clearImages() {
        imagePackRepository.clearPack()
    }
}
