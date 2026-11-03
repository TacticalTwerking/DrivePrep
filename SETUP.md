# 项目设置说明

## 下一步工作

### 1. 添加题库数据
- 从 https://github.com/doupoa/DrivingTestSubjectOne 下载 `q.json`
- 将文件放到 `app/src/main/assets/q.json`

### 2. 需要完善的功能

#### Repository 实现
- `ExamRepositoryImpl.kt` 中的方法需要完整实现
- 特别是 `loadQuestionsFromAssets()` - 首次启动时导入题库
- `getRandomExamQuestions()` - 随机抽取100题用于模拟考试

#### UI 组件
以下组件需要创建：
- `ExamScreen.kt` - 考试主界面
- `QuestionCard.kt` - 题目卡片（支持单选/多选/判断）
- `AnswerSheet.kt` - 答题卡导航
- `OptionItem.kt` - 选项组件
- `TimerBar.kt` - 倒计时条
- `ResultScreen.kt` - 成绩界面

#### Navigation
- 添加 Navigation Compose 依赖
- 创建导航图连接各个界面

#### 首次启动
- 在 `MainActivity` 或 `HomeViewModel` 中检查数据库是否为空
- 如果为空，调用 `loadQuestionsFromAssets()` 导入题库

### 3. 数据映射

需要在 `QuestionMapper.kt` 中实现：
```kotlin
fun QuestionEntity.toDomain(): Question {
    val optionIds = json.decodeFromString<List<String>>(itemsTitleArray)
    val optionTexts = json.decodeFromString<List<String>>(itemsDescArray)
    
    return Question(
        id = id,
        type = when(type) {
            1 -> QuestionType.SINGLE_CHOICE
            2 -> QuestionType.MULTIPLE_CHOICE
            3 -> QuestionType.TRUE_FALSE
            else -> QuestionType.SINGLE_CHOICE
        },
        question = question,
        imageUrl = imageUrl,
        options = optionIds.zip(optionTexts) { id, text ->
            Option(id, text)
        },
        correctAnswer = answer,
        chapterId = chapterId,
        difficulty = difficulty,
        errorRate = errorRate.toFloatOrNull() ?: 0f,
        answerSkill = answerSkill,
        answerSkillExplain = answerSkillExplain,
        remark = remark,
        videoExplain = if (aliyunVid != null) {
            VideoExplain(aliyunVid, coverUrl)
        } else null,
        isNewRule = newRuleFlag,
        isErrorProne = errorProneFlag
    )
}
```

### 4. 测试建议

创建简单的测试验证：
- Room DAO 的 CRUD 操作
- Repository 的数据转换
- ViewModel 的状态更新逻辑
- 答题正确性判断（单选/多选/判断）

### 5. 性能优化

后续可以考虑：
- 题库分页加载（使用 Paging 3）
- 图片预加载策略
- 答题记录统计分析
- 题目收藏功能

## 当前架构优势

✅ **清晰的分层** - data/domain/presentation 职责明确
✅ **状态管理** - 单向数据流，状态可预测
✅ **依赖注入** - Hilt 管理依赖，易于测试
✅ **离线优先** - Room 本地存储，完全离线可用
✅ **可扩展** - 易于添加新题型、新功能

## 常见问题

Q: 为什么选择 Room 而不是直接读 JSON？
A: Room 提供类型安全的查询、索引优化、事务支持，适合频繁查询和更新的场景

Q: 多选题如何判断答对？
A: 将用户选择的 Set<String> 与正确答案拆分后的 Set 进行完全匹配

Q: 如何处理考试中途退出？
A: ExamProgressDao 自动保存状态，下次启动时 restoreExamProgress() 恢复

Q: 图片加载失败怎么办？
A: Coil 自动重试，可配置占位图和错误图


TODO

### 已完成
- [x] 错题按错误次数累计，错题本按错误次数从高到低排序
- [x] 设置页支持“立即显示对错”“显示答题技巧”“显示答案解析”
- [x] 答题卡支持最近题目卡片和快速跳题，保留题号网格
- [x] 模拟考试倒计时、暂停/继续、时间到自动交卷
- [x] 错题专项练习，可从错题本或成绩页进入
- [x] 题目图片最大高度限制为 240dp
- [x] 返回练习/考试前弹出确认，并保存进度
- [x] 练习报告页：练习次数、平均分、最高分、通过率、高频错题和记录列表
- [x] 多选题选项乱序，仅打乱展示顺序，不影响答案 ID
- [x] 数据字段利用：难度、易错率、新规、高频易错、答案解析、法规说明

### 后续可选
- [ ] 视频讲解播放（题库已有 `aliyVid` / `coverUrl` 字段）
- [ ] 章节选择页（当前章节练习随机抽题）
- [ ] 题库分页加载（Paging 3）
- [ ] 题目收藏、按知识点聚合统计
