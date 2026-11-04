package com.example.driveprep.data.preferences

import android.content.Context
import com.example.driveprep.domain.model.DarkMode
import com.example.driveprep.domain.model.QuestionFontSize
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

    fun setDarkMode(mode: DarkMode) {
        preferences.edit().putString(KEY_DARK_MODE, mode.name).apply()
        refresh()
    }

    fun setQuestionFontSize(size: QuestionFontSize) {
        preferences.edit().putString(KEY_QUESTION_FONT_SIZE, size.name).apply()
        refresh()
    }

    fun setLastSubject(subject: Int) {
        preferences.edit().putInt(KEY_LAST_SUBJECT, subject).apply()
        refresh()
    }

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings(): UserSettings {
        return UserSettings(
            showImmediateFeedback = preferences.getBoolean(KEY_IMMEDIATE_FEEDBACK, false),
            showAnswerSkill = preferences.getBoolean(KEY_ANSWER_SKILL, false),
            showAnswerExplain = preferences.getBoolean(KEY_ANSWER_EXPLAIN, false),
            darkMode = preferences.getString(KEY_DARK_MODE, null)
                ?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() }
                ?: DarkMode.SYSTEM,
            questionFontSize = preferences.getString(KEY_QUESTION_FONT_SIZE, null)
                ?.let { runCatching { QuestionFontSize.valueOf(it) }.getOrNull() }
                ?: QuestionFontSize.MEDIUM,
            lastSubject = preferences.getInt(KEY_LAST_SUBJECT, 1)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "user_settings"
        const val KEY_IMMEDIATE_FEEDBACK = "show_immediate_feedback"
        const val KEY_ANSWER_SKILL = "show_answer_skill"
        const val KEY_ANSWER_EXPLAIN = "show_answer_explain"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_QUESTION_FONT_SIZE = "question_font_size"
        const val KEY_LAST_SUBJECT = "last_subject"
    }
}
