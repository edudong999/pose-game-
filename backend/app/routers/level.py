from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..database import get_db

router = APIRouter(prefix="/api/levels", tags=["levels"])

POSE_TYPES = [
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

MOCK_LEVELS = [
    {"id": 1, "name": "比心挑战", "description": "双手比心，爱心满满", "pose_type": "heart", "pass_score": 60, "sticker_reward": "sticker_001"},
    {"id": 2, "name": "举手欢呼", "description": "双手高举，欢呼庆祝", "pose_type": "hands_up", "pass_score": 60, "sticker_reward": "sticker_002"},
    {"id": 3, "name": "深蹲挑战", "description": "标准深蹲姿势", "pose_type": "squat", "pass_score": 65, "sticker_reward": "sticker_003"},
    {"id": 4, "name": "歪头杀", "description": "萌萌歪头杀", "pose_type": "tilt_head", "pass_score": 55, "sticker_reward": "sticker_004"},
    {"id": 5, "name": "招手问候", "description": "热情招手打招呼", "pose_type": "wave", "pass_score": 60, "sticker_reward": "sticker_005"},
    {"id": 6, "name": "比耶胜利", "description": "胜利手势V", "pose_type": "peace", "pass_score": 60, "sticker_reward": "sticker_006"},
    {"id": 7, "name": "点赞鼓励", "description": "给个赞吧", "pose_type": "thumb_up", "pass_score": 55, "sticker_reward": "sticker_007"},
    {"id": 8, "name": "跳舞狂欢", "description": "跟着节奏摇摆", "pose_type": "dance", "pass_score": 70, "sticker_reward": "sticker_008"},
    {"id": 9, "name": "伸展运动", "description": "伸个懒腰", "pose_type": "stretch", "pass_score": 60, "sticker_reward": "sticker_009"},
    {"id": 10, "name": "跳跃挑战", "description": "原地跳跃", "pose_type": "jump", "pass_score": 65, "sticker_reward": "sticker_010"}
]


@router.get("", response_model=dict)
def get_levels(db: Session = Depends(get_db)):
    result = []
    for level in MOCK_LEVELS:
        result.append({
            "id": level["id"],
            "name": level["name"],
            "description": level["description"],
            "poseType": level["pose_type"],
            "passScore": level["pass_score"],
            "stickerReward": level["sticker_reward"],
            "isUnlocked": level["id"] <= 3,
            "isPassed": level["id"] <= 2
        })
    return {"code": 200, "msg": "success", "data": {"list": result, "total": len(result)}}


@router.get("/{level_id}", response_model=dict)
def get_level(level_id: int, db: Session = Depends(get_db)):
    for level in MOCK_LEVELS:
        if level["id"] == level_id:
            return {
                "code": 200,
                "msg": "success",
                "data": {
                    "id": level["id"],
                    "name": level["name"],
                    "description": level["description"],
                    "poseType": level["pose_type"],
                    "targetPose": {"keypoints": [{"x": 0.5, "y": 0.1, "name": "nose"}]},
                    "passScore": level["pass_score"],
                    "stickerReward": level["sticker_reward"]
                }
            }
    return {"code": 404, "msg": "关卡不存在", "data": None}


@router.get("/pose/types", response_model=dict)
def get_pose_types():
    return {"code": 200, "msg": "success", "data": {"list": POSE_TYPES}}