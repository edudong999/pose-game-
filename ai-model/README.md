# AI 模型 - AI 姿态模仿闯关

## 项目简介

人体姿态估计模型，提取关节点坐标，对比标准姿态计算匹配度，输出评分。

## 技术选型

- **推荐模型**：Google MediaPipe Pose（轻量级，33个关节点）
- **备选模型**：YOLO Pose

## 功能特性

| 功能 | 说明 |
|------|------|
| 实时姿态检测 | 支持实时视频流处理 |
| 关节点提取 | 33个人体关键点坐标 |
| 相似度计算 | 用户姿态 vs 标准姿态 |
| 批量处理 | 支持图片/视频批量识别 |

## 项目结构

```
ai-model/
├── pose_estimation/
│   ├── __init__.py
│   ├── mediapipe_pose.py   # MediaPipe 实现
│   ├── yolo_pose.py       # YOLO 实现
│   ├── similarity.py      # 相似度计算
│   └── keypoints.py       # 关节点定义
├── api/
│   ├── __init__.py
│   └── pose_api.py        # REST API 服务
├── models/
│   └── pose_model.py      # 模型封装
├── requirements.txt
└── README.md
```

## 快速开始

```bash
cd ai-model

# 安装依赖
pip install -r requirements.txt

# 启动 AI 服务
python -m uvicorn api.pose_api:app --host 0.0.0.0 --port 8000
```

## API 接口

### 姿态识别
```
POST /recognize
Content-Type: multipart/form-data

参数:
  - image: 图片文件

返回:
  - keypoints: 关节点坐标列表
  - score: 相似度得分 (0-100)
```

### 视频识别
```
POST /recognize_video
Content-Type: multipart/form-data

参数:
  - video: 视频文件

返回:
  - best_frame: 最佳帧图片
  - score: 最终得分
```

## 关节点列表 (33个)

| 部位 | 关节点 |
|------|--------|
| 头部 | nose, eyes, ears, mouth |
| 肩膀 | left_shoulder, right_shoulder |
| 手臂 | elbow, wrist, thumb, index, pinky |
| 躯干 | hip |
| 腿部 | knee, ankle, heel, foot_index |

详细定义见 `pose_estimation/keypoints.py`

## 相似度计算算法

```
1. 提取用户姿态关键点坐标 (x, y, z)
2. 提取标准姿态关键点坐标
3. 计算欧氏距离
4. 加权求和（头部权重高，手臂权重低）
5. 归一化到 0-100 分
```

## 主要依赖

```
mediapipe
opencv-python
numpy
torch
ultralytics  # YOLO
fastapi
uvicorn
```

## 开发人员

- AI 模型开发：待分配

## 模型对比

| 模型 | 参数量 | 精度 | 速度 | 推荐场景 |
|------|--------|------|------|----------|
| MediaPipe Pose | 3.2MB | 中 | 快 | 移动端、实时 |
| YOLO Pose | 30MB+ | 高 | 中 | 服务器、离线 |

## 注意事项

1. MediaPipe 需要较高的 CPU 性能
2. 视频文件建议不超过 5 秒
3. 建议使用 GPU 加速推理