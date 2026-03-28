# AI Story Weaver 简化版实施步骤

## 🎯 项目目标

基于现有AiChat Android应用，快速添加AI互动故事功能，接入个人API服务商生成小说内容。

## 📋 现有基础分析

### 已具备的功能
✅ **成熟的架构**: Jetpack Compose + MVVM
✅ **聊天系统**: 消息发送、接收、显示
✅ **数据持久化**: DataStore + Room数据库
✅ **API集成框架**: 支持自定义服务商
✅ **UI组件**: 现有的界面元素和主题

### 需要添加的功能
🎯 **故事模板管理**: 简单的预设故事类型
🎯 **分支选择**: 基本的互动选择推进
🎯 **进度保存**: 自动保存故事进度
🎯 **API配置**: 个人服务商接入

## 🔧 具体实施步骤

### Step 1: 创建故事数据模型 (第1天)

**文件**: `app/src/main/java/com/example/aichat/data/`

```kotlin
// StoryModels.kt
data class SimpleStoryTemplate(
    val id: Long,
    val title: String,
    val genre: String, // "冒险", "浪漫", "悬疑", "奇幻", "科幻"
    val systemPrompt: String,
    val initialScene: String
)

data class StoryNode(
    val id: String,
    val text: String,
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val id: String,
    val text: String,
    val nextNodeId: String? = null
)

data class StoryProgress(
    val templateId: Long,
    val currentNodeId: String,
    val choicesMade: List<String> = emptyList(),
    val lastPlayedTime: Long = System.currentTimeMillis()
)
```

**任务**:
- [ ] 创建StoryModels.kt文件
- [ ] 定义上述数据类
- [ ] 测试编译通过

---

### Step 2: 创建故事Repository (第2天)

**文件**: `app/src/main/java/com/example/aichat/data/`

```kotlin
// StoryRepository.kt
class StoryRepository(private val context: Context) {
    private val prefsRepo = PreferencesRepository(context)

    suspend fun getStoryTemplates(): List<SimpleStoryTemplate> {
        // 返回预设的故事模板列表
        return listOf(
            SimpleStoryTemplate(1, "神秘冒险", "冒险", "你是一个勇敢的探险家...", "start"),
            SimpleStoryTemplate(2, "浪漫邂逅", "浪漫", "在一个阳光明媚的下午...", "meeting"),
            SimpleStoryTemplate(3, "悬疑推理", "悬疑", "深夜，你接到一个神秘的电话...", "call")
        )
    }

    suspend fun saveProgress(progress: StoryProgress) {
        // 使用DataStore保存进度
        val key = stringPreferencesKey("story_progress_${progress.templateId}")
        val progressJson = Json.encodeToString(progress)
        context.dataStore.edit { prefs ->
            prefs[key] = progressJson
        }
    }

    suspend fun loadProgress(templateId: Long): StoryProgress? {
        val key = stringPreferencesKey("story_progress_$templateId")
        val progressJson = context.dataStore.data.first()[key]
        return if (progressJson != null) {
            Json.decodeFromString<StoryProgress>(progressJson)
        } else null
    }
}
```

**任务**:
- [ ] 创建StoryRepository.kt文件
- [ ] 实现基本的数据操作方法
- [ ] 测试数据读写功能

---

### Step 3: 创建StoryViewModel (第3天)

**文件**: `app/src/main/java/com/example/aichat/viewmodel/`

```kotlin
// StoryViewModel.kt
class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val storyRepository = StoryRepository(application)

    private val _currentStory = MutableStateFlow<StoryState>(StoryState.Idle)
    val currentStory: StateFlow<StoryState> = _currentStory.asStateFlow()

    private val _templates = MutableStateFlow<List<SimpleStoryTemplate>>(emptyList())
    val templates: StateFlow<List<SimpleStoryTemplate>> = _templates.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                val templates = storyRepository.getStoryTemplates()
                _templates.value = templates
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun startStory(templateId: Long) {
        viewModelScope.launch {
            try {
                val template = _templates.value.find { it.id == templateId }
                if (template != null) {
                    val savedProgress = storyRepository.loadProgress(templateId)
                    val initialNode = StoryNode(
                        id = template.initialScene,
                        text = "故事开始...",
                        choices = listOf(
                            Choice("choice1", "选择第一个选项", "next1"),
                            Choice("choice2", "选择第二个选项", "next2")
                        )
                    )

                    _currentStory.value = StoryState.Playing(
                        template = template,
                        currentNode = initialNode,
                        progress = savedProgress ?: StoryProgress(templateId, template.initialScene)
                    )
                }
            } catch (e: Exception) {
                _currentStory.value = StoryState.Error(e.message ?: "启动故事失败")
            }
        }
    }

    fun makeChoice(choiceId: String) {
        viewModelScope.launch {
            val state = _currentStory.value
            if (state is StoryState.Playing) {
                val newChoices = state.progress.choicesMade + choiceId
                val updatedProgress = state.progress.copy(
                    currentNodeId = choiceId,
                    choicesMade = newChoices,
                    lastPlayedTime = System.currentTimeMillis()
                )

                // 保存进度
                storyRepository.saveProgress(updatedProgress)

                // 生成下一个场景
                generateNextScene(state.template, choiceId, updatedProgress)
            }
        }
    }

    private suspend fun generateNextScene(
        template: SimpleStoryTemplate,
        choiceId: String,
        progress: StoryProgress
    ) {
        try {
            val settings = PreferencesRepository(application).settingsFlow.first()
            val chatApi = ChatApi()

            // 构建对话历史
            val messages = buildList<ChatMessage> {
                add(ChatMessage(role = "system", content = template.systemPrompt))
                progress.choicesMade.forEachIndexed { index, choice ->
                    add(ChatMessage(role = "user", content = "选择: $choice"))
                    if (index < progress.choicesMade.size - 1) {
                        add(ChatMessage(role = "assistant", content = "故事继续..."))
                    }
                }
                add(ChatMessage(role = "user", content = "根据刚才的选择继续故事"))
            }

            var generatedText = ""
            chatApi.streamChat(
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = settings.model,
                systemPrompt = template.systemPrompt,
                messages = messages,
                onDelta = { delta -> generatedText += delta }
            )

            val nextNode = StoryNode(
                id = choiceId,
                text = generatedText,
                choices = listOf(
                    Choice("continue1", "继续探索", "end_good"),
                    Choice("continue2", "换个方向", "end_bad")
                )
            )

            _currentStory.value = StoryState.Playing(
                template = template,
                currentNode = nextNode,
                progress = progress
            )
        } catch (e: Exception) {
            _currentStory.value = StoryState.Error("生成故事内容失败: ${e.message}")
        }
    }
}

sealed class StoryState {
    object Idle : StoryState()
    data class Playing(
        val template: SimpleStoryTemplate,
        val currentNode: StoryNode,
        val progress: StoryProgress
    ) : StoryState()
    data class Error(val message: String) : StoryState()
}
```

**任务**:
- [ ] 创建StoryViewModel.kt文件
- [ ] 实现故事状态管理和API调用
- [ ] 测试故事生成和选择逻辑

---

### Step 4: 创建故事界面 (第4天)

**文件**: `app/src/main/java/com/example/aichat/ui/screens/`

```kotlin
// StoryScreen.kt
@Composable
fun StoryScreen(
    viewModel: StoryViewModel,
    onBackToMenu: () -> Unit
) {
    val currentStory by viewModel.currentStory.collectAsState()
    val templates by viewModel.templates.collectAsState()

    when (val state = currentStory) {
        is StoryState.Idle -> {
            StorySelectionScreen(
                templates = templates,
                onSelectTemplate = viewModel::startStory
            )
        }
        is StoryState.Playing -> {
            StoryReadingScreen(
                node = state.currentNode,
                onChoiceSelected = viewModel::makeChoice,
                onBackToMenu = onBackToMenu
            )
        }
        is StoryState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = { /* 重试逻辑 */ },
                onBackToMenu = onBackToMenu
            )
        }
    }
}

@Composable
private fun StoryReadingScreen(
    node: StoryNode,
    onChoiceSelected: (String) -> Unit,
    onBackToMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 故事文本区域
        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = node.text,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // 选择按钮区域
        if (node.choices.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                node.choices.forEach { choice ->
                    Button(
                        onClick = { onChoiceSelected(choice.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(choice.text)
                    }
                }
            }
        }

        // 底部导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBackToMenu) {
                Text("返回菜单")
            }
        }
    }
}

@Composable
private fun StorySelectionScreen(
    templates: List<SimpleStoryTemplate>,
    onSelectTemplate: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "选择故事类型",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { template ->
                Card(
                    onClick = { onSelectTemplate(template.id) },
                    modifier = Modifier.height(120.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.genre,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

**任务**:
- [ ] 创建StoryScreen.kt文件
- [ ] 实现故事选择和阅读界面
- [ ] 测试UI交互功能

---

### Step 5: 更新主应用和导航 (第5天)

**文件**: `app/src/main/java/com/example/aichat/ui/AiChatApp.kt`

```kotlin
// 在现有导航中添加故事路由
NavHost(
    navController = navController,
    startDestination = "chat"
) {
    composable("chat") {
        ChatScreen(
            viewModel = viewModel,
            onNavigateToSetup = { navController.navigate("setup") },
            onNavigateToHistory = { navController.navigate("history") },
            onNavigateToStory = { navController.navigate("story") }
        )
    }

    composable("story") {
        StoryScreen(
            viewModel = StoryViewModel(application),
            onBackToMenu = { navController.popBackStack() }
        )
    }

    // ... 其他现有路由
}
```

**文件**: `app/src/main/java/com/example/aichat/MainActivity.kt`

```kotlin
// 确保Application类支持Hilt
@HiltAndroidApp
class AiChatApplication : Application()

// MainActivity中获取Application实例
class MainActivity : ComponentActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = requireNotNull(this.application) as AiChatApplication
        val viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]

        setContent {
            AiChatApp(viewModel = viewModel)
        }
    }
}
```

**任务**:
- [ ] 更新AiChatApp.kt添加故事路由
- [ ] 配置Hilt依赖注入
- [ ] 更新MainActivity.kt
- [ ] 测试应用启动和导航

---

### Step 6: 测试和完善 (第6-7天)

**任务**:
- [ ] 测试故事模板加载
- [ ] 验证API连接和故事生成
- [ ] 检查进度保存和恢复
- [ ] 测试不同选择路径
- [ ] 修复发现的bug
- [ ] 优化用户体验

**交付物**: 可用的AI互动故事应用

---

## 🚀 快速部署

### 第1周完成以下步骤:
1. **Day 1**: 创建数据模型
2. **Day 2**: 实现Repository
3. **Day 3**: 开发ViewModel
4. **Day 4**: 创建UI界面
5. **Day 5**: 更新导航和应用
6. **Day 6-7**: 测试和完善

### 配置API密钥
在应用的设置页面添加API配置:
```kotlin
// 在现有设置界面中添加
OutlinedTextField(
    value = apiKey,
    onValueChange = { viewModel.updateApiKey(it) },
    label = { Text("API密钥") },
    placeholder = { Text("输入您的API密钥") }
)
```

---

## 📱 最终成果

**应用功能**:
- ✅ 选择故事类型开始阅读
- ✅ AI生成故事内容
- ✅ 做出选择推进故事
- ✅ 自动保存阅读进度
- ✅ 返回菜单重新开始

**技术特点**:
- ✅ 基于现有成熟架构
- ✅ 无需复杂新功能
- ✅ 个人API服务商支持
- ✅ 简洁易用的界面

这个实施步骤让您能够快速在现有AiChat应用基础上添加AI互动故事功能，专注于核心需求，避免不必要的复杂性。