package com.faster.aiwriter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 故事生成器 - 负责调用AI API生成故事内容和大纲
 */
class StoryGenerator {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 生成故事大纲
     */
    suspend fun generateStoryOutline(
        template: StoryTemplate,
        customSettings: CustomStorySettings
    ): Result<StoryOutline> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildOutlinePrompt(template, customSettings)
            val response = callOpenAIApi(prompt)

            if (response.isSuccessful) {
                val content = extractContentFromResponse(response.body?.string())
                val outline = parseOutline(content, template)
                Result.success(outline)
            } else {
                Result.failure(Exception("API调用失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成故事章节
     */
    suspend fun generateChapter(
        currentNode: StoryNode?,
        template: StoryTemplate,
        progress: StoryProgress,
        userChoice: String? = null
    ): Result<StoryNode> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildChapterPrompt(currentNode, template, progress, userChoice)
            val response = callOpenAIApi(prompt)

            if (response.isSuccessful) {
                val content = extractContentFromResponse(response.body?.string())
                val chapter = parseChapter(content, currentNode)
                Result.success(chapter)
            } else {
                Result.failure(Exception("API调用失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成分支选择
     */
    suspend fun generateChoices(
        currentNode: StoryNode,
        template: StoryTemplate,
        progress: StoryProgress
    ): Result<List<Choice>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildChoicesPrompt(currentNode, template, progress)
            val response = callOpenAIApi(prompt)

            if (response.isSuccessful) {
                val content = extractContentFromResponse(response.body?.string())
                val choices = parseChoices(content)
                Result.success(choices)
            } else {
                Result.failure(Exception("API调用失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建大纲生成提示词
     */
    private fun buildOutlinePrompt(template: StoryTemplate, settings: CustomStorySettings): String {
        return """
            请为以下故事类型创建一个详细的大纲：

            故事类型：${template.genre.displayName}
            难度等级：${template.difficulty.displayName}
            预计章节数：${template.estimatedChapters}

            角色设定：${settings.characterDescription}
            故事背景：${settings.backgroundDescription}
            特殊要求：${settings.specialRequirements}

            请按照以下格式返回：
            # 故事标题
            [简洁有力的故事标题]

            ## 故事概要
            [50-100字的整体故事描述]

            ## 主要角色
            1. [角色名称] - [角色定位]
               - 性格特点：[详细描述]
               - 背景故事：[简要介绍]
            （至少3个主要角色）

            ## 情节结构
            第1章：[章节标题]
            - 类型：开端
            - 内容：[简要描述]

            第2章：[章节标题]
            - 类型：发展
            - 内容：[简要描述]

            （按照开端、发展、高潮、回落、结局的结构，规划8-12个关键情节点）

            ## 核心主题
            - [主题1]
            - [主题2]
            - [主题3]

            注意：请确保故事符合${template.difficulty.displayName}难度，包含足够的冲突和选择机会。
        """.trimIndent()
    }

    /**
     * 构建章节生成提示词
     */
    private fun buildChapterPrompt(
        currentNode: StoryNode?,
        template: StoryTemplate,
        progress: StoryProgress,
        userChoice: String?
    ): String {
        val chapterNumber = currentNode?.chapterNumber ?: progress.currentChapter
        val previousContent = currentNode?.content ?: ""

        return """
            请继续创作第${chapterNumber}章的内容：

            故事模板：${template.name}
            当前进度：第${progress.currentChapter}章
            故事类型：${template.genre.displayName}

            ${if (previousContent.isNotEmpty()) "上一章结尾：$previousContent" else ""}

            ${userChoice?.let { "玩家选择：$it" } ?: ""}

            请生成一个完整的章节内容，要求：
            1. 保持故事的连贯性和紧张感
            2. 如果这是分支节点（每8-10章），请提供3-4个合理的选择
            3. 字数控制在300-500字之间
            4. 使用生动的描写和对话
            5. 为下一章埋下伏笔

            输出格式：
            # 第${chapterNumber}章：[章节标题]

            [章节正文内容]

            ${if (shouldIncludeChoices(chapterNumber)) """
            ## 你的选择：
            1. [选择1描述]
            2. [选择2描述]
            3. [选择3描述]
            ${if (template.difficulty != DifficultyLevel.NOVICE) "4. [选择4描述]" else ""}
            """ else ""}
        """.trimIndent()
    }

    /**
     * 构建选择生成提示词
     */
    private fun buildChoicesPrompt(currentNode: StoryNode, template: StoryTemplate, progress: StoryProgress): String {
        return """
            基于当前章节内容，为玩家提供3-4个合理的故事发展方向：

            当前章节：${currentNode.title}
            故事类型：${template.genre.displayName}
            难度等级：${template.difficulty.displayName}

            章节内容：${currentNode.content}

            请生成3-4个不同的选择，每个选择应该：
            1. 代表不同的故事发展方向
            2. 符合当前情节的逻辑性
            3. 体现${template.difficulty.displayName}难度的挑战性
            4. 为玩家提供有意义的决定

            输出格式：
            1. [选择1的详细描述]
            2. [选择2的详细描述]
            3. [选择3的详细描述]
            ${if (template.difficulty != DifficultyLevel.NOVICE) "4. [选择4的详细描述]" else ""}
        """.trimIndent()
    }

    /**
     * 调用OpenAI API
     */
    private fun callOpenAIApi(prompt: String): okhttp3.Response {
        val requestBody = """
            {
                "model": "gpt-4o-mini",
                "messages": [
                    {
                        "role": "system",
                        "content": "你是一个专业的故事创作助手，擅长创作各种类型的小说和互动故事。"
                    },
                    {
                        "role": "user",
                        "content": "$prompt"
                    }
                ],
                "temperature": 0.7,
                "max_tokens": 2000
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(requestBody)
            .header("Authorization", "Bearer YOUR_API_KEY") // 用户需要替换为自己的API Key
            .header("Content-Type", "application/json")
            .build()

        return client.newCall(request).execute()
    }

    /**
     * 从响应中提取内容
     */
    private fun extractContentFromResponse(responseBody: String?): String {
        if (responseBody == null) return ""

        return try {
            val jsonStart = responseBody.indexOf("\"content\":\"")
            if (jsonStart != -1) {
                val contentStart = jsonStart + "\"content\":\"".length
                val contentEnd = responseBody.indexOf("\"", contentStart)
                if (contentEnd != -1) {
                    responseBody.substring(contentStart, contentEnd)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                } else ""
            } else {
                responseBody
            }
        } catch (e: Exception) {
            responseBody
        }
    }

    /**
     * 解析大纲
     */
    private fun parseOutline(content: String, template: StoryTemplate): StoryOutline {
        val lines = content.lines().filter { it.trim().isNotEmpty() }

        var title = ""
        var summary = ""
        val characters = mutableListOf<Character>()
        val plotStructure = mutableListOf<PlotPoint>()
        val themes = mutableListOf<String>()

        var currentSection = ""
        for (line in lines) {
            when {
                line.startsWith("# ") && title.isEmpty() -> title = line.removePrefix("# ").trim()
                line.startsWith("## ") -> currentSection = line.removePrefix("## ").trim()
                line.startsWith("- ") && currentSection.contains("主题") -> {
                    themes.add(line.removePrefix("- ").trim())
                }
                line.startsWith("第") && line.contains("章") && currentSection.contains("情节") -> {
                    val parts = line.split("：")
                    if (parts.size >= 2) {
                        val chapterTitle = parts[1].trim()
                        val type = when {
                            line.contains("开端") -> PlotType.EXPOSITION
                            line.contains("发展") -> PlotType.RISING_ACTION
                            line.contains("高潮") -> PlotType.CLIMAX
                            line.contains("回落") -> PlotType.FALLING_ACTION
                            line.contains("结局") -> PlotType.RESOLUTION
                            else -> PlotType.EXPOSITION
                        }
                        plotStructure.add(PlotPoint(chapter = plotStructure.size + 1, title = chapterTitle, type = type))
                    }
                }
            }
        }

        return StoryOutline(
            title = title.ifEmpty { template.name },
            summary = summary,
            characters = characters,
            plotStructure = plotStructure,
            themes = themes
        )
    }

    /**
     * 解析章节
     */
    private fun parseChapter(content: String, currentNode: StoryNode?): StoryNode {
        val lines = content.lines().filter { it.trim().isNotEmpty() }

        var chapterNumber = currentNode?.chapterNumber ?: 1
        var title = ""
        var chapterContent = ""

        for (line in lines) {
            if (line.startsWith("第") && line.contains("章")) {
                title = line.substringAfter("第").substringBefore("章").trim()
                break
            }
        }

        if (title.isEmpty()) {
            title = "第${chapterNumber}章"
        }

        chapterContent = lines.joinToString("\n") { it.trim() }
            .replace(Regex("^#+\\s*"), "")
            .trim()

        return StoryNode(
            id = System.currentTimeMillis().toString(),
            chapterNumber = chapterNumber,
            title = title,
            content = chapterContent,
            isBranchPoint = shouldIncludeChoices(chapterNumber)
        )
    }

    /**
     * 解析选择项
     */
    private fun parseChoices(content: String): List<Choice> {
        val choices = mutableListOf<Choice>()
        val lines = content.lines().filter { it.trim().isNotEmpty() }

        for (line in lines) {
            if (line.matches(Regex("^\\d+\\.\\s.*"))) {
                val choiceText = line.substringAfter(".").trim()
                if (choiceText.isNotEmpty()) {
                    choices.add(
                        Choice(
                            id = System.currentTimeMillis().toString() + choices.size,
                            text = choiceText,
                            nextNodeId = ""
                        )
                    )
                }
            }
        }

        return choices.take(4) // 最多4个选择
    }

    /**
     * 判断是否需要包含选择
     */
    private fun shouldIncludeChoices(chapterNumber: Int): Boolean {
        return chapterNumber % 8 == 0 || chapterNumber % 10 == 0
    }
}

/**
 * 自定义故事设置数据类
 */
data class CustomStorySettings(
    val characterDescription: String = "",
    val backgroundDescription: String = "",
    val specialRequirements: String = ""
)