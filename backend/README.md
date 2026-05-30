# Python 后端 - AI 姿态模仿闯关

## 项目简介

轻量级 Python 后端服务，提供 REST API 接口，接收前端视频流/图片，转发 AI 推理请求，存储关卡数据、分数记录。

## 技术栈

- **框架**：Flask / FastAPI
- **数据库**：SQLite / MySQL
- **AI 接口**：MediaPipe Pose

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户管理 | 注册、登录、用户信息 |
| 关卡管理 | 关卡列表、详情、动作类型 |
| 姿态识别 | 图片识别、视频识别、WebSocket实时识别 |
| 游戏记录 | 闯关记录、排行榜 |
| 贴纸管理 | 贴纸列表、奖励发放 |

## 项目结构

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py           # FastAPI 入口
│   ├── routers/          # 路由模块
│   │   ├── user.py
│   │   ├── level.py
│   │   ├── pose.py
│   │   └── game.py
│   ├── models/           # 数据模型
│   ├── services/        # 业务逻辑
│   └── utils/           # 工具函数
├── requirements.txt
└── README.md
```

## 快速开始

```bash
cd backend

# 安装依赖
pip install -r requirements.txt

# 启动服务
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 5000
```

## 接口文档

- **Base URL**：`http://localhost:5000/api`
- 详细接口文档：根目录 `../接口文档.md`

## 主要依赖

```
fastapi
uvicorn
sqlalchemy
pymysql
python-multipart
websockets
```

## 开发人员

- 后端开发：待分配

## 注意事项

1. 确保 Python 版本 ≥ 3.8
2. 数据库初始化会自动创建表结构
3. 需要启动 AI 模型服务（默认端口 8000）