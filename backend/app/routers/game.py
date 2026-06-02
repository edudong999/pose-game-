from fastapi import APIRouter, Depends, File, HTTPException, Header, UploadFile
from sqlalchemy.orm import Session
from sqlalchemy import desc
import jwt
import os
import uuid

from ..database import get_db
from ..models import User, Level, GameRecord

router = APIRouter(prefix="/api/game", tags=["game"])

SECRET_KEY = "pose_game_secret_key_2024"
ALGORITHM = "HS256"


def get_user_id(authorization: str = None) -> int:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未授权")
    token = authorization.replace("Bearer ", "")
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return int(payload.get("sub"))
    except:
        raise HTTPException(status_code=401, detail="token无效")


@router.post("/record/media")
async def upload_record_media(
    image: UploadFile = File(...),
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    """上传闯关截图，返回可访问的 URL"""
    get_user_id(authorization)  # 鉴权，未登录直接 401

    save_dir = "static/records"
    os.makedirs(save_dir, exist_ok=True)
    ext = os.path.splitext(image.filename or "")[1] or ".jpg"
    filename = f"{uuid.uuid4().hex}{ext}"
    save_path = os.path.join(save_dir, filename)

    contents = await image.read()
    with open(save_path, "wb") as f:
        f.write(contents)

    return {
        "code": 200,
        "msg": "上传成功",
        "data": {"imageUrl": f"/static/records/{filename}"}
    }


@router.post("/record", response_model=dict)
def submit_record(
    levelId: int,
    score: int,
    isPass: bool,
    mediaUrl: str = None,
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    user_id = get_user_id(authorization)

    level = db.query(Level).filter(Level.id == levelId).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    db_record = GameRecord(
        user_id=user_id,
        level_id=levelId,
        score=score,
        is_pass=isPass,
        media_url=mediaUrl
    )
    db.add(db_record)

    user = db.query(User).filter(User.id == user_id).first()
    new_stickers = []
    if user:
        user.total_score += score
        if isPass and user.level_unlock <= levelId:
            user.level_unlock = levelId + 1

        if isPass and level.sticker_reward:
            if user.stickers is None:
                user.stickers = []
            if level.sticker_reward not in user.stickers:
                user.stickers.append(level.sticker_reward)
                new_stickers.append(level.sticker_reward)

    db.commit()

    return {
        "code": 200,
        "msg": "记录成功",
        "data": {
            "recordId": db_record.id,
            "score": score,
            "isPass": isPass,
            "stickerUnlocked": level.sticker_reward if isPass else None,
            "newStickers": new_stickers
        }
    }


@router.get("/records", response_model=dict)
def get_records(
    page: int = 1,
    pageSize: int = 10,
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    user_id = get_user_id(authorization)

    records = db.query(GameRecord).filter(
        GameRecord.user_id == user_id
    ).order_by(desc(GameRecord.created_at)).offset((page-1)*pageSize).limit(pageSize).all()

    total = db.query(GameRecord).filter(GameRecord.user_id == user_id).count()

    result = []
    for record in records:
        level = db.query(Level).filter(Level.id == record.level_id).first()
        result.append({
            "id": record.id,
            "levelId": record.level_id,
            "levelName": level.name if level else "未知关卡",
            "score": record.score,
            "isPass": record.is_pass,
            "mediaUrl": record.media_url,
            "createdAt": record.created_at.isoformat() if record.created_at else None
        })

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": result,
            "total": total,
            "page": page,
            "pageSize": pageSize
        }
    }


@router.get("/leaderboard/{level_id}", response_model=dict)
def get_leaderboard(level_id: int, db: Session = Depends(get_db)):
    records = db.query(GameRecord).filter(
        GameRecord.level_id == level_id,
        GameRecord.is_pass == True
    ).order_by(desc(GameRecord.score)).limit(10).all()

    level = db.query(Level).filter(Level.id == level_id).first()

    ranking = []
    for i, record in enumerate(records):
        user = db.query(User).filter(User.id == record.user_id).first()
        ranking.append({
            "rank": i + 1,
            "nickname": user.nickname if user else "匿名",
            "score": record.score,
            "avatarUrl": user.avatar_url if user else None
        })

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "levelId": level_id,
            "levelName": level.name if level else "未知关卡",
            "ranking": ranking
        }
    }