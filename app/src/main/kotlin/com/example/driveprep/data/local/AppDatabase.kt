package com.example.driveprep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.driveprep.data.local.dao.ExamProgressDao
import com.example.driveprep.data.local.dao.ExamRecordDao
import com.example.driveprep.data.local.dao.QuestionDao
import com.example.driveprep.data.local.dao.WrongQuestionDao
import com.example.driveprep.data.local.entity.ExamProgressEntity
import com.example.driveprep.data.local.entity.ExamRecordEntity
import com.example.driveprep.data.local.entity.QuestionEntity
import com.example.driveprep.data.local.entity.WrongQuestionEntity

@Database(
    entities = [
        QuestionEntity::class,
        ExamProgressEntity::class,
        ExamRecordEntity::class,
        WrongQuestionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun examProgressDao(): ExamProgressDao
    abstract fun examRecordDao(): ExamRecordDao
    abstract fun wrongQuestionDao(): WrongQuestionDao
}
