package com.example.driveprep.data.mapper

import com.example.driveprep.data.local.entity.QuestionEntity
import com.example.driveprep.domain.model.Option
import com.example.driveprep.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionMapperTest {

    private fun entity(
        type: Int = 1,
        chapterId: String? = "1",
        errorRate: String? = "4.3",
        imageUrl: String? = "https://example.com/img.png",
        aliyunVid: String? = null,
        coverUrl: String? = null,
        newRuleFlag: Boolean = false,
        errorProneFlag: Boolean = false
    ) = QuestionEntity(
        id = "q1",
        type = type,
        question = "夜间会车应在多少米外改用近光灯?",
        imageUrl = imageUrl,
        answer = "B",
        itemsTitleArray = listOf("A", "B", "C", "D"),
        itemsDescArray = listOf("50米", "150米", "100米", "30米"),
        chapterId = chapterId,
        difficulty = 2,
        errorRate = errorRate,
        answerSkill = "口诀:会车一百五,改用近光灯",
        answerSkillExplain = "夜间会车 150 米外换近光灯",
        remark = "安全行车常识",
        aliyunVid = aliyunVid,
        coverUrl = coverUrl,
        newRuleFlag = newRuleFlag,
        errorProneFlag = errorProneFlag
    )

    @Test
    fun `type 1 maps to single choice`() {
        assertEquals(QuestionType.SINGLE_CHOICE, QuestionMapper.toDomain(entity(type = 1)).type)
    }

    @Test
    fun `type 2 maps to multiple choice`() {
        assertEquals(QuestionType.MULTIPLE_CHOICE, QuestionMapper.toDomain(entity(type = 2)).type)
    }

    @Test
    fun `type 3 maps to true false`() {
        assertEquals(QuestionType.TRUE_FALSE, QuestionMapper.toDomain(entity(type = 3)).type)
    }

    @Test
    fun `unknown type falls back to single choice`() {
        assertEquals(QuestionType.SINGLE_CHOICE, QuestionMapper.toDomain(entity(type = 99)).type)
    }

    @Test
    fun `options are zipped from title and desc arrays`() {
        val question = QuestionMapper.toDomain(entity())
        assertEquals(4, question.options.size)
        assertEquals(Option("A", "50米"), question.options[0])
        assertEquals(Option("D", "30米"), question.options[3])
    }

    @Test
    fun `base fields are copied`() {
        val question = QuestionMapper.toDomain(entity())
        assertEquals("q1", question.id)
        assertEquals("夜间会车应在多少米外改用近光灯?", question.question)
        assertEquals("https://example.com/img.png", question.imageUrl)
        assertEquals("B", question.correctAnswer)
        assertEquals(2, question.difficulty)
        assertEquals("口诀:会车一百五,改用近光灯", question.answerSkill)
    }

    @Test
    fun `null chapterId falls back to 1`() {
        assertEquals("1", QuestionMapper.toDomain(entity(chapterId = null)).chapterId)
    }

    @Test
    fun `errorRate string parses to float`() {
        assertEquals(4.3f, QuestionMapper.toDomain(entity(errorRate = "4.3")).errorRate, 0.001f)
    }

    @Test
    fun `invalid or missing errorRate falls back to zero`() {
        assertEquals(0f, QuestionMapper.toDomain(entity(errorRate = null)).errorRate, 0.001f)
        assertEquals(0f, QuestionMapper.toDomain(entity(errorRate = "abc")).errorRate, 0.001f)
    }

    @Test
    fun `no video fields means no video explain`() {
        assertNull(QuestionMapper.toDomain(entity()).videoExplain)
    }

    @Test
    fun `video id alone produces video explain`() {
        val video = QuestionMapper.toDomain(entity(aliyunVid = "vid123")).videoExplain
        assertEquals("vid123", video?.aliyunVideoId)
        assertNull(video?.coverUrl)
    }

    @Test
    fun `cover url alone produces video explain`() {
        val video = QuestionMapper.toDomain(entity(coverUrl = "https://example.com/cover.jpg")).videoExplain
        assertNull(video?.aliyunVideoId)
        assertEquals("https://example.com/cover.jpg", video?.coverUrl)
    }

    @Test
    fun `http image url upgrades to https`() {
        val question = QuestionMapper.toDomain(
            entity(imageUrl = "http://app.static.public.chetailian.com/img.jpg")
        )
        assertEquals("https://app.static.public.chetailian.com/img.jpg", question.imageUrl)
    }

    @Test
    fun `null image url stays null`() {
        assertNull(QuestionMapper.toDomain(entity(imageUrl = null)).imageUrl)
    }

    @Test
    fun `http cover url upgrades to https`() {
        val video = QuestionMapper.toDomain(
            entity(coverUrl = "http://app.static.public.chetailian.com/cover.jpg")
        ).videoExplain
        assertEquals("https://app.static.public.chetailian.com/cover.jpg", video?.coverUrl)
    }

    @Test
    fun `flags are copied`() {
        val question = QuestionMapper.toDomain(entity(newRuleFlag = true, errorProneFlag = true))
        assertTrue(question.isNewRule)
        assertTrue(question.isErrorProne)

        val default = QuestionMapper.toDomain(entity())
        assertFalse(default.isNewRule)
        assertFalse(default.isErrorProne)
    }

    @Test
    fun `list mapping preserves order and size`() {
        val entities = listOf(entity(type = 1), entity(type = 2), entity(type = 3))
        val questions = QuestionMapper.toDomainList(entities)
        assertEquals(3, questions.size)
        assertEquals(QuestionType.SINGLE_CHOICE, questions[0].type)
        assertEquals(QuestionType.MULTIPLE_CHOICE, questions[1].type)
        assertEquals(QuestionType.TRUE_FALSE, questions[2].type)
    }
}
