# AI Story Weaver 技术规格说明书

## 1. 核心数据模型设计

### 1.1 故事模板系统
```kotlin
// 故事类型枚举
enum class StoryGenre(val displayName: String, val systemPrompt: String) {
    ADVENTURE("冒险", "你是一个充满想象力的冒险故事讲述者，创造引人入胜的冒险情节"),
    ROMANCE("浪漫", "你是一个擅长描写情感和关系的故事作家，注重人物内心世界的刻画"),
    MYSTERY("悬疑", "你是一个精于布局的悬疑故事大师，善于制造悬念和反转"),
    FANTASY("奇幻", "你是一个富有创意的奇幻故事创作者，擅长构建魔法世界和神秘生物"),
    SCIENCE_FICTION("科幻", "你是一个有远见的科幻作家，善于描绘未来科技和人类进化")
}

enum class DifficultyLevel(val displayName: String) {
    EASY("简单"), MEDIUM("中等"), HARD("困难")
}

// 故事模板主数据类
data class StoryTemplate(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val genre: StoryGenre,
    val difficulty: DifficultyLevel,
    val estimatedDuration: Int, // 预计游玩时间（分钟）
    val systemPrompt: String,
    val initialSceneId: String,
    val nodes: List<StoryNode> = emptyList(),
    val choices: List<ChoiceOption> = emptyList(),
    val endings: List<Ending> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 故事节点基类
sealed class StoryNode {
    abstract val id: String
    abstract val text: String
    abstract val nextNodeId: String?
}

// 场景节点
data class SceneNode(
    override val id: String,
    override val text: String,
    override val nextNodeId: String? = null,
    val imagePrompt: String? = null, // 预留给未来图像功能
    val backgroundMusic: String? = null,
    val choices: List<String> = emptyList() // 指向的choice ID列表
) : StoryNode()

// 选择节点
data class ChoiceNode(
    override val id: String,
    override val text: String,
    override val nextNodeId: String? = null,
    val conditions: List<ChoiceCondition> = emptyList(),
    val effects: List<PlayerEffect> = emptyList()
) : StoryNode()

// 结局节点
data class EndingNode(
    override val id: String,
    override val text: String,
    override val nextNodeId: String? = null,
    val isGoodEnding: Boolean,
    val unlockAchievements: List<String> = emptyList(),
    val finalStats: PlayerStats? = null
) : StoryNode()
```

### 1.2 玩家状态系统
```kotlin
// 玩家属性
data class PlayerStats(
    val health: Int = 100,
    val courage: Int = 50,
    val wisdom: Int = 50,
    val charm: Int = 50,
    val strength: Int = 50,
    val intelligence: Int = 50
) {
    fun canPerformAction(action: String): Boolean {
        return when (action) {
            "brave_action" -> courage >= 30
            "wise_decision" -> wisdom >= 40
            "charming_talk" -> charm >= 35
            "physical_combat" -> strength >= 45
            "intellectual_solution" -> intelligence >= 40
            else -> true
        }
    }
}

// 选择条件
data class ChoiceCondition(
    val type: ConditionType,
    val statName: String,
    val requiredValue: Int,
    val comparison: ComparisonOperator
)

enum class ConditionType { STAT, ACHIEVEMENT, PREVIOUS_CHOICE }
enum class ComparisonOperator { GREATER_THAN, LESS_THAN, EQUALS }

// 选择效果
data class PlayerEffect(
    val statName: String,
    val changeAmount: Int,
    val description: String
)
```

### 1.3 进度保存系统
```kotlin
// 故事进度数据类
@Entity(tableName = "story_progress")
data class StoryProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val currentNodeId: String,
    val playerStats: PlayerStats,
    val choicesMade: List<ChoiceRecord>,
    val unlockedEndings: Set<String>,
    val lastPlayedTime: Long,
    val playCount: Int,
    val completed: Boolean = false
)

// 选择记录
data class ChoiceRecord(
    val nodeId: String,
    val choiceText: String,
    val timestamp: Long,
    val statsBefore: PlayerStats,
    val statsAfter: PlayerStats
)

// 成就系统
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val unlockedBy: List<String> = emptyList() // 解锁条件
)
```

## 2. 业务逻辑层设计

### 2.1 故事引擎核心
```kotlin
class StoryEngine(private val repository: StoryRepository) {

    private val _currentState = MutableStateFlow(StoryState.initial())
    val currentState: StateFlow<StoryState> = _currentState.asStateFlow()

    suspend fun startStory(templateId: Long): Result<Unit> {
        return try {
            val template = repository.getTemplate(templateId)
            val progress = repository.getProgress(templateId) ?: createNewProgress(template)
            _currentState.value = StoryState(
                template = template,
                progress = progress,
                currentNode = template.nodes.first { it.id == progress.currentNodeId }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun makeChoice(choiceId: String): Result<Unit> {
        val state = _currentState.value
        val choice = findChoiceById(choiceId) ?: return Result.failure(Exception("无效选择"))

        // 验证条件
        if (!evaluateConditions(choice.conditions, state.progress.playerStats)) {
            return Result.failure(Exception("条件不满足"))
        }

        // 应用效果
        val newStats = applyEffects(choice.effects, state.progress.playerStats)
        val newChoices = state.progress.choicesMade + ChoiceRecord(
            nodeId = state.currentNode.id,
            choiceText = choice.text,
            timestamp = System.currentTimeMillis(),
            statsBefore = state.progress.playerStats,
            statsAfter = newStats
        )

        // 更新进度
        val updatedProgress = state.progress.copy(
            currentNodeId = choice.nextNodeId ?: generateEnding(state),
            playerStats = newStats,
            choicesMade = newChoices,
            lastPlayedTime = System.currentTimeMillis()
        )

        // 保存进度
        repository.saveProgress(updatedProgress)

        // 更新状态
        _currentState.value = state.copy(
            progress = updatedProgress,
            currentNode = findNextNode(choice.nextNodeId ?: generateEnding(state))
        )

        return Result.success(Unit)
    }

    private fun evaluateConditions(
        conditions: List<ChoiceCondition>,
        stats: PlayerStats
    ): Boolean {
        return conditions.all { condition ->
            val actualValue = when (condition.statName) {
                "courage" -> stats.courage
                "wisdom" -> stats.wisdom
                "charm" -> stats.charm
                "strength" -> stats.strength
                "intelligence" -> stats.intelligence
                else -> 0
            }

            when (condition.comparison) {
                ComparisonOperator.GREATER_THAN -> actualValue > condition.requiredValue
                ComparisonOperator.LESS_THAN -> actualValue < condition.requiredValue
                ComparisonOperator.EQUALS -> actualValue == condition.requiredValue
            }
        }
    }
}
```

### 2.2 AI集成服务
```kotlin
interface AIService {
    suspend fun generateStoryContent(
        context: StoryContext,
        prompt: String
    ): Result<String>

    suspend fun evaluateChoiceOptions(
        currentScene: String,
        playerStats: PlayerStats,
        availableChoices: List<String>
    ): Result<List<ChoiceEvaluation>>
}

class OpenAIAIService : AIService {
    override suspend fun generateStoryContent(
        context: StoryContext,
        prompt: String
    ): Result<String> {
        return try {
            val response = chatApi.streamChat(
                baseUrl = context.baseUrl,
                apiKey = context.apiKey,
                model = context.model,
                systemPrompt = context.systemPrompt,
                messages = context.messages,
                onDelta = { delta ->
                    // 实时处理AI响应
                }
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StoryContext(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String,
    val messages: List<ChatMessage>
)
```

## 3. UI界面设计规范

### 3.1 沉浸式阅读界面
```kotlin
@Composable
fun StoryReadingScreen(
    viewModel: StoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.currentState.collectAsState()
    val theme by viewModel.theme.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // 背景渐变
        AnimatedGradientBackground(
            colors = theme.backgroundColors,
            animationSpec = tween(2000)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 故事内容区域
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = state.currentNode.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 28.sp
                            ),
                            color = theme.textColor,
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }

            // 选择按钮区域
            if (state.currentNode is ChoiceNode) {
                ChoiceButtonsSection(
                    choices = state.availableChoices,
                    onChoiceSelected = viewModel::makeChoice
                )
            }

            // 底部导航栏
            BottomNavigationBar(
                onBackToMenu = viewModel::navigateToMenu,
                onSaveProgress = viewModel::saveProgress,
                onLoadProgress = viewModel::showProgressDialog
            )
        }
    }
}
```

### 3.2 动画效果规范
- **页面切换**: 滑动过渡动画，持续时间300ms
- **文本显示**: 逐字显示效果，打字机风格
- **选择按钮**: 弹性点击反馈，缩放动画
- **状态变化**: 平滑的颜色和位置过渡

## 4. 数据库设计

### 4.1 Room数据库配置
```kotlin
@Database(
    entities = [
        StoryTemplateEntity::class,
        StoryProgressEntity::class,
        ChoiceRecordEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun progressDao(): ProgressDao
    abstract fun achievementDao(): AchievementDao
}

@Entity(tableName = "story_templates")
data class StoryTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val genre: String,
    val difficulty: String,
    val estimatedDuration: Int,
    val systemPrompt: String,
    val initialSceneId: String,
    val nodesJson: String, // JSON序列化
    val choicesJson: String,
    val endingsJson: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 4.2 缓存策略
- **内存缓存**: LRU缓存最近访问的故事模板
- **磁盘缓存**: SQLite数据库存储用户进度
- **网络缓存**: OkHttp缓存AI API响应
- **预加载**: 启动时预加载常用模板

## 5. 性能优化方案

### 5.1 内存管理
- **对象池**: 重用StoryNode等频繁创建的对象
- **懒加载**: 延迟加载大型故事模板
- **图片压缩**: 使用WebP格式减少内存占用
- **GC优化**: 避免在UI线程创建大对象

### 5.2 启动优化
- **异步初始化**: 后台预加载必要资源
- **分包加载**: 按需加载功能模块
- **资源预取**: 预测用户行为提前加载
- **冷启动优化**: 最小化初始包大小

### 5.3 电池优化
- **网络节流**: 限制不必要的API调用
- **后台任务**: 使用WorkManager调度非紧急任务
- **传感器管理**: 及时释放不需要的传感器
- **定位优化**: 使用低功耗定位模式

## 6. 测试策略

### 6.1 单元测试
- **故事引擎**: 测试分支逻辑和条件判断
- **数据层**: 测试数据库操作和缓存机制
- **AI服务**: 模拟API响应和错误处理

### 6.2 集成测试
- **UI流程**: 测试完整的故事体验流程
- **数据同步**: 测试本地和云端数据一致性
- **性能监控**: 集成Firebase Performance Monitoring

### 6.3 用户测试
- **A/B测试**: 不同UI设计的用户体验对比
- **可用性测试**: 观察用户使用过程
- **反馈收集**: 内置反馈机制和评分系统

## 7. 发布准备

### 7.1 应用商店要求
- **隐私政策**: 符合Google Play隐私政策
- **权限声明**: 清晰说明所需权限
- **截图展示**: 高质量的应用截图和视频
- **版本历史**: 详细的更新日志

### 7.2 监控体系
- **崩溃报告**: Firebase Crashlytics实时监控
- **性能分析**: Firebase Performance Monitoring
- **用户行为**: Firebase Analytics追踪关键指标
- **A/B测试**: Optimize进行功能实验

这个技术规格为AI Story Weaver的开发提供了详细的技术指导，确保项目能够高质量地完成。