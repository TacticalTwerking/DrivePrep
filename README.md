# DrivePrep - 驾考模拟练习

基于 Android Jetpack Compose 的驾考理论考试练习应用,覆盖科目一与科目四双题库。题目与练习记录完全离线可用;图片题可通过设置中的「离线图片包」一键缓存(约 35 MB),下载后做题无需联网。

## 技术栈

- **架构**: MVVM + Clean Architecture
- **UI**: Jetpack Compose + Material 3
- **依赖注入**: Hilt
- **数据库**: Room
- **异步处理**: Kotlin Coroutines + Flow
- **图片加载**: Coil
- **序列化**: Kotlin Serialization(题库 JSON 解析与进度持久化)

## 项目结构

```
app/src/main/kotlin/com/example/driveprep/
├── data/                  # 数据层
│   ├── local/             # Room 数据库
│   │   ├── dao/           #   数据访问接口
│   │   └── entity/        #   表实体
│   ├── repository/        # Repository 实现
│   ├── mapper/            # 数据映射
│   └── preferences/       # 用户设置(SharedPreferences)
├── domain/                # 业务逻辑层
│   ├── model/             # 领域模型
│   └── repository/        # Repository 接口
├── presentation/          # UI 层
│   ├── exam/              # 考试/练习界面(含 components 组件)
│   ├── home/              # 首页
│   ├── wrongbook/         # 错题本
│   ├── statistics/        # 练习报告
│   ├── settings/          # 设置
│   ├── navigation/        # 导航
│   └── theme/             # 主题配置
└── di/                    # Hilt 依赖注入

app/src/test/              # 单元测试(37 例)
```

## 功能特性

- ✅ 科目一 / 科目四双题库(共 4300+ 题,离线可用)
- ✅ 模拟考试(科一 100 题·45 分钟 / 科四 50 题·30 分钟),倒计时自动交卷
- ✅ 随机练习,题量与考试标准一致
- ✅ 图片题加载,多选题作答顺序不影响判分
- ✅ 离线图片包:设置中一键下载全部题目图片到应用私有缓存(约 35 MB),断点续传、可清除,下载后做题完全离线
- ✅ 错题本:按错误次数排序、专项练习、标记掌握
- ✅ 练习报告:练习次数、平均分、通过率、高频错题、历史记录
- ✅ 考试进度自动保存与恢复(模拟/练习按科目+模式独立,错题复习独立),支持不保存退出与一键重考
- ✅ 夜间模式三态(跟随系统/白天/夜间),状态栏联动
- ✅ 题目字号三档可调(大/中/小)
- ✅ 答题反馈设置:立即显示对错、答题技巧、答案解析

## 应用预览

| 首页 | 答题卡 | 成绩页 | 设置 |
|---|---|---|---|
| ![首页](docs/screenshots/home.png) | ![答题卡](docs/screenshots/answer_sheet.png) | ![成绩页](docs/screenshots/result.png) | ![设置](docs/screenshots/settings.png) |

| 随机练习(夜间模式) | 错题本 | 练习报告 |
|---|---|---|
| ![随机练习](docs/screenshots/practice_dark.png) | ![错题本](docs/screenshots/wrong_book.png) | ![练习报告](docs/screenshots/statistics.png) |

## 下载安装

从 [GitHub Releases](https://github.com/TacticalTwerking/DrivePrep/releases/latest) 下载最新 APK:

| 版本 | 下载 | 说明 |
|---|---|---|
| v1.1 | [DrivePrep-v1.1.apk](https://github.com/TacticalTwerking/DrivePrep/releases/download/v1.1/DrivePrep-v1.1.apk) | 新增离线图片包:一键缓存全部题目图片(约 35 MB),下载后做题完全离线 |
| v1.0 | [DrivePrep-v1.0.apk](https://github.com/TacticalTwerking/DrivePrep/releases/download/v1.0/DrivePrep-v1.0.apk) | 科目一/科目四双题库、模拟考试、错题本、练习报告分科目 |

安装时如提示"未知来源应用",在系统弹窗中允许安装即可。

## 开始使用

1. 克隆项目(题库 `q.json` 已包含在仓库中)
2. 使用 Android Studio 打开项目
3. 运行应用(或 `./gradlew assembleDebug`)

## 数据模型

题库数据格式参考：https://github.com/doupoa/DrivingTestSubjectOne

## License

本项目基于 [MIT](LICENSE) 协议开源。
