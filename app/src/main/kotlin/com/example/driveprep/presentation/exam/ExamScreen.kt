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
        // 由 ViewModel 决定:同模式会话进行中则复用,否则按需恢复进度或开新会话
        viewModel.onEvent(ExamEvent.StartExam(mode))
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

    BackHandler(enabled = uiState.interactedThisSession && !uiState.isFinished) {
        showExitDialog = true
    }

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    if (uiState.mode is ExamMode.Completed) {
        val score = (uiState.mode as ExamMode.Completed).finalScore
        // "查看错题"只复习实际作答且答错的题,与错题本规则一致
        val reviewableIds = score.wrongQuestions
            .filter { it.userAnswer.isNotEmpty() }
            .map { it.questionId }
        ExamResultScreen(
            score = score,
            hasReviewableWrong = reviewableIds.isNotEmpty(),
            onReviewWrongQuestions = {
                viewModel.onEvent(ExamEvent.StartQuestionReview(reviewableIds))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (uiState.mode) {
                                    is ExamMode.ErrorReview -> if (uiState.subject == 4) "科目四错题复习" else "错题复习"
                                    is ExamMode.Practice -> if (uiState.subject == 4) "科目四随机练习" else "随机练习"
                                    else -> if (uiState.subject == 4) "科目四模拟考试" else "模拟考试"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            // 模拟考试:倒计时直接跟在标题后面
                            if (uiState.hasTimer) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = formatTime(uiState.timeRemaining),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.timeRemaining < 5 * 60 * 1000L) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            // 本次会话作答过才弹确认框,否则直接退出(进度本就自动保存)
                            if (uiState.interactedThisSession && !uiState.isFinished) {
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
                                // 自动跳下一题只用于模拟考试提速;
                                // 练习/错题复习要留在当前题查看答案与解析
                                if (uiState.mode is ExamMode.MockExam &&
                                    !uiState.showImmediateFeedback &&
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
                            questionFontSizeSp = uiState.questionFontSizeSp,
                            onRevealAnswer = {
                                viewModel.onEvent(ExamEvent.RevealAnswer(question.id))
                            }
                        )
                    }
                }

                if (uiState.isPaused) {
                    PausedOverlay(
                        onResume = { viewModel.onEvent(ExamEvent.ResumeExam) },
                        onRestart = {
                            viewModel.onEvent(ExamEvent.RestartExam)
                        },
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
                        // 瞬时直达目标页:动画滚动会途经中间页,导致依次闪过其他题目
                        pagerState.scrollToPage(index)
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
            text = { Text("「退出并保存」保留进度，下次进入可继续；「直接退出」将丢弃本次答题记录。") },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续答题")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showExitDialog = false
                        viewModel.discardAndExit(onNavigateBack)
                    }) {
                        Text("直接退出", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        showExitDialog = false
                        viewModel.saveAndExit(onNavigateBack)
                    }) {
                        Text("退出并保存")
                    }
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
private fun PausedOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
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
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("重考(重新抽题)")
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
