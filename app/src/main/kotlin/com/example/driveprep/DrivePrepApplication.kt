package com.example.driveprep

import android.app.Application
import com.example.driveprep.domain.repository.ExamRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DrivePrepApplication : Application() {
    
    @Inject
    lateinit var examRepository: ExamRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        // Load questions from assets on first launch
        applicationScope.launch {
            examRepository.loadQuestionsFromAssets()
        }
    }
}
