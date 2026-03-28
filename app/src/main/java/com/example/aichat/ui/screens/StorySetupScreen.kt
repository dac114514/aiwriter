package com.faster.aiwriter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.aiwriter.data.*
import com.faster.aiwriter.viewmodel.StoryViewModel

import com.faster.aiwriter.data.CustomStorySettings
data class CustomStorySettings(
    val characterDescription: String = "",
    val backgroundDescription: String = "",
    val specialRequirements: String = ""
)

/**
 * 故事设置屏幕 - 用户配置故事参数并开始创作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySetupScreen(
    viewModel: StoryViewModel,
    onNavigateToStory: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var customSettings by remember { mutableStateOf(CustomStorySettings()) }
    var showOutlinePreview by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) } // 0: 选择模板, 1: 自定义设置, 2: 大纲预览, 3: 开始创作

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            0 -> "选择故事类型"
                            1 -> "自定义设定"
                            2 -> "故事大纲"
                            3 -> "开始创作"
                            else -> "AI故事创作"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            when (currentStep) {
                0 -> items(templates) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate?.id == template.id,
                        onClick = {
                            viewModel.selectTemplate(template)
                            currentStep = 1
                        }
                    )
                }

                1 -> {
                    item {
                        CustomSettingsCard(
                            settings = customSettings,
                            onSettingsChanged = { customSettings = it },
                            onNext = { currentStep = 2 }
                        )
                    }
                }

                2 -> {
                    item {
                        OutlinePreviewCard(
                            outline = null, // TODO: 从viewModel获取大纲
                            onStartStory = {
                                viewModel.startNewStory(customSettings)
                                currentStep = 3
                            },
                            onEditSettings = { currentStep = 1 },
                            isGenerating = uiState.isGeneratingOutline
                        )
                    }
                }

                3 -> {
                    item {
                        StoryCreationCard(
                            onContinueToStory = onNavigateToStory,
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }

            // 错误信息显示
            if (uiState.error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearError) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 故事模板卡片
 */
@Composable
private fun TemplateCard(
    template: StoryTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(template.genre.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                AssistChip(
                    onClick = { },
                    label = { Text(template.difficulty.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (template.difficulty) {
                            DifficultyLevel.NOVICE -> Color(0xFFE8F5E8)
                            DifficultyLevel.INTERMEDIATE -> Color(0xFFFFF3E0)
                            DifficultyLevel.EXPERT -> Color(0xFFFFEBEE)
                        }
                    )
                )
            }

            Text(
                "预计 ${template.estimatedChapters} 章",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 自定义设置卡片
 */
@Composable
private fun CustomSettingsCard(
    settings: CustomStorySettings,
    onSettingsChanged: (CustomStorySettings) -> Unit,
    onNext: () -> Unit
) {
    var characterDescription by remember(settings.characterDescription) {
        mutableStateOf(settings.characterDescription)
    }
    var backgroundDescription by remember(settings.backgroundDescription) {
        mutableStateOf(settings.backgroundDescription)
    }
    var specialRequirements by remember(settings.specialRequirements) {
        mutableStateOf(settings.specialRequirements)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "自定义故事设定",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = characterDescription,
                onValueChange = { characterDescription = it },
                label = { Text("角色设定") },
                placeholder = { Text("描述主角的性格、外貌、技能等...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = backgroundDescription,
                onValueChange = { backgroundDescription = it },
                label = { Text("故事背景") },
                placeholder = { Text("描述故事发生的世界、时代背景等...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = specialRequirements,
                onValueChange = { specialRequirements = it },
                label = { Text("特殊要求") },
                placeholder = { Text("任何特殊的创作要求或限制...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Button(
                onClick = {
                    onSettingsChanged(
                        CustomStorySettings(
                            characterDescription = characterDescription,
                            backgroundDescription = backgroundDescription,
                            specialRequirements = specialRequirements
                        )
                    )
                    onNext()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("生成大纲")
            }
        }
    }
}

/**
 * 大纲预览卡片
 */
@Composable
private fun OutlinePreviewCard(
    outline: StoryOutline?,
    onStartStory: () -> Unit,
    onEditSettings: () -> Unit,
    isGenerating: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "故事大纲预览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("正在生成故事大纲...")
                    }
                }
            } else if (outline != null) {
                // TODO: 显示大纲内容
                Text(
                    "大纲生成完成！点击下方按钮开始创作你的故事。",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "点击上方按钮生成故事大纲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("修改设定")
                }

                Button(
                    onClick = onStartStory,
                    enabled = !isGenerating && outline != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("开始创作")
                }
            }
        }
    }
}

/**
 * 故事创作卡片
 */
@Composable
private fun StoryCreationCard(
    onContinueToStory: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "准备开始创作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("正在初始化故事...")
                    }
                }
            } else {
                Text(
                    "你的故事已经准备就绪！点击下方按钮进入故事世界，开始你的创作之旅。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onContinueToStory,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("进入故事世界")
            }
        }
    }
}