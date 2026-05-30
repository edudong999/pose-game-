# AI 姿态模仿闯关 - 项目总览

本项目是一个基于 AI 姿态识别的休闲闯关游戏，分为三个模块：

## 项目结构

```
├── android-app/     # Android 前端应用
├── backend/         # Python 后端服务
├── ai-model/        # AI 姿态识别模型
└── 接口文档.md      # 详细接口文档
```

## 团队分工

| 成员 | 负责模块 | 技术栈 |
|------|----------|--------|
| 前端开发 | Android APP | Kotlin, CameraX, Retrofit |
| 后端开发 | 接口服务 | Python FastAPI, MySQL |
| AI开发 | 姿态识别 | MediaPipe Pose |

## 快速开始

### 1. 克隆项目
```bash
git clone <仓库地址>
cd project1
```

### 2. 启动后端
```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### 3. 启动AI服务
```bash
cd ai-model
pip install -r requirements.txt
python -m uvicorn api.pose_api:app --port 8000
```

### 4. 运行AndroidAPP
使用 Android Studio 打开 `android-app` 目录，连接真机运行

## 接口文档

详细接口说明请查看 `接口文档.md`