package com.example.driveprep.presentation.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.driveprep.domain.model.ExamMode
import com.example.driveprep.domain.model.QuestionType
import com.example.driveprep.presentation.exam.components.AnswerProgressSheet
import com.example.driveprep.presentation.exam.components.LoadingScreen
import com.example.driveprep.presentation.exam.components.QuestionCard
import com.example.driveprep.presentation.exam.components.TimerBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    mode: String,
    onNavigateBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { uiState.questions.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showProgressSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        if (uiState.mode is ExamMode.NotStarted && uiState.questions.isEmpty()) {
            viewModel.onEvent(ExamEvent.StartExam(mode))
        }
    }

    LaunchedEffect(uiState.currentIndex) {
        if (uiState.currentIndex in 0 until pagerState.pageCount &&
            pagerState.currentPage != uiState.currentIndex
        ) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onEvent(ExamEvent.NavigateToQuestion(pagerState.currentPage))
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onEvent(ExamEvent.DismissError)
    }

    DisposableEffect(lifecycleOwner, uiState.hasTimer, uiState.isFinished) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && uiState.hasTimer && !uiState.isFinished) {
                viewModel.onEvent(ExamEvent.PauseExam)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = uiState.questions.isNotEmpty() && !uiState.isFinished) {
        showExitDialog = true
    }

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    if (uiState.mode is ExamMode.Completed) {
        val score = (uiState.mode as ExamMode.Completed).finalScore
        ExamResultScreen(
            score = score,
            onReviewWrongQuestions = {
                viewModel.onEvent(
                    ExamEvent.StartQuestionReview(score.wrongQuestions.map { it.questionId })
                )
            },
            onRetry = { viewModel.onEvent(ExamEvent.RestartExam) },
            onExit = onNavigateBack
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.mode) {
                            is ExamMode.ErrorReview -> "错题复习"
                            is ExamMode.Practice -> "章节练习"
                            else -> "模拟考试"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.questions.isNotEmpty() && !uiState.isFinished) {
                            showExitDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showProgressSheet = true }) {
                        Text(
                            text = "${uiState.currentIndex + 1}/${uiState.questions.size}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.hasTimer) {
                        Text(
                            text = formatTime(uiState.timeRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.timeRemaining < 5 * 60 * 1000L) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        TextButton(
                            onClick = {
                                if (uiState.isPaused) {
                                    viewModel.onEvent(ExamEvent.ResumeExam)
                                } else {
                                    viewModel.onEvent(ExamEvent.PauseExam)
                                }
                            }
                        ) {
                            Text(if (uiState.isPaused) "继续" else "暂停")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.hasTimer) {
                TimerBar(timeRemaining = uiState.timeRemaining)
            }

            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !uiState.isPaused,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val question = uiState.questions.getOrNull(page)
                    if (question != null) {
                        QuestionCard(
                            question = question,
                            selectedAnswers = uiState.answers[question.id].orEmpty(),
                            onOptionSelected = { optionId ->
                                viewModel.onEvent(
                                    ExamEvent.SelectAnswer(
                                        questionId = question.id,
                                        optionId = optionId
                                    )
                                )
                                if (!uiState.showImmediateFeedback &&
                                    question.type != QuestionType.MULTIPLE_CHOICE &&
                                    page < uiState.questions.lastIndex
                                ) {
                                    coroutineScope.launch {
                                        delay(280)
                                        pagerState.animateScrollToPage(page + 1)
                                    }
                                }
                            },
                            revealed = question.id in uiState.revealedQuestionIds,
                            allowReveal = uiState.mode !is ExamMode.MockExam,
                            showAnswerSkill = uiState.showAnswerSkill,
                            showAnswerExplain = uiState.showAnswerExplain,
                            onRevealAnswer = {
                                viewModel.onEvent(ExamEvent.RevealAnswer(question.id))
                            }
                        )
                    }
                }

                if (uiState.isPaused) {
                    PausedOverlay(
                        onResume = { viewModel.onEvent(ExamEvent.ResumeExam) },
                        onExit = { viewModel.saveAndExit(onNavigateBack) }
                    )
                }
            }

            ExamNavigationBar(
                currentIndex = uiState.currentIndex,
                total = uiState.questions.size,
                enabled = !uiState.isPaused,
                onPrevious = { viewModel.onEvent(ExamEvent.PreviousQuestion) },
                onNext = { viewModel.onEvent(ExamEvent.NextQuestion) },
                onSubmit = { showSubmitDialog = true }
            )
        }

        if (showProgressSheet) {
            AnswerProgressSheet(
                questions = uiState.questions,
                answers = uiState.answers,
                currentIndex = uiState.currentIndex,
                onDismiss = { showProgressSheet = false },
                onQuestionClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                        showProgressSheet = false
                    }
                },
                onSubmit = {
                    showProgressSheet = false
                    showSubmitDialog = true
                }
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出当前练习？") },
            text = { Text("答题进度会自动保存，下次进入考试时会恢复原题和答案。") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.saveAndExit(onNavigateBack)
                }) {
                    Text("退出并保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续答题")
                }
            }
        )
    }

    if (showSubmitDialog) {
        val unanswered = uiState.questions.size - uiState.answers.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("确认交卷？") },
            text = {
                Text(
                    if (unanswered > 0) {
                        "还有 $unanswered 道题未作答，交卷后将立即计算成绩。"
                    } else {
                        "所有题目均已作答，交卷后将立即计算成绩。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitDialog = false
                    viewModel.onEvent(ExamEvent.SubmitExam)
                }) {
                    Text("交卷")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("继续答题")
                }
            }
        )
    }
}

@Composable
private fun ExamNavigationBar(
    currentIndex: Int,
    total: Int,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = enabled && currentIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("上一题")
            }
            if (currentIndex < total - 1) {
                Button(
                    onClick = onNext,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("下一题")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("交卷")
                }
            }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "考试已暂停",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "倒计时已停止，继续后恢复计时。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text("继续考试")
                }
                TextButton(onClick = onExit) {
                    Text("保存并返回")
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
