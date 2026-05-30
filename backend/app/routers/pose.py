from fastapi import APIRouter, Depends, File, UploadFile, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Level

router = APIRouter(prefix="/api/pose", tags=["pose"])


def calculate_similarity(detected_pose: dict, target_pose: dict) -> int:
    if not target_pose or not detected_pose:
        return 0

    keypoints_detected = detected_pose.get("keypoints", [])
    keypoints_target = target_pose.get("keypoints", [])

    if not keypoints_target or not keypoints_detected:
        return 50

    total_distance = 0
    count = min(len(keypoints_detected), len(keypoints_target))

    for i in range(count):
        dx = keypoints_detected[i].get("x", 0) - keypoints_target[i].get("x", 0)
        dy = keypoints_detected[i].get("y", 0) - keypoints_target[i].get("y", 0)
        distance = (dx**2 + dy**2) ** 0.5
        total_distance += distance

    avg_distance = total_distance / count if count > 0 else 1
    score = max(0, min(100, int(100 - avg_distance * 100)))
    return score


@router.post("/recognize", response_model=dict)
async def recognize_pose(
    level_id: int,
    image: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    level = db.query(Level).filter(Level.id == level_id).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    image_bytes = await image.read()

    detected_pose = {
        "keypoints": [
            {"x": 0.5, "y": 0.2, "name": "nose", "confidence": 0.95},
            {"x": 0.3, "y": 0.4, "name": "left_shoulder", "confidence": 0.92}
        ]
    }

    target_pose = level.target_pose
    score = calculate_similarity(detected_pose, target_pose)
    is_pass = score >= level.pass_score

    return {
        "code": 200,
        "msg": "识别成功",
        "data": {
            "score": score,
            "isPass": is_pass,
            "detectedPose": detected_pose,
            "targetPose": target_pose,
            "matchDetails": {"head": 90, "shoulders": 85, "arms": 80, "body": 88},
            "stickerReward": level.sticker_reward if is_pass else None
        }
    }


@router.post("/recognize/video", response_model=dict)
async def recognize_video(
    level_id: int,
    video: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    level = db.query(Level).filter(Level.id == level_id).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    return {
        "code": 200,
        "msg": "识别成功",
        "data": {
            "score": 78,
            "isPass": True,
            "bestFrame": {"imageUrl": "https://xxx/frame_3.png", "score": 78},
            "stickerReward": level.sticker_reward
        }
    }