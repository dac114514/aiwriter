# AI Story Weaver - 沉浸式AI互动故事小说App

基于现有的AI Chat Android应用开发的沉浸式AI互动故事创作应用。

## 🎯 核心功能

### 1. Setup阶段（故事配置）
- **选择故事类型**：奇幻冒险、科幻、悬疑推理、古风、现代言情、无限流
- **自定义设定**：角色设定、故事背景、特殊要求
- **难度等级**：新手、进阶、专家三个级别
- **大纲生成**：自动生成详细的故事大纲和人物设定

### 2. 章节推进系统
- **线性推进**：按章节顺序阅读故事
- **分支选择**：在关键节点（每8-10章）提供3-4个选择
- **多结局支持**：根据玩家选择产生不同的故事走向
- **进度追踪**：实时显示阅读进度和完成度

## 📱 界面说明

### 主界面导航
```
AI助手 (默认)
├── 设置 (API配置)
├── 历史对话
└── AI故事创作 (新入口)
```

### 故事创作流程
1. **选择模板** → 2. **自定义设定** → 3. **大纲预览** → 4. **开始创作**

### 故事阅读界面
- 顶部：章节进度指示器
- 中部：章节内容显示
- 底部：操作按钮（重新开始/继续）
- 分支点：显示可选择的分支选项

## 🔧 技术架构

### 数据模型
- `StoryTemplate` - 故事模板
- `StoryNode` - 故事章节节点
- `Choice` - 选择项
- `StoryProgress` - 故事进度
- `StoryOutline` - 故事大纲

### Repository模式
- `StoryRepository` - 数据持久化
- `StoryGenerator` - AI API调用
- 支持自定义服务商和模型选择

### MVVM架构
- `StoryViewModel` - 状态管理
- 响应式UI更新
- 错误处理和加载状态

## 🚀 快速开始

### 1. 配置API
1. 进入"设置"页面
2. 配置OpenAI API Key
3. 选择模型（推荐gpt-4o-mini）

### 2. 创建故事
1. 点击"AI故事创作"
2. 选择故事类型和难度
3. 填写角色设定和背景
4. 生成并预览大纲
5. 开始创作

### 3. 阅读故事
1. 按章节顺序阅读
2. 在分支点做出选择
3. 体验不同的故事走向
4. 随时重新开始或返回设置

## ⚙️ 配置说明

### API配置
```kotlin
// 在StoryGenerator中配置
val request = Request.Builder()
    .url("https://api.openai.com/v1/chat/completions")
    .header("Authorization", "Bearer YOUR_API_KEY")
    // ...
```

### 故事参数
- **章节长度**：300-500字
- **分支频率**：每8-10章一个分支点
- **选择数量**：3-4个选择（根据难度调整）
- **故事长度**：25-100章

## 🛠️ 开发说明

### 项目结构
```
app/src/main/java/com/example/aichat/
├── data/
│   ├── StoryData.kt          # 数据模型
│   ├── StoryRepository.kt     # 数据仓库
│   └── StoryGenerator.kt      # AI生成器
├── viewmodel/
│   └── StoryViewModel.kt      # 视图模型
└── ui/screens/
    ├── StorySetupScreen.kt    # 故事设置
    └── StoryScreen.kt         # 故事阅读
```

### 依赖关系
- Jetpack Compose UI
- Navigation组件
- DataStore持久化
- OkHttp网络请求
- Kotlinx Serialization

## 🎨 用户体验优化

### Material Design 3
- 遵循Google Material Design 3规范
- 动态色彩主题
- 流畅的动画过渡
- 响应式布局设计

### 交互设计
- 直观的导航流程
- 清晰的视觉层次
- 友好的错误提示
- 流畅的加载状态

## 📝 使用示例

### 创建奇幻故事
1. 选择"奇幻冒险之旅"
2. 设置难度为"新手"
3. 描述主角：勇敢的年轻法师，拥有神秘的力量
4. 设定背景：魔法世界正在面临黑暗势力的威胁
5. 生成大纲并开始创作

### 体验分支选择
- 在第8章时会出现第一个分支点
- 选择不同的行动路线
- 体验不同的情节发展
- 最终到达不同的故事结局

## 🔄 后续功能规划

### 即将添加
- [ ] Qwen图像API集成
- [ ] 故事保存和加载
- [ ] 多人协作创作
- [ ] 社交分享功能
- [ ] 更多故事类型

### 高级功能
- [ ] 实时多人互动
- [ ] 语音朗读支持
- [ ] 离线缓存
- [ ] 云同步
- [ ] 创作者工具包

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 📄 许可证

本项目基于现有AI Chat应用开发，遵循相应的开源协议。

---

**Enjoy your AI story creation journey!** 🌟