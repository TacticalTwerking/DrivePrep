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
            .fallbackToDestructiveMigration()
            .build()
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE exam_progress ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0"
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
