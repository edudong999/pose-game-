from fastapi import APIRouter, Depends, Header, HTTPException, Request
from sqlalchemy.orm import Session
import jwt

from ..database import get_db
from ..models import Level, GameRecord, User

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
def get_levels(request: Request, db: Session = Depends(get_db)):
    # Get authorization from request headers (case-insensitive)
    auth_header = request.headers.get("Authorization") or request.headers.get("authorization")

    # Get current user id if token provided
    user_id = None
    if auth_header and auth_header.startswith("Bearer "):
        try:
            SECRET_KEY = "pose_game_secret_key_2024"
            token = auth_header.replace("Bearer ", "")
            payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
            user_id = int(payload.get("sub"))
        except:
            pass

    levels = db.query(Level).all()

    result = []
    for level in levels:
        # Check if CURRENT user passed this level
        passed = False
        is_unlocked = False
        if user_id:
            passed = db.query(GameRecord).filter(
                GameRecord.level_id == level.id,
                GameRecord.user_id == user_id,
                GameRecord.is_pass == True
            ).first() is not None

            # Unlock based on user's level_unlock
            user = db.query(User).filter(User.id == user_id).first()
            if user:
                is_unlocked = level.id <= user.level_unlock
        else:
            # For anonymous users, show first 3 as unlocked
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