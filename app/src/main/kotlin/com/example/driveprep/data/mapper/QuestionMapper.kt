package com.example.driveprep.data.mapper

import com.example.driveprep.data.local.entity.QuestionEntity
import com.example.driveprep.domain.model.Option
import com.example.driveprep.domain.model.Question
import com.example.driveprep.domain.model.QuestionType
import com.example.driveprep.domain.model.VideoExplain

object QuestionMapper {
    fun toDomain(entity: QuestionEntity): Question {
        val type = when (entity.type) {
            1 -> QuestionType.SINGLE_CHOICE
            2 -> QuestionType.MULTIPLE_CHOICE
            3 -> QuestionType.TRUE_FALSE
            else -> QuestionType.SINGLE_CHOICE
        }

        val options = entity.itemsTitleArray.zip(entity.itemsDescArray).map { (id, text) ->
            Option(id = id, text = text)
        }

        val videoExplain = if (!entity.aliyunVid.isNullOrEmpty() || !entity.coverUrl.isNullOrEmpty()) {
            VideoExplain(
                aliyunVideoId = entity.aliyunVid,
                coverUrl = entity.coverUrl
            )
        } else null

        return Question(
            id = entity.id,
            type = type,
            question = entity.question,
            imageUrl = entity.imageUrl,
            options = options,
            correctAnswer = entity.answer,
            chapterId = entity.chapterId ?: "1",
            difficulty = entity.difficulty,
            errorRate = entity.errorRate?.toFloatOrNull() ?: 0f,
            answerSkill = entity.answerSkill,
            answerSkillExplain = entity.answerSkillExplain,
            remark = entity.remark,
            videoExplain = videoExplain,
            isNewRule = entity.newRuleFlag,
            isErrorProne = entity.errorProneFlag
        )
    }

    fun toDomainList(entities: List<QuestionEntity>): List<Question> {
        return entities.map { toDomain(it) }
    }
}
