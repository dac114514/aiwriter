package com.faster.aiwriter.data

import kotlinx.serialization.Serializable

/**
 * 故事模板数据类
 */
@Serializable
data class StoryTemplate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val genre: StoryGenre = StoryGenre.FANTASY,
    val difficulty: DifficultyLevel = DifficultyLevel.NOVICE,
    val estimatedChapters: Int = 50,
    val systemPrompt: String = ""
)

/**
 * 故事类型枚举
 */
enum class StoryGenre(val displayName: String) {
    FANTASY("奇幻冒险"),
    SCI_FI("科幻"),
    MYSTERY("悬疑推理"),
    ANCIENT("古风"),
    ROMANCE("现代言情"),
    INFINITE("无限流")
}

/**
 * 难度等级枚举
 */
enum class DifficultyLevel(val displayName: String, val description: String) {
    NOVICE("新手", "简单直接的故事发展，适合初次体验"),
    INTERMEDIATE("进阶", "适度挑战，需要一些思考选择"),
    EXPERT("专家", "复杂情节，多重分支，高难度决策")
}

/**
 * 故事节点数据类
 */
@Serializable
data class StoryNode(
    val id: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String = "",
    val isBranchPoint: Boolean = false,
    val choices: List<Choice> = emptyList(),
    val nextNodeId: String? = null
)

/**
 * 选择项数据类
 */
@Serializable
data class Choice(
    val id: String = "",
    val text: String = "",
    val nextNodeId: String = "",
    val consequences: String = ""
)

/**
 * 故事进度数据类
 */
@Serializable
data class StoryProgress(
    val templateId: String = "",
    val currentChapter: Int = 1,
    val currentNodeId: String = "",
    val completedNodes: Set<String> = emptySet(),
    val playerChoices: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 大纲生成结果
 */
@Serializable
data class StoryOutline(
    val title: String = "",
    val summary: String = "",
    val characters: List<Character> = emptyList(),
    val plotStructure: List<PlotPoint> = emptyList(),
    val themes: List<String> = emptyList()
)

/**
 * 角色数据类
 */
@Serializable
data class Character(
    val name: String = "",
    val role: String = "", // 主角、配角、反派等
    val description: String = "",
    val personality: String = "",
    val background: String = ""
)

/**
 * 情节点数据类
 */
@Serializable
data class PlotPoint(
    val chapter: Int = 1,
    val title: String = "",
    val description: String = "",
    val type: PlotType = PlotType.EXPOSITION
)

/**
 * 情节类型枚举
 */
enum class PlotType(val displayName: String) {
    EXPOSITION("开端"),
    RISING_ACTION("发展"),
    CLIMAX("高潮"),
    FALLING_ACTION("回落"),
    RESOLUTION("结局")
}