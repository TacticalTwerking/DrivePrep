package com.example.driveprep.presentation.statistics

import com.example.driveprep.domain.model.ExamRecordData
import com.example.driveprep.domain.model.Option
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.QuestionType
import com.example.driveprep.domain.model.WrongQuestionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsUiStateTest {

    private fun record(
        id: Long,
        subject: Int,
        score: Int,
        passed: Boolean
    ) = ExamRecordData(
        id = id,
        mode = "mock_exam",
        subject = subject,
        score = score,
        correct = score,
        total = 100,
        passed = passed,
        timeUsed = 60_000L,
        wrongQuestionIds = emptyList(),
        completedAt = 0L
    )

    private fun wrongQuestion(id: String, subject: Int) = WrongQuestionItem(
        question = Question(
            id = id,
            type = QuestionType.SINGLE_CHOICE,
            question = "题目",
            imageUrl = null,
            options = listOf(Option("A", "选项A")),
            correctAnswer = "A",
            chapterId = "1",
            difficulty = 1,
            errorRate = 0f,
            answerSkill = null,
            answerSkillExplain = null,
            remark = null,
            videoExplain = null,
            isNewRule = false,
            isErrorProne = false,
            subject = subject
        ),
        wrongCount = 1,
        lastWrongAt = 0L
    )

    @Test
    fun `no filter aggregates all records`() {
        val state = StatisticsUiState(
            records = listOf(
                record(1, subject = 1, score = 90, passed = true),
                record(2, subject = 4, score = 80, passed = false)
            )
        )
        assertEquals(null, state.selectedSubject)
        assertEquals(2, state.totalExams)
        assertEquals(85, state.averageScore)
        assertEquals(90, state.bestScore)
        assertEquals(50, state.passRate)
        assertEquals(200, state.totalQuestions)
    }

    @Test
    fun `subject filter only aggregates matching records`() {
        val state = StatisticsUiState(
            records = listOf(
                record(1, subject = 1, score = 90, passed = true),
                record(2, subject = 4, score = 80, passed = false),
                record(3, subject = 4, score = 100, passed = true)
            ),
            selectedSubject = 4
        )
        assertEquals(2, state.totalExams)
        assertEquals(90, state.averageScore)
        assertEquals(100, state.bestScore)
        assertEquals(50, state.passRate)
        assertEquals(200, state.totalQuestions)
    }

    @Test
    fun `filter with no matching records yields zero metrics`() {
        val state = StatisticsUiState(
            records = listOf(record(1, subject = 1, score = 90, passed = true)),
            selectedSubject = 4
        )
        assertEquals(0, state.totalExams)
        assertEquals(0, state.averageScore)
        assertEquals(0, state.bestScore)
        assertEquals(0, state.passRate)
    }

    @Test
    fun `empty records yields zero metrics without filter`() {
        val state = StatisticsUiState(records = emptyList())
        assertEquals(0, state.totalExams)
        assertEquals(0, state.averageScore)
        assertEquals(0, state.bestScore)
        assertEquals(0, state.passRate)
        assertEquals(0, state.totalQuestions)
    }

    @Test
    fun `subject filter only keeps matching wrong questions`() {
        val state = StatisticsUiState(
            wrongQuestions = listOf(
                wrongQuestion("q1", subject = 1),
                wrongQuestion("q4", subject = 4)
            ),
            selectedSubject = 4
        )
        assertEquals(listOf("q4"), state.visibleWrongQuestions.map { it.question.id })
    }
}
