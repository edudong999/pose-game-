from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Level, GameRecord

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


@router.get("", response_model=dict)
def get_levels(db: Session = Depends(get_db)):
    levels = db.query(Level).all()
    result = []
    for level in levels:
        passed = db.query(GameRecord).filter(
            GameRecord.level_id == level.id,
            GameRecord.is_pass == True
        ).first() is not None
        is_unlocked = level.id <= 3

        result.append({
            "id": level.id,
            "name": level.name,
            "description": level.description,
            "poseType": level.pose_type,
            "passScore": level.pass_score,
            "stickerReward": level.sticker_reward,
            "isUnlocked": is_unlocked,
            "isPassed": passed
        })

    return {"code": 200, "msg": "success", "data": {"list": result, "total": len(result)}}


@router.get("/{level_id}", response_model=dict)
def get_level(level_id: int, db: Session = Depends(get_db)):
    level = db.query(Level).filter(Level.id == level_id).first()
    if not level:
        return {"code": 404, "msg": "关卡不存在", "data": None}

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "id": level.id,
            "name": level.name,
            "description": level.description,
            "poseType": level.pose_type,
            "targetPose": level.target_pose,
            "passScore": level.pass_score,
            "stickerReward": level.sticker_reward
        }
    }


@router.get("/pose/types", response_model=dict)
def get_pose_types():
    return {"code": 200, "msg": "success", "data": {"list": POSE_TYPES}}