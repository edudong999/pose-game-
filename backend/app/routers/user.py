from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from passlib.context import CryptContext
import jwt
from datetime import datetime, timedelta

from ..database import get_db
from ..models import User
from ..schemas import UserCreate, UserLogin, UserResponse, UserLoginResponse

router = APIRouter(prefix="/api/user", tags=["user"])
SECRET_KEY = "pose_game_secret_key_2024"
ALGORITHM = "HS256"

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def create_token(user_id: int) -> str:
    expire = datetime.utcnow() + timedelta(days=7)
    to_encode = {"sub": str(user_id), "exp": expire}
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


@router.post("/register", response_model=dict)
def register(user: UserCreate, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.nickname == user.nickname).first()
    if existing:
        raise HTTPException(status_code=400, detail="用户已存在")

    hashed_password = pwd_context.hash(user.password)
    db_user = User(nickname=user.nickname, password=hashed_password)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    token = create_token(db_user.id)
    return {
        "code": 200,
        "msg": "注册成功",
        "data": {
            "userId": db_user.id,
            "nickname": db_user.nickname,
            "token": token
        }
    }


@router.post("/login", response_model=dict)
def login(user: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.nickname == user.nickname).first()
    if not db_user or not pwd_context.verify(user.password, db_user.password):
        raise HTTPException(status_code=401, detail="用户名或密码错误")

    token = create_token(db_user.id)
    return {
        "code": 200,
        "msg": "登录成功",
        "data": {
            "userId": db_user.id,
            "nickname": db_user.nickname,
            "avatarUrl": db_user.avatar_url,
            "totalScore": db_user.total_score,
            "levelUnlock": db_user.level_unlock,
            "stickers": db_user.stickers or [],
            "token": token
        }
    }


@router.get("/info", response_model=dict)
def get_user_info(authorization: str = None, db: Session = Depends(get_db)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未授权")

    token = authorization.replace("Bearer ", "")
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id = int(payload.get("sub"))
    except:
        raise HTTPException(status_code=401, detail="token无效")

    db_user = db.query(User).filter(User.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="用户不存在")

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "userId": db_user.id,
            "nickname": db_user.nickname,
            "avatarUrl": db_user.avatar_url,
            "totalScore": db_user.total_score,
            "levelUnlock": db_user.level_unlock,
            "stickers": db_user.stickers or []
        }
    }