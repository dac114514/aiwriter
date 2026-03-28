# AI Story Weaver 系统架构设计

## 1. 整体架构概览

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[UI Components<br/>Jetpack Compose]
        VM[ViewModels<br/>MVVM Pattern]
        NAV[Navigation<br/>Compose Navigation]
    end

    subgraph "Domain Layer"
        USECASE[Use Cases<br/>Business Logic]
        REPO[Repositories<br/>Data Access]
        ENTITY[Entities<br/>Data Models]
    end

    subgraph "Data Layer"
        LOCAL[(Local Database<br/>Room)]
        REMOTE[Remote APIs<br/>Retrofit]
        CACHE[Cache Manager<br/>Memory/Disk]
        STORAGE[Storage Manager<br/>DataStore]
    end

    subgraph "External Services"
        AI[AI Services<br/>OpenAI/DeepSeek]
        ANALYTICS[Analytics<br/>Firebase]
        CRASHLYTICS[Crashlytics<br/>Error Tracking]
    end

    %% Presentation Layer Connections
    UI --> VM
    VM --> NAV
    UI --> NAV

    %% Domain Layer Connections
    VM --> USECASE
    USECASE --> REPO
    REPO --> ENTITY

    %% Data Layer Connections
    REPO --> LOCAL
    REPO --> REMOTE
    REPO --> CACHE
    REPO --> STORAGE

    REMOTE --> AI
    UI --> ANALYTICS
    UI --> CRASHLYTICS

    style UI fill:#e3f2fd,stroke:#1976d2
    style VM fill:#f3e5f5,stroke:#7b1fa2
    style NAV fill:#e8f5e8,stroke:#388e3c
    style USECASE fill:#fff3e0,stroke:#f57c00
    style REPO fill:#fce4ec,stroke:#c2185b
    style ENTITY fill:#e0f2f1,stroke:#00695c
    style LOCAL fill:#f1f8e9,stroke:#689f38
    style REMOTE fill:#ede7f6,stroke:#5e35b1
    style CACHE fill:#fffde7,stroke:#fbc02d
    style STORAGE fill:#e1f5fe,stroke:#0288d1
    style AI fill:#ffebee,stroke:#d32f2f
    style ANALYTICS fill:#f3e5f5,stroke:#7b1fa2
    style CRASHLYTICS fill:#fce4ec,stroke:#c2185b
```

## 2. 核心模块详细设计

### 2.1 故事引擎模块

```mermaid
classDiagram
    class StoryEngine {
        +String storyId
        +List~StoryNode~ nodes
        +Map~String, Any~ variables
        +StoryProgress progress
        +startStory()
        +makeChoice(choice: Choice)
        +getCurrentNode(): StoryNode
        +checkConditions(nodeId: String): Boolean
        +saveProgress()
        +loadProgress()
    }

    class StoryNode {
        +String id
        +String title
        +String content
        +List~Choice~ choices
        +Map~String, Any~ conditions
        +List~String~ nextNodes
    }

    class Choice {
        +String text
        +String targetNodeId
        +Map~String, Any~ requirements
        +boolean isAvailable()
    }

    class StoryTemplate {
        +String id
        +String name
        +StoryGenre genre
        +String initialPrompt
        +String systemPrompt
        +List~BranchingRule~ rules
        +createStory(): StoryEngine
    }

    class BranchingRule {
        +String condition
        +String action
        +Map~String, Any~ parameters
    }

    class StoryProgress {
        +String storyId
        +String currentNodeId
        +Set~String~ completedNodes
        +Map~String, Any~ variables
        +List~String~ achievements
        +updateProgress()
        +getCompletionPercentage(): float
    }

    StoryEngine "1" -- "*" StoryNode : contains
    StoryEngine "1" -- "1" StoryProgress : tracks
    StoryNode "1" -- "*" Choice : has
    StoryTemplate "1" -- "*" BranchingRule : defines
    StoryEngine "1" -- "1" StoryTemplate : based on
```

### 2.2 数据流设计

```mermaid
sequenceDiagram
    participant User as 用户
    participant UI as UI组件
    participant VM as ViewModel
    participant USECASE as UseCase
    participant REPO as Repository
    participant DB as 本地数据库
    participant API as AI API
    participant CACHE as 缓存

    %% 故事开始流程
    User->>UI: 选择故事模板
    UI->>VM: onTemplateSelected(template)
    VM->>USECASE: startNewStory(template)
    USECASE->>REPO: createStoryEngine(template)
    REPO->>DB: saveStoryTemplate(template)
    REPO-->>USECASE: return storyEngine
    USECASE-->>VM: return storyEngine
    VM-->>UI: updateUI(storyEngine)
    UI-->>User: 显示故事界面

    %% 用户选择分支
    User->>UI: 点击选择分支
    UI->>VM: onChoiceSelected(choice)
    VM->>USECASE: makeChoice(choice)
    USECASE->>REPO: validateChoice(choice)
    REPO->>DB: getStoryProgress()
    alt 条件满足
        REPO->>API: generateNextContent(prompt)
        API-->>REPO: return generatedContent
        REPO->>CACHE: cacheContent(content)
        REPO->>DB: updateStoryProgress(progress)
        REPO-->>USECASE: return updatedStory
        USECASE-->>VM: return updatedStory
        VM-->>UI: updateUI(updatedStory)
        UI-->>User: 显示新内容
    else 条件不满足
        REPO-->>USECASE: throw ConditionNotMetException
        USECASE-->>VM: throw error
        VM-->>UI: showErrorMessage()
        UI-->>User: 显示错误信息
    end
```

### 2.3 网络层设计

```mermaid
graph LR
    subgraph "Network Module"
        RETROFIT[Retrofit Client]
        INTERCEPTOR[Http Interceptor]
        CONVERTER[Json Converter]
        ADAPTER[Coroutine Adapter]
    end

    subgraph "API Services"
        CHATAPI[ChatService<br/>对话API]
        STORYAPI[StoryService<br/>故事API]
        AUTHAPI[AuthService<br/>认证API]
    end

    subgraph "Response Handling"
        RESPONSE[ApiResponse<T>]
        ERRORHANDLER[ErrorHandler]
        RETRYLOGIC[RetryLogic]
    end

    RETROFIT --> INTERCEPTOR
    RETROFIT --> CONVERTER
    RETROFIT --> ADAPTER

    INTERCEPTOR --> ERRORHANDLER
    INTERCEPTOR --> RETRYLOGIC

    CHATAPI --> RESPONSE
    STORYAPI --> RESPONSE
    AUTHAPI --> RESPONSE

    ERRORHANDLER --> RETRYLOGIC
    RETRYLOGIC --> RETROFIT

    style RETROFIT fill:#e3f2fd,stroke:#1976d2
    style INTERCEPTOR fill:#f3e5f5,stroke:#7b1fa2
    style CONVERTER fill:#e8f5e8,stroke:#388e3c
    style ADAPTER fill:#fff3e0,stroke:#f57c00
    style CHATAPI fill:#fce4ec,stroke:#c2185b
    style STORYAPI fill:#e0f2f1,stroke:#00695c
    style AUTHAPI fill:#f1f8e9,stroke:#689f38
    style RESPONSE fill:#ede7f6,stroke:#5e35b1
    style ERRORHANDLER fill:#fffde7,stroke:#fbc02d
    style RETRYLOGIC fill:#e1f5fe,stroke:#0288d1
```

### 2.4 状态管理设计

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Loading: startStory()
    Loading --> Ready: storyLoaded
    Loading --> Error: loadFailed(error)
    
    Ready --> Reading: userStartReading()
    Reading --> Choosing: userMakeChoice()
    Choosing --> Processing: choiceSubmitted()
    Processing --> Reading: contentGenerated()
    Processing --> Error: generationFailed(error)
    
    Reading --> Paused: userPause()
    Paused --> Reading: userResume()
    
    Choosing --> Ready: backToMenu()
    Reading --> Ready: restartStory()
    
    Error --> Retrying: retryRequested()
    Retrying --> Loading: retryStarted()
    Retrying --> Error: retryFailed()
    
    state Ready {
        [*] --> Menu
        Menu --> StorySetup: selectTemplate()
        StorySetup --> StoryIntro: templateSelected()
        StoryIntro --> MainStory: introComplete()
    }
    
    state Reading {
        [*] --> Normal
        Normal --> Highlighted: textSelected()
        Highlighted --> Normal: selectionCleared()
    }
    
    state Choosing {
        [*] --> OptionsShown
        OptionsShown --> OptionSelected: choiceMade()
        OptionSelected --> ConfirmationShown: confirmChoice()
        ConfirmationShown --> OptionsShown: cancelChoice()
    }
```

## 3. 关键技术决策

### 3.1 架构模式选择
- **MVVM**: 清晰的关注点分离，便于测试和维护
- **Repository模式**: 抽象数据源，支持多数据源切换
- **UseCase模式**: 封装业务逻辑，提高代码复用性

### 3.2 技术栈选择
- **Jetpack Compose**: 现代化的声明式UI框架
- **Room**: 类型安全的SQLite数据库
- **Retrofit**: 灵活的HTTP客户端
- **Hilt**: 依赖注入框架
- **Coroutines**: 异步编程
- **Flow**: 响应式数据流

### 3.3 性能优化策略
- **懒加载**: 按需加载故事内容
- **缓存机制**: 内存+磁盘两级缓存
- **分页加载**: 大文本分块显示
- **预加载**: 预测用户行为提前加载
- **压缩传输**: 减少网络流量

## 4. 安全考虑

### 4.1 数据安全
- **本地加密**: 敏感数据AES加密存储
- **传输安全**: HTTPS + Certificate Pinning
- **权限控制**: 最小权限原则
- **数据清理**: 及时清理临时文件

### 4.2 内容安全
- **输入过滤**: 防止XSS攻击
- **输出过滤**: 内容安全审查
- **速率限制**: 防止API滥用
- **日志脱敏**: 保护用户隐私

## 5. 监控和运维

### 5.1 应用监控
- **崩溃监控**: Firebase Crashlytics
- **性能监控**: Android Vitals
- **用户行为**: Firebase Analytics
- **自定义指标**: 关键业务指标追踪

### 5.2 运维工具
- **CI/CD**: GitHub Actions
- **代码质量**: SonarQube
- **依赖管理**: Dependabot
- **文档生成**: Dokka

---

**架构版本**: 1.0
**创建时间**: 2026-03-28
**最后更新**: 2026-03-28
**维护人**: 架构师