package com.example.driveprep.data.local.entity

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 验证题库 JSON(q.json)到 QuestionEntity 的反序列化,
 * 字段名映射需与线上数据源保持一致(如 url、aliyVid)。
 */
class QuestionEntityJsonTest {

    // 与 AppModule.provideJson 保持一致:忽略未知字段 + 宽松解析
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `decodes full question with unknown keys ignored`() {
        val raw = """
            [
              {
                "aliyVid": null,
                "answer": "B",
                "answerSkill": "看图答题",
                "answerSkillExplain": "夜间会车不能使用远光灯",
                "chapterId": "1",
                "coverUrl": null,
                "difficulty": 1,
                "easyErrorFlag": false,
                "errorProneFlag": true,
                "errorRate": "4.3",
                "flag": 0,
                "id": "201904101025192asdcqevyphgpzrvkk8wrd",
                "itemsDescArray": ["正确", "错误"],
                "itemsTitleArray": ["A", "B"],
                "newRuleFlag": false,
                "question": "夜间会车应使用近光灯",
                "subject": 1,
                "type": 3,
                "url": "https://example.com/img.png"
              }
            ]
        """.trimIndent()

        val entities = json.decodeFromString(ListSerializer(QuestionEntity.serializer()), raw)

        assertEquals(1, entities.size)
        val entity = entities[0]
        assertEquals("201904101025192asdcqevyphgpzrvkk8wrd", entity.id)
        assertEquals(3, entity.type)
        assertEquals("B", entity.answer)
        assertEquals(listOf("A", "B"), entity.itemsTitleArray)
        assertEquals(listOf("正确", "错误"), entity.itemsDescArray)
        assertEquals("https://example.com/img.png", entity.imageUrl)
        assertEquals("4.3", entity.errorRate)
        assertEquals(true, entity.errorProneFlag)
        assertEquals(1, entity.subject)
    }

    @Test
    fun `serial name url maps to imageUrl`() {
        val raw = """
            [
              {
                "id": "q1",
                "type": 1,
                "question": "题目",
                "answer": "A",
                "itemsTitleArray": ["A"],
                "itemsDescArray": ["选项A"],
                "url": "https://example.com/1.png"
              }
            ]
        """.trimIndent()

        val entity = json.decodeFromString(ListSerializer(QuestionEntity.serializer()), raw)[0]
        assertEquals("https://example.com/1.png", entity.imageUrl)
    }

    @Test
    fun `serial name aliyVid maps to aliyunVid`() {
        val raw = """
            [
              {
                "id": "q2",
                "type": 1,
                "question": "题目",
                "answer": "A",
                "itemsTitleArray": ["A"],
                "itemsDescArray": ["选项A"],
                "aliyVid": "vid001",
                "coverUrl": "https://example.com/cover.jpg"
              }
            ]
        """.trimIndent()

        val entity = json.decodeFromString(ListSerializer(QuestionEntity.serializer()), raw)[0]
        assertEquals("vid001", entity.aliyunVid)
        assertEquals("https://example.com/cover.jpg", entity.coverUrl)
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val raw = """
            [
              {
                "id": "q3",
                "type": 1,
                "question": "题目",
                "answer": "A",
                "itemsTitleArray": ["A"],
                "itemsDescArray": ["选项A"]
              }
            ]
        """.trimIndent()

        val entity = json.decodeFromString(ListSerializer(QuestionEntity.serializer()), raw)[0]
        assertNull(entity.imageUrl)
        assertNull(entity.chapterId)
        assertNull(entity.errorRate)
        assertNull(entity.aliyunVid)
        assertEquals(1, entity.difficulty)
        assertEquals(false, entity.newRuleFlag)
        assertEquals(false, entity.errorProneFlag)
        assertEquals(1, entity.subject)
    }
}
