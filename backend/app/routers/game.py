from fastapi import APIRouter, Depends, HTTPException, Header
from sqlalchemy.orm import Session
from sqlalchemy import desc
import jwt

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


@router.post("/record", response_model=dict)
def submit_record(
    record,
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    user_id = get_user_id(authorization)

    level = db.query(Level).filter(Level.id == record.levelId).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    db_record = GameRecord(
        user_id=user_id,
        level_id=record.levelId,
        score=record.score,
        is_pass=record.isPass,
        media_url=record.mediaUrl
    )
    db.add(db_record)

    user = db.query(User).filter(User.id == user_id).first()
    new_stickers = []
    if user:
        user.total_score += record.score
        if record.isPass and user.level_unlock <= record.levelId:
            user.level_unlock = record.levelId + 1

        if record.isPass and level.sticker_reward:
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
            "score": record.score,
            "isPass": record.isPass,
            "stickerUnlocked": level.sticker_reward if record.isPass else None,
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