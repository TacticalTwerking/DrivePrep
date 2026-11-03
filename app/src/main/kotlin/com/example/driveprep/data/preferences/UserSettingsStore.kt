package com.example.driveprep.data.preferences

import android.content.Context
import com.example.driveprep.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun setImmediateFeedback(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_IMMEDIATE_FEEDBACK, enabled).apply()
        refresh()
    }

    fun setShowAnswerSkill(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANSWER_SKILL, enabled).apply()
        refresh()
    }

    fun setShowAnswerExplain(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANSWER_EXPLAIN, enabled).apply()
        refresh()
    }

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings(): UserSettings {
        return UserSettings(
            showImmediateFeedback = preferences.getBoolean(KEY_IMMEDIATE_FEEDBACK, false),
            showAnswerSkill = preferences.getBoolean(KEY_ANSWER_SKILL, false),
            showAnswerExplain = preferences.getBoolean(KEY_ANSWER_EXPLAIN, false)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "user_settings"
        const val KEY_IMMEDIATE_FEEDBACK = "show_immediate_feedback"
        const val KEY_ANSWER_SKILL = "show_answer_skill"
        const val KEY_ANSWER_EXPLAIN = "show_answer_explain"
    }
}
