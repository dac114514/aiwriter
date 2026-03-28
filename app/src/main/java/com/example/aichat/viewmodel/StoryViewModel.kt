package com.faster.aiwriter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.aiwriter.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.faster.aiwriter.data.CustomStorySettings

data class CustomStorySettings(
    val characterDescription: String = "",
    val backgroundDescription: String = "",
    val specialRequirements: String = ""
)

/**
 * 故事视图模型 - 管理故事相关的状态和操作
 */
class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val storyRepository = StoryRepository(application)
    private val storyGenerator = StoryGenerator()

    // 故事模板列表
    private val _templates = MutableStateFlow<List<StoryTemplate>>(emptyList())
    val templates: StateFlow<List<StoryTemplate>> = _templates.asStateFlow()

    // 当前选中的模板
    private val _selectedTemplate = MutableStateFlow<StoryTemplate?>(null)
    val selectedTemplate: StateFlow<StoryTemplate?> = _selectedTemplate.asStateFlow()

    // 故事进度
    private val _storyProgress = MutableStateFlow<StoryProgress?>(null)
    val storyProgress: StateFlow<StoryProgress?> = _storyProgress.asStateFlow()

    // 当前章节
    private val _currentChapter = MutableStateFlow<StoryNode?>(null)
    val currentChapter: StateFlow<StoryNode?> = _currentChapter.asStateFlow()

    // 可用选择项
    private val _availableChoices = MutableStateFlow<List<Choice>>(emptyList())
    val availableChoices: StateFlow<List<Choice>> = _availableChoices.asStateFlow()

    // 大纲数据
    private val _storyOutline = MutableStateFlow<StoryOutline?>(null)
    val storyOutline: StateFlow<StoryOutline?> = _storyOutline.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        observeStoryProgress()
    }

    /**
     * 加载故事模板
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            _templates.value = storyRepository.getDefaultTemplates()
        }
    }

    /**
     * 观察故事进度变化
     */
    private fun observeStoryProgress() {
        viewModelScope.launch {
            storyRepository.getStoryProgress().collect { progress ->
                _storyProgress.value = progress
                if (progress != null) {
                    loadCurrentChapter(progress.currentNodeId)
                }
            }
        }
    }

    /**
     * 选择故事模板
     */
    fun selectTemplate(template: StoryTemplate) {
        _selectedTemplate.value = template
        viewModelScope.launch {
            storyRepository.saveCurrentTemplate(template.id)
        }
    }

    /**
     * 开始新故事
     */
    fun startNewStory(customSettings: CustomStorySettings) {
        val template = _selectedTemplate.value ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 生成故事大纲
                val outlineResult = storyGenerator.generateStoryOutline(template, customSettings)
                if (outlineResult.isSuccess) {
                    _storyOutline.value = outlineResult.getOrNull()
                    storyRepository.saveOutlineCache(outlineResult.getOrNull()!!, template.id)

                    // 创建初始章节
                    val initialChapter = createInitialChapter(template, customSettings)
                    _currentChapter.value = initialChapter

                    // 保存故事进度
                    val progress = StoryProgress(
                        templateId = template.id,
                        currentChapter = 1,
                        currentNodeId = initialChapter.id
                    )
                    storyRepository.saveStoryProgress(progress)
                    _storyProgress.value = progress

                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = outlineResult.exceptionOrNull()?.message ?: "生成大纲失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * 创建初始章节
     */
    private fun createInitialChapter(template: StoryTemplate, settings: CustomStorySettings): StoryNode {
        return StoryNode(
            id = System.currentTimeMillis().toString(),
            chapterNumber = 1,
            title = "故事开端",
            content = """
                ${template.name}开始了...

                这是一个充满${template.genre.displayName}的世界。根据你的设定，角色${if (settings.characterDescription.isNotEmpty()) settings.characterDescription else "勇敢的主角"}正在探索这个神秘的地方。

                故事背景：${if (settings.backgroundDescription.isNotEmpty()) settings.backgroundDescription else "一个引人入胜的故事世界"}

                ${if (settings.specialRequirements.isNotEmpty()) "特殊要求：${settings.specialRequirements}" else ""}

                请继续这个故事，描述接下来会发生什么...
            """.trimIndent(),
            isBranchPoint = false
        )
    }

    /**
     * 继续故事（下一章）
     */
    fun continueStory(userChoice: String? = null) {
        val currentChapter = _currentChapter.value
        val template = _selectedTemplate.value
        val progress = _storyProgress.value

        if (currentChapter == null || template == null || progress == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 生成下一章
                val chapterResult = storyGenerator.generateChapter(
                    currentNode = currentChapter,
                    template = template,
                    progress = progress,
                    userChoice = userChoice
                )

                if (chapterResult.isSuccess) {
                    val newChapter = chapterResult.getOrNull()!!
                    _currentChapter.value = newChapter

                    // 更新进度
                    val updatedProgress = progress.copy(
                        currentChapter = newChapter.chapterNumber,
                        currentNodeId = newChapter.id
                    )
                    storyRepository.saveStoryProgress(updatedProgress)
                    _storyProgress.value = updatedProgress

                    // 如果需要选择，生成分支选项
                    if (newChapter.isBranchPoint) {
                        generateChoicesForChapter(newChapter, template, updatedProgress)
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = chapterResult.exceptionOrNull()?.message ?: "生成章节失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * 生成分支选择
     */
    private fun generateChoicesForChapter(currentChapter: StoryNode, template: StoryTemplate, progress: StoryProgress) {
        viewModelScope.launch {
            val choicesResult = storyGenerator.generateChoices(currentChapter, template, progress)

            if (choicesResult.isSuccess) {
                _availableChoices.value = choicesResult.getOrNull() ?: emptyList()
            }
        }
    }

    /**
     * 做出选择
     */
    fun makeChoice(choice: Choice) {
        val currentChapter = _currentChapter.value
        val template = _selectedTemplate.value
        val progress = _storyProgress.value

        if (currentChapter == null || template == null || progress == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 记录选择
                val updatedChoices = progress.playerChoices + ("chapter_${currentChapter.chapterNumber}" to choice.text)
                val updatedProgress = progress.copy(playerChoices = updatedChoices)

                // 生成基于选择的下一章
                val chapterResult = storyGenerator.generateChapter(
                    currentNode = currentChapter,
                    template = template,
                    progress = updatedProgress,
                    userChoice = choice.text
                )

                if (chapterResult.isSuccess) {
                    val newChapter = chapterResult.getOrNull()!!
                    _currentChapter.value = newChapter

                    // 更新进度
                    val finalProgress = updatedProgress.copy(
                        currentChapter = newChapter.chapterNumber,
                        currentNodeId = newChapter.id
                    )
                    storyRepository.saveStoryProgress(finalProgress)
                    _storyProgress.value = finalProgress

                    // 清除当前选择
                    _availableChoices.value = emptyList()

                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = chapterResult.exceptionOrNull()?.message ?: "处理选择失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * 加载当前章节
     */
    private fun loadCurrentChapter(nodeId: String) {
        // 这里可以从缓存或重新生成加载章节
        // 为了简化，我们假设章节已经存在
    }

    /**
     * 重新开始故事
     */
    fun restartStory() {
        viewModelScope.launch {
            storyRepository.clearStoryProgress()
            _storyProgress.value = null
            _currentChapter.value = null
            _availableChoices.value = emptyList()
            _storyOutline.value = null
            _uiState.value = StoryUiState()
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取故事统计信息
     */
    fun getStoryStats(): StoryStats {
        val progress = _storyProgress.value
        val currentChapter = _currentChapter.value
        val template = _selectedTemplate.value

        return StoryStats(
            totalChapters = template?.estimatedChapters ?: 0,
            currentChapter = progress?.currentChapter ?: 1,
            completionPercentage = if (template != null && progress != null) {
                ((progress.currentChapter.toFloat() / template.estimatedChapters) * 100).toInt()
            } else 0,
            hasChoicesAvailable = _availableChoices.value.isNotEmpty(),
            playerChoicesCount = progress?.playerChoices?.size ?: 0
        )
    }
}

/**
 * 故事UI状态数据类
 */
data class StoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGeneratingOutline: Boolean = false,
    val isGeneratingChapter: Boolean = false
)

/**
 * 故事统计数据类
 */
data class StoryStats(
    val totalChapters: Int,
    val currentChapter: Int,
    val completionPercentage: Int,
    val hasChoicesAvailable: Boolean,
    val playerChoicesCount: Int
)