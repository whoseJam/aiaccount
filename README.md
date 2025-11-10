# AI记账 Android应用

一个基于AI的智能记账Android应用，支持文字和语音输入，自动分析支出类目和金额。

## 功能特性

### 1. AI对话记账
- 📝 **文字输入**：直接输入记账信息，如"今天午饭花了35元"
- 🎤 **语音输入**：支持语音录音，自动转换为文字并分析
- 🤖 **智能分析**：使用AI自动识别支出类目和金额
- 💾 **本地存储**：所有数据存储在本地数据库，保护隐私

### 2. 数据统计分析
- 📊 **饼图展示**：直观显示月度和年度支出占比
- 📈 **类目排名**：展示各类目支出排名
- 📋 **支出明细**：查看所有记账记录详情
- 💰 **总额统计**：实时显示月度和年度总支出

## 支出类目

应用支持以下记账类目：
- 🍔 餐饮
- 🚗 交通
- 📚 教育
- 🎮 娱乐
- ⚽ 运动
- ✈️ 旅行
- 🏠 住房
- 💡 生活缴费
- 📦 其他

## 技术架构

### 核心技术
- **语言**：Kotlin
- **架构**：MVVM (Model-View-ViewModel)
- **数据库**：Room Database
- **网络请求**：Retrofit + OkHttp
- **图表库**：MPAndroidChart
- **UI组件**：Material Design Components

### AI服务
- **语音转文字**：OpenAI Whisper API
- **文本分析**：OpenAI GPT-3.5-turbo API

## 项目结构

```
app/
├── src/main/
│   ├── java/com/aiaccounting/app/
│   │   ├── MainActivity.kt                 # 主Activity
│   │   ├── adapter/                        # RecyclerView适配器
│   │   │   ├── ChatAdapter.kt             # 聊天消息适配器
│   │   │   ├── ExpenseAdapter.kt          # 支出明细适配器
│   │   │   └── CategoryStatAdapter.kt     # 类目统计适配器
│   │   ├── api/                           # API相关
│   │   │   ├── AIService.kt               # AI服务接口
│   │   │   ├── RetrofitClient.kt          # Retrofit客户端
│   │   │   └── AIHelper.kt                # AI助手类
│   │   ├── data/                          # 数据层
│   │   │   ├── ExpenseEntity.kt           # 支出实体
│   │   │   ├── ExpenseDao.kt              # 数据访问对象
│   │   │   ├── AppDatabase.kt             # 数据库
│   │   │   └── ExpenseRepository.kt       # 数据仓库
│   │   ├── ui/                            # UI层
│   │   │   ├── ChatFragment.kt            # 记账页面
│   │   │   └── StatisticsFragment.kt      # 统计页面
│   │   └── viewmodel/                     # ViewModel层
│   │       └── ExpenseViewModel.kt        # 支出ViewModel
│   └── res/                               # 资源文件
│       ├── layout/                        # 布局文件
│       ├── values/                        # 值资源
│       └── menu/                          # 菜单资源
└── build.gradle                           # 构建配置
```

## 配置说明

### 1. OpenAI API Key配置

在使用前，需要配置OpenAI API Key：

打开文件：`app/src/main/java/com/aiaccounting/app/api/AIHelper.kt`

找到以下代码并替换为您的API Key：

```kotlin
private val apiKey = "YOUR_OPENAI_API_KEY"
```

### 2. 权限说明

应用需要以下权限：
- `INTERNET`：网络访问，用于调用AI服务
- `RECORD_AUDIO`：录音权限，用于语音输入
- `ACCESS_NETWORK_STATE`：网络状态检查

## 构建和运行

### 环境要求
- Android Studio Arctic Fox或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.1.0

### 构建步骤

1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 配置OpenAI API Key（见上文）
4. 同步Gradle依赖
5. 连接Android设备或启动模拟器
6. 点击运行按钮

### Gradle命令

```bash
# 清理项目
./gradlew clean

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 使用说明

### 记账操作

1. **文字记账**
   - 在输入框中输入记账信息
   - 例如："今天午饭花了35元"、"打车回家20块"
   - 点击发送按钮
   - AI会自动分析并保存记录

2. **语音记账**
   - 点击麦克风按钮开始录音
   - 说出记账信息
   - 再次点击停止录音
   - 系统会自动识别并分析

### 查看统计

1. 点击底部"统计"标签
2. 查看月度和年度支出饼图
3. 浏览类目排名
4. 查看详细支出明细

## 注意事项

1. **API费用**：使用OpenAI API会产生费用，请注意控制使用量
2. **网络连接**：AI功能需要网络连接才能使用
3. **隐私保护**：所有记账数据仅存储在本地，不会上传到服务器
4. **语音质量**：录音时请保持环境安静，以提高识别准确率

## 未来计划

- [ ] 支持预算设置和提醒
- [ ] 添加收入记录功能
- [ ] 支持多账户管理
- [ ] 数据导出功能（Excel/CSV）
- [ ] 云端备份和同步
- [ ] 支持更多AI模型选择
- [ ] 添加数据可视化图表类型

## 开源协议

MIT License

## 联系方式

如有问题或建议，欢迎提Issue或Pull Request。

---

**注意**：本应用仅供学习和个人使用，请勿用于商业用途。
