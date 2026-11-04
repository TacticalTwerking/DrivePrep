package com.example.driveprep.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.driveprep.data.local.AppDatabase
import com.example.driveprep.data.local.dao.ExamProgressDao
import com.example.driveprep.data.local.dao.ExamRecordDao
import com.example.driveprep.data.local.dao.QuestionDao
import com.example.driveprep.data.local.dao.WrongQuestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "driveprep_database"
        )
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .build()
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE exam_progress ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v5 调整了进度行的主键语义:由单一 current_exam 改为按模式/科目分别保存。
     * 表结构未变,但需要把旧进度迁移到新 key,避免升级后丢失未完成的考试。
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                UPDATE OR REPLACE exam_progress
                SET id = CASE
                        WHEN mode IN ('practice', 'chapter') THEN 'chapter_s1'
                        WHEN mode IN ('error_review', 'wrong_book') THEN 'wrong_book'
                        ELSE 'mock_exam_s1'
                    END,
                    mode = CASE
                        WHEN mode IN ('practice', 'chapter') THEN 'chapter_s1'
                        WHEN mode IN ('error_review', 'wrong_book') THEN 'wrong_book'
                        ELSE 'mock_exam_s1'
                    END
                WHERE id = 'current_exam'
                """.trimIndent()
            )
        }
    }

    /**
     * v6 成绩记录增加科目列。存量行无法判别科目,
     * 统一回填为科目一(科目四支持上线前的记录本就只有科目一)。
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE exam_records ADD COLUMN subject INTEGER NOT NULL DEFAULT 1"
            )
        }
    }

    @Provides
    @Singleton
    fun provideQuestionDao(database: AppDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    @Singleton
    fun provideExamProgressDao(database: AppDatabase): ExamProgressDao {
        return database.examProgressDao()
    }

    @Provides
    @Singleton
    fun provideExamRecordDao(database: AppDatabase): ExamRecordDao {
        return database.examRecordDao()
    }

    @Provides
    @Singleton
    fun provideWrongQuestionDao(database: AppDatabase): WrongQuestionDao {
        return database.wrongQuestionDao()
    }
}
