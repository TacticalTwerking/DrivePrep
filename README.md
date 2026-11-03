# DrivePrep - 驾考科目一练习

基于 Android Jetpack Compose 的驾考科目一练习应用

## 技术栈

- **架构**: MVVM + Clean Architecture
- **UI**: Jetpack Compose + Material 3
- **依赖注入**: Hilt
- **数据库**: Room
- **异步处理**: Kotlin Coroutines + Flow
- **图片加载**: Coil
- **网络**: Retrofit + Kotlin Serialization

## 项目结构

```
app/
├── data/              # 数据层
│   ├── local/         # Room 数据库
│   ├── repository/    # Repository 实现
│   ├── mapper/        # 数据映射
│   └── preferences/   # 用户设置
├── domain/            # 业务逻辑层
│   ├── model/         # 领域模型
│   └── repository/    # Repository 接口
├── presentation/      # UI 层
│   ├── exam/          # 考试/练习界面
│   ├── home/          # 首页
│   ├── wrongbook/     # 错题本
│   ├── statistics/    # 统计
│   ├── settings/      # 设置
│   ├── navigation/    # 导航
│   └── theme/         # 主题配置
└── di/                # 依赖注入

```

## 功能特性

- ✅ 模拟考试（100题，45分钟）
- ✅ 章节练习
- ✅ 错题本
- ✅ 考试记录
- ✅ 自动保存进度
- ✅ 离线题库
- ✅ 图片缓存

## 开始使用

1. 克隆项目(题库 `q.json` 已包含在仓库中)
2. 使用 Android Studio 打开项目
3. 运行应用(或 `./gradlew assembleDebug`)

## 数据模型

题库数据格式参考：https://github.com/doupoa/DrivingTestSubjectOne

## 构建

```bash
./gradlew assembleDebug
```

## License

MIT
