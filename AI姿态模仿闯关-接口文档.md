# AI 姿态模仿闯关 - 接口文档

## 一、项目概述

### 1.1 项目简介
- **项目名称**：AI 姿态模仿闯关
- **项目类型**：Android APP + Python 后台 + AI 姿态识别
- **核心功能**：用户模仿标准动作拍照/录像，AI识别姿态并打分，闯关解锁趣味特效

### 1.2 技术架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Android   │────▶│  Python     │────▶│  AI 模型    │
│  前端APP    │◀────│  后端接口    │◀────│  MediaPipe  │
└─────────────┘     └─────────────┘     └─────────────┘
```

| 角色 | 技术栈 | 职责 |
|------|--------|------|
| 移动端前端 | Android | 界面、相机预览、动作提示、分数展示、关卡切换 |
| 后端 | Python Flask/FastAPI | 接口、视频流转发、AI调用、数据存储 |
| AI模型 | MediaPipe Pose | 人体姿态识别、关节点提取、相似度计算 |

### 1.3 核心功能清单
| 功能 | 说明 |
|------|------|
| F1 | 实时摄像头识别人体关节姿态（头、肩、手、腰、腿） |
| F2 | 内置10+趣味动作（比心、举手、深蹲、歪头杀等） |
| F3 | 姿态相似度打分（0-100分），达标闯关成功 |
| F4 | 关卡模式，连续闯关解锁趣味特效贴纸 |

---

## 二、数据库设计

### 2.1 关卡表 level
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| name | VARCHAR(50) | 关卡名称 |
| description | VARCHAR(200) | 关卡描述 |
| pose_type | VARCHAR(50) | 动作类型（比心/举手/深蹲等） |
| target_pose | JSON | 标准姿态关节点坐标 |
| pass_score | INT | 通过分数（默认60） |
| sticker_reward | VARCHAR(100) | 奖励贴纸ID |
| created_at | DATETIME | 创建时间 |

### 2.2 用户表 user
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| nickname | VARCHAR(50) | 昵称 |
| avatar_url | VARCHAR(200) | 头像URL |
| total_score | INT | 累计得分 |
| level_unlock | INT | 已解锁关卡数 |
| stickers | JSON | 已获得贴纸列表 |
| created_at | DATETIME | 创建时间 |

### 2.3 闯关记录表 game_record
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| user_id | INT | 用户ID |
| level_id | INT | 关卡ID |
| score | INT | 本次得分 |
| is_pass | TINYINT | 是否通过（0失败/1通过） |
| video_url | VARCHAR(200) | 视频/图片URL |
| created_at | DATETIME | 闯关时间 |

### 2.4 贴纸表 sticker
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| name | VARCHAR(50) | 贴纸名称 |
| image_url | VARCHAR(200) | 贴纸图片URL |
| unlock_level | INT | 解锁所需关卡数 |

---

## 三、接口规范

### 3.1 基础规范
- **Base URL**：`http://localhost:5000/api`
- **数据格式**：JSON
- **编码格式**：UTF-8

### 3.2 通用响应格式

**成功响应**
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

**失败响应**
```json
{
  "code": 500,
  "msg": "error message",
  "data": null
}
```

### 3.3 认证方式
- Header 中携带 `Authorization: Bearer <token>`
- 用户注册登录后获取 token

---

## 四、接口详情

### 4.1 用户模块

#### 4.1.1 用户注册
**接口路径**：`POST /api/user/register`

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | String | 是 | 昵称 |
| password | String | 是 | 密码 |

**请求示例**
```json
{
  "nickname": "张三",
  "password": "123456"
}
```

**响应示例**
```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {
    "userId": 1,
    "nickname": "张三",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

#### 4.1.2 用户登录
**接口路径**：`POST /api/user/login`

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | String | 是 | 昵称 |
| password | String | 是 | 密码 |

**请求示例**
```json
{
  "nickname": "张三",
  "password": "123456"
}
```

**响应示例**
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "userId": 1,
    "nickname": "张三",
    "avatarUrl": "https://xxx/avatar.png",
    "totalScore": 850,
    "levelUnlock": 5,
    "stickers": ["sticker_001", "sticker_002"],
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

#### 4.1.3 获取用户信息
**接口路径**：`GET /api/user/info`

**请求头**
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer token |

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "nickname": "张三",
    "avatarUrl": "https://xxx/avatar.png",
    "totalScore": 850,
    "levelUnlock": 5,
    "stickers": ["sticker_001", "sticker_002"]
  }
}
```

---

### 4.2 关卡模块

#### 4.2.1 获取关卡列表
**接口路径**：`GET /api/levels`

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "比心挑战",
        "description": "双手比心，爱心满满",
        "poseType": "heart",
        "passScore": 60,
        "stickerReward": "sticker_001",
        "isUnlocked": true,
        "isPassed": false
      },
      {
        "id": 2,
        "name": "举手欢呼",
        "description": "双手高举，欢呼庆祝",
        "poseType": "hands_up",
        "passScore": 60,
        "stickerReward": "sticker_002",
        "isUnlocked": true,
        "isPassed": true
      }
    ],
    "total": 12
  }
}
```

#### 4.2.2 获取关卡详情
**接口路径**：`GET /api/levels/{id}`

**路径参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 关卡ID |

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "比心挑战",
    "description": "双手比心，爱心满满",
    "poseType": "heart",
    "targetPose": {
      "keypoints": [
        {"x": 0.5, "y": 0.2, "name": "nose"},
        {"x": 0.3, "y": 0.4, "name": "left_shoulder"},
        {"x": 0.7, "y": 0.4, "name": "right_shoulder"},
        {"x": 0.2, "y": 0.6, "name": "left_elbow"},
        {"x": 0.8, "y": 0.6, "name": "right_elbow"}
      ],
      "imageUrl": "https://xxx/heart_pose.png"
    },
    "passScore": 60,
    "stickerReward": "sticker_001"
  }
}
```

#### 4.2.3 获取所有动作类型
**接口路径**：`GET /api/pose/types`

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {"type": "heart", "name": "比心", "icon": "❤️"},
      {"type": "hands_up", "name": "举手", "icon": "🙋"},
      {"type": "squat", "name": "深蹲", "icon": "🏋️"},
      {"type": "tilt_head", "name": "歪头杀", "icon": "😴"},
      {"type": "wave", "name": "招手", "icon": "👋"},
      {"type": "peace", "name": "比耶", "icon": "✌️"},
      {"type": "thumb_up", "name": "点赞", "icon": "👍"},
      {"type": "dance", "name": "跳舞", "icon": "💃"},
      {"type": "stretch", "name": "伸展", "icon": "🧘"},
      {"type": "jump", "name": "跳跃", "icon": "🏃"}
    ]
  }
}
```

---

### 4.3 姿态识别模块（核心）

#### 4.3.1 提交姿态图片进行识别
**接口路径**：`POST /api/pose/recognize`

**请求头**
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer token |
| Content-Type | 是 | multipart/form-data |

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image | File | 是 | 用户拍摄的图片 |
| level_id | Integer | 是 | 关卡ID |

**响应示例**
```json
{
  "code": 200,
  "msg": "识别成功",
  "data": {
    "score": 85,
    "isPass": true,
    "detectedPose": {
      "keypoints": [
        {"x": 0.52, "y": 0.18, "name": "nose", "confidence": 0.95},
        {"x": 0.28, "y": 0.42, "name": "left_shoulder", "confidence": 0.92}
      ]
    },
    "targetPose": {
      "keypoints": [...]
    },
    "matchDetails": {
      "head": 90,
      "shoulders": 85,
      "arms": 80,
      "body": 88
    },
    "stickerReward": "sticker_001"
  }
}
```

#### 4.3.2 提交视频流进行实时识别
**接口路径**：`POST /api/pose/recognize/video`

**请求头**
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer token |

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| video | File | 是 | 短视频文件（建议≤5秒） |
| level_id | Integer | 是 | 关卡ID |

**响应示例**
```json
{
  "code": 200,
  "msg": "识别成功",
  "data": {
    "score": 78,
    "isPass": true,
    "bestFrame": {
      "imageUrl": "https://xxx/frame_3.png",
      "score": 78
    },
    "stickerReward": null
  }
}
```

#### 4.3.3 实时流识别（WebSocket）
**接口路径**：`WS /api/pose/live`

**连接参数**：`ws://localhost:5000/api/pose/live?token=<token>&level_id=<level_id>`

**服务端推送消息**
| 消息类型 | 说明 | 示例 |
|----------|------|------|
| pose_update | 检测到姿态 | `{"type":"pose_update","data":{"keypoints":[...],"score":75}}` |
| frame_result | 单帧结果 | `{"type":"frame_result","data":{"score":72,"isPass":false}}` |
| final_result | 最终结果 | `{"type":"final_result","data":{"score":80,"isPass":true,"stickerReward":"sticker_001"}}` |

**客户端发送消息**
| 消息类型 | 说明 | 示例 |
|----------|------|------|
| video_frame | 视频帧 | `{"type":"video_frame","data":"base64..."}` |

---

### 4.4 游戏记录模块

#### 4.4.1 提交闯关结果
**接口路径**：`POST /api/game/record`

**请求头**
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer token |

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| level_id | Integer | 是 | 关卡ID |
| score | Integer | 是 | 得分 |
| is_pass | Boolean | 是 | 是否通过 |
| media_url | String | 否 | 视频/图片URL |

**请求示例**
```json
{
  "levelId": 1,
  "score": 85,
  "isPass": true,
  "mediaUrl": "https://xxx/video_1.mp4"
}
```

**响应示例**
```json
{
  "code": 200,
  "msg": "记录成功",
  "data": {
    "recordId": 100,
    "score": 85,
    "isPass": true,
    "stickerUnlocked": "sticker_001",
    "newStickers": ["sticker_001"]
  }
}
```

#### 4.4.2 获取闯关记录
**接口路径**：`GET /api/game/records`

**请求参数**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 100,
        "levelId": 1,
        "levelName": "比心挑战",
        "score": 85,
        "isPass": true,
        "createdAt": "2026-05-30 10:30:00"
      }
    ],
    "total": 25,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 4.4.3 获取关卡排行榜
**接口路径**：`GET /api/game/leaderboard/{level_id}`

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "levelId": 1,
    "levelName": "比心挑战",
    "ranking": [
      {"rank": 1, "nickname": "张三", "score": 98, "avatarUrl": "..."},
      {"rank": 2, "nickname": "李四", "score": 95, "avatarUrl": "..."},
      {"rank": 3, "nickname": "王五", "score": 92, "avatarUrl": "..."}
    ]
  }
}
```

---

### 4.5 贴纸模块

#### 4.5.1 获取用户贴纸列表
**接口路径**：`GET /api/stickers`

**请求头**
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer token |

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {"id": 1, "name": "爱心贴纸", "imageUrl": "https://xxx/sticker_1.png", "unlockLevel": 1},
      {"id": 2, "name": "星星贴纸", "imageUrl": "https://xxx/sticker_2.png", "unlockLevel": 3}
    ],
    "total": 5
  }
}
```

#### 4.5.2 获取所有贴纸（包括未解锁）
**接口路径**：`GET /api/stickers/all`

**响应示例**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {"id": 1, "name": "爱心贴纸", "imageUrl": "...", "unlockLevel": 1, "isUnlocked": true},
      {"id": 2, "name": "星星贴纸", "imageUrl": "...", "unlockLevel": 3, "isUnlocked": false},
      {"id": 3, "name": "皇冠贴纸", "imageUrl": "...", "unlockLevel": 10, "isUnlocked": false}
    ]
  }
}
```

---

## 五、前端与后端交互流程

### 5.1 用户登录流程
```
1. 前端：用户输入昵称密码，点击登录
2. 前端：调用 POST /api/user/login
3. 后端：验证账号，返回token和用户信息
4. 前端：保存token到本地，跳转到主页
```

### 5.2 开始闯关流程
```
1. 前端：展示关卡列表，用户选择关卡
2. 前端：调用 GET /api/levels/{id} 获取关卡详情和标准姿态
3. 前端：显示标准动作示范图，用户点击开始
4. 前端：打开相机，用户做动作并拍照/录像
5. 前端：调用 POST /api/pose/recognize 提交识别
6. 后端：转发给AI模型计算相似度，返回得分
7. 前端：展示得分，成功则发放贴纸奖励
8. 前端：调用 POST /api/game/record 保存闯关记录
```

### 5.3 实时闯关流程（WebSocket）
```
1. 前端：连接 WS /api/pose/live
2. 前端：开始实时视频流推送
3. 后端：逐帧调用AI模型，实时推送得分
4. 前端：显示实时得分和姿态对比
5. 视频结束，后端发送最终结果
6. 前端：显示闯关成功/失败，发放奖励
```

---

## 六、团队分工

### 6.1 分工总览

| 角色 | 成员 | 负责模块 | 接口范围 |
|------|------|----------|----------|
| 移动端前端 | 前端开发 | Android界面、相机交互 | 调用 4.1、4.2、4.3、4.4、4.5 |
| 后端 | 后端开发 | 接口服务、AI转发、数据存储 | 实现全部接口 |
| AI模型 | AI开发 | 姿态识别、相似度计算 | 提供识别SDK/服务 |

### 6.2 前端开发任务

| 任务 | 说明 | 对应接口 |
|------|------|----------|
| T1 | 登录注册界面 | 4.1.1、4.1.2 |
| T2 | 主页、关卡列表 | 4.2.1 |
| T3 | 关卡详情、标准动作展示 | 4.2.2、4.2.3 |
| T4 | 相机预览、拍照/录像 | 4.3.1、4.3.2 |
| T5 | 实时识别界面（WebSocket） | 4.3.3 |
| T6 | 得分展示、贴纸奖励 | 4.4.1、4.5.1 |
| T7 | 闯关记录、历史排行榜 | 4.4.2、4.4.3 |
| T8 | 贴纸墙、个人中心 | 4.5.1、4.5.2 |

### 6.3 后端开发任务

| 任务 | 说明 | 对应接口 |
|------|------|----------|
| B1 | 用户模块（注册、登录、信息） | 4.1 |
| B2 | 关卡模块（CRUD、列表） | 4.2 |
| B3 | AI识别接口（图片/视频/实时） | 4.3 |
| B4 | 游戏记录（提交、查询、排行） | 4.4 |
| B5 | 贴纸模块 | 4.5 |
| B6 | 数据库设计与维护 | - |
| B7 | AI模型服务集成 | - |

### 6.4 AI开发任务

| 任务 | 说明 |
|------|------|
| A1 | 集成 MediaPipe Pose / YOLO Pose 模型 |
| A2 | 实现关节点提取（33个关键点） |
| A3 | 实现姿态相似度计算算法 |
| A4 | 提供 Python SDK / REST API 接口 |
| A5 | 优化推理性能，支持实时处理 |

---

## 七、AI 模型规格

### 7.1 姿态识别模型
- **推荐模型**：MediaPipe Pose（轻量级，33个关节点）
- **备选模型**：YOLO Pose

### 7.2 关节点列表
| 序号 | 名称 | 说明 |
|------|------|------|
| 0 | nose | 鼻子 |
| 1 | left_eye_inner | 左眼内侧 |
| 2 | left_eye | 左眼 |
| 3 | left_eye_outer | 左眼外侧 |
| 4 | right_eye_inner | 右眼内侧 |
| 5 | right_eye | 右眼 |
| 6 | right_eye_outer | 右眼外侧 |
| 7 | left_ear | 左耳 |
| 8 | right_ear | 右耳 |
| 9 | mouth_left | 左嘴角 |
| 10 | mouth_right | 右嘴角 |
| 11 | left_shoulder | 左肩 |
| 12 | right_shoulder | 右肩 |
| 13 | left_elbow | 左肘 |
| 14 | right_elbow | 右肘 |
| 15 | left_wrist | 左腕 |
| 16 | right_wrist | 右腕 |
| 17 | left_pinky | 左小指 |
| 18 | right_pinky | 右小指 |
| 19 | left_index | 左食指 |
| 20 | right_index | 右食指 |
| 21 | left_thumb | 左拇指 |
| 22 | right_thumb | 右拇指 |
| 23 | left_hip | 左腰 |
| 24 | right_hip | 右腰 |
| 25 | left_knee | 左膝 |
| 26 | right_knee | 右膝 |
| 27 | left_ankle | 左踝 |
| 28 | right_ankle | 右踝 |
| 29 | left_heel | 左脚跟 |
| 30 | right_heel | 右脚跟 |
| 31 | left_foot_index | 左脚趾 |
| 32 | right_foot_index | 右脚趾 |

### 7.3 相似度计算算法
```
1. 提取用户姿态关节点坐标
2. 提取标准姿态关节点坐标
3. 计算各关节点距离（欧氏距离）
4. 加权计算总体相似度
5. 输出 0-100 分
```

---

## 八、版本信息

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-05-30 | 初始版本 |