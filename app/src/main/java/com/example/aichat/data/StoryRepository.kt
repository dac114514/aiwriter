package com.faster.aiwriter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.storyDataStore: DataStore<Preferences> by preferencesDataStore(name = "story_settings")

/**
 * 故事仓库 - 负责故事数据的持久化和操作
 */
class StoryRepository(private val context: Context) {

    companion object {
        private val KEY_STORY_PROGRESS = stringPreferencesKey("story_progress")
        private val KEY_CURRENT_TEMPLATE = stringPreferencesKey("current_template")
        private val KEY_OUTLINE_CACHE = stringPreferencesKey("outline_cache")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 保存故事进度
     */
    suspend fun saveStoryProgress(progress: StoryProgress) {
        context.storyDataStore.edit { prefs ->
            prefs[KEY_STORY_PROGRESS] = json.encodeToString(progress)
        }
    }

    /**
     * 获取故事进度
     */
    fun getStoryProgress(): Flow<StoryProgress?> {
        return context.storyDataStore.data.map { prefs ->
            try {
                val progressJson = prefs[KEY_STORY_PROGRESS]
                if (progressJson != null) {
                    json.decodeFromString<StoryProgress>(progressJson)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 保存当前模板ID
     */
    suspend fun saveCurrentTemplate(templateId: String) {
        context.storyDataStore.edit { prefs ->
            prefs[KEY_CURRENT_TEMPLATE] = templateId
        }
    }

    /**
     * 获取当前模板ID
     */
    fun getCurrentTemplate(): Flow<String?> {
        return context.storyDataStore.data.map { prefs ->
            prefs[KEY_CURRENT_TEMPLATE]
        }
    }

    /**
     * 保存大纲缓存
     */
    suspend fun saveOutlineCache(outline: StoryOutline, templateId: String) {
        context.storyDataStore.edit { prefs ->
            val cacheKey = "${templateId}_outline"
            prefs[stringPreferencesKey(cacheKey)] = json.encodeToString(outline)
        }
    }

    /**
     * 获取大纲缓存
     */
    suspend fun getOutlineCache(templateId: String): StoryOutline? {
        return try {
            val prefs = context.storyDataStore.data.first()
            val cacheKey = "${templateId}_outline"
            val outlineJson = prefs[stringPreferencesKey(cacheKey)]
            if (outlineJson != null) {
                json.decodeFromString<StoryOutline>(outlineJson)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 清除故事进度
     */
    suspend fun clearStoryProgress() {
        context.storyDataStore.edit { prefs ->
            prefs.remove(KEY_STORY_PROGRESS)
            prefs.remove(KEY_CURRENT_TEMPLATE)
        }
    }

    /**
     * 预定义的故事模板
     */
    fun getDefaultTemplates(): List<StoryTemplate> {
        return listOf(
            StoryTemplate(
                id = "fantasy_001",
                name = "奇幻冒险之旅",
                description = "踏上神秘的魔法世界，探索古老的传说",
                genre = StoryGenre.FANTASY,
                difficulty = DifficultyLevel.NOVICE,
                estimatedChapters = 60,
                systemPrompt = "你是一个奇幻世界的向导，正在讲述一个充满魔法和冒险的故事。请保持故事的神秘感和探索性，让读者能够沉浸在这个奇妙的世界中。"
            ),
            StoryTemplate(
                id = "sci_fi_001",
                name = "未来科幻世界",
                description = "在遥远的未来，人类已经掌握了星际旅行的技术",
                genre = StoryGenre.SCI_FI,
                difficulty = DifficultyLevel.INTERMEDIATE,
                estimatedChapters = 80,
                systemPrompt = "你是一个未来世界的叙述者，正在描述一个科技高度发达的近未来世界。请注重科技细节和逻辑性，同时保持故事的紧张感和探索性。"
            ),
            StoryTemplate(
                id = "mystery_001",
                name = "悬疑推理案件",
                description = "在一个封闭的空间中，解开层层谜团",
                genre = StoryGenre.MYSTERY,
                difficulty = DifficultyLevel.EXPERT,
                estimatedChapters = 45,
                systemPrompt = "你是一个专业的侦探助手，正在协助调查一起复杂的案件。请保持推理的逻辑性和悬念感，让读者跟随你的思路一步步揭开真相。"
            ),
            StoryTemplate(
                id = "ancient_001",
                name = "古风武侠传奇",
                description = "江湖恩怨，武林争霸，谱写一段英雄史诗",
                genre = StoryGenre.ANCIENT,
                difficulty = DifficultyLevel.INTERMEDIATE,
                estimatedChapters = 70,
                systemPrompt = "你是一个古典武侠小说的叙述者，正在描绘一个充满江湖义气和武功对决的世界。请使用古典文学的风格，注重人物性格的刻画和情节的起伏。"
            ),
            StoryTemplate(
                id = "romance_001",
                name = "现代爱情故事",
                description = "都市中的浪漫邂逅，情感纠葛与成长",
                genre = StoryGenre.ROMANCE,
                difficulty = DifficultyLevel.NOVICE,
                estimatedChapters = 55,
                systemPrompt = "你是一个浪漫故事的叙述者，正在描写现代都市中的爱情经历。请注重情感的细腻描写和人物内心的变化，让读者感受到爱情的甜蜜与苦涩。"
            ),
            StoryTemplate(
                id = "infinite_001",
                name = "无限流生存挑战",
                description = "在各种恐怖世界中求生，面对未知的恐惧",
                genre = StoryGenre.INFINITE,
                difficulty = DifficultyLevel.EXPERT,
                estimatedChapters = 100,
                systemPrompt = "你是一个无限流小说的叙述者，正在描述主角在各个恐怖世界中的生存挑战。请营造紧张刺激的氛围，注重心理描写和生存策略的思考。"
            )
        )
    }
}