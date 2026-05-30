# Android 前端 - AI 姿态模仿闯关

## 项目简介

移动端拍照/实时摄像，AI识别人体肢体姿态，给出标准示范动作，用户模仿后系统自动打分、判定是否闯关成功。

## 技术栈

- **开发工具**：Android Studio
- **语言**：Kotlin / Java
- **最低 SDK**：24 (Android 7.0)
- **目标 SDK**：34 (Android 14)

## 主要依赖

- CameraX（相机功能）
- Retrofit（网络请求）
- OkHttp（WebSocket实时通信）
- Glide（图片加载）
- Gson（JSON 解析）

## 功能模块

| 模块 | 说明 |
|------|------|
| 登录注册 | 用户账号管理 |
| 关卡列表 | 展示所有关卡及解锁状态 |
| 相机预览 | 实时摄像头画面 |
| 姿态识别 | 拍照/录像提交AI识别 |
| 结果展示 | 得分、贴纸奖励展示 |
| 个人中心 | 贴纸墙、历史记录 |

## 项目结构

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/posegame/app/
│   │   │   ├── ui/          # 界面
│   │   │   ├── model/       # 数据模型
│   │   │   ├── api/         # 网络接口
│   │   │   ├── util/        # 工具类
│   │   │   └── PoseApp.java # 应用入口
│   │   ├── res/             # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
└── settings.gradle
```

## 接口对接

- **Base URL**：`http://localhost:5000/api`
- 详细接口文档：根目录 `../接口文档.md`

## 开发人员

- 前端开发：待分配

## 开始开发

```bash
# 克隆仓库后
cd android-app
./gradlew assembleDebug  # 编译Debug版本
```

## 注意事项

1. 确保 Android Studio 已安装并配置好 Android SDK
2. 相机功能需要真机测试，模拟器可能不支持
3. 网络请求需要添加网络权限 (INTERNET)