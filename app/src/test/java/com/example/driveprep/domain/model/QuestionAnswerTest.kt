package com.example.driveprep.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionAnswerTest {

    // region parseAnswerSet

    @Test
    fun `single answer parses to single element set`() {
        assertEquals(setOf("A"), parseAnswerSet("A"))
    }

    @Test
    fun `english comma separated answer parses`() {
        assertEquals(setOf("A", "B", "C"), parseAnswerSet("A,B,C"))
    }

    @Test
    fun `chinese comma separated answer parses`() {
        assertEquals(setOf("A", "B"), parseAnswerSet("A，B"))
    }

    @Test
    fun `whitespace separated answer parses`() {
        assertEquals(setOf("A", "B"), parseAnswerSet("A B"))
    }

    @Test
    fun `mixed separators with surrounding blanks parse`() {
        assertEquals(setOf("A", "B"), parseAnswerSet("A , ，B "))
    }

    @Test
    fun `blank answers parse to empty set`() {
        assertEquals(emptySet<String>(), parseAnswerSet(""))
        assertEquals(emptySet<String>(), parseAnswerSet(" , ， "))
    }

    // endregion

    // region isCorrect

    private fun question(correctAnswer: String) = Question(
        id = "q1",
        type = QuestionType.SINGLE_CHOICE,
        question = "测试题目",
        imageUrl = null,
        options = listOf(Option("A", "选项A"), Option("B", "选项B")),
        correctAnswer = correctAnswer,
        chapterId = "1",
        difficulty = 1,
        errorRate = 0f,
        answerSkill = null,
        answerSkillExplain = null,
        remark = null,
        videoExplain = null,
        isNewRule = false,
        isErrorProne = false
    )

    @Test
    fun `single choice correct answer matches`() {
        assertTrue(question("A").isCorrect(setOf("A")))
    }

    @Test
    fun `single choice wrong answer fails`() {
        assertFalse(question("A").isCorrect(setOf("B")))
    }

    @Test
    fun `unanswered question fails`() {
        assertFalse(question("A").isCorrect(emptySet()))
    }

    @Test
    fun `multiple choice requires exact match`() {
        val q = question("A,B,C")
        assertTrue(q.isCorrect(setOf("A", "B", "C")))
        assertFalse(q.isCorrect(setOf("A", "B")))
        assertFalse(q.isCorrect(setOf("A", "B", "C", "D")))
    }

    @Test
    fun `multiple choice ignores selection order`() {
        assertTrue(question("A,B").isCorrect(setOf("B", "A")))
    }

    @Test
    fun `judge questions with chinese answers match`() {
        assertTrue(question("正确").isCorrect(setOf("正确")))
        assertFalse(question("正确").isCorrect(setOf("错误")))
    }

    // endregion
}
