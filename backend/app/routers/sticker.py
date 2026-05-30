from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session
import jwt

from ..database import get_db
from ..models import Sticker, User

router = APIRouter(prefix="/api/stickers", tags=["stickers"])

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


@router.get("", response_model=dict)
def get_my_stickers(
    authorization: str = Header(None),
    db: Session = Depends(get_db)
):
    user_id = get_user_id(authorization)
    user = db.query(User).filter(User.id == user_id).first()

    if not user or not user.stickers:
        return {"code": 200, "msg": "success", "data": {"list": [], "total": 0}}

    sticker_ids = []
    for s in user.stickers:
        if s.startswith("sticker_"):
            try:
                sticker_ids.append(int(s.replace("sticker_", "")))
            except:
                pass

    stickers = db.query(Sticker).filter(Sticker.id.in_(sticker_ids)).all() if sticker_ids else []

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": [{"id": s.id, "name": s.name, "imageUrl": s.image_url, "unlockLevel": s.unlock_level} for s in stickers],
            "total": len(stickers)
        }
    }


@router.get("/all", response_model=dict)
def get_all_stickers(db: Session = Depends(get_db)):
    stickers = db.query(Sticker).all()
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": [{"id": s.id, "name": s.name, "imageUrl": s.image_url, "unlockLevel": s.unlock_level, "isUnlocked": False} for s in stickers]
        }
    }