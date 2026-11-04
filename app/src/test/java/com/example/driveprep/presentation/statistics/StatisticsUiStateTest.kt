package com.example.driveprep.presentation.statistics

import com.example.driveprep.domain.model.ExamRecordData
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
}
