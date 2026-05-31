from fastapi import APIRouter, HTTPException, Header
from pydantic import BaseModel
from typing import Optional, List

router = APIRouter(prefix="/api/user", tags=["user"])


class UserLoginRequest(BaseModel):
    nickname: str
    password: str


class UserRegisterRequest(BaseModel):
    nickname: str
    password: str


@router.post("/register", response_model=dict)
def register(user: UserRegisterRequest):
    return {
        "code": 200,
        "msg": "注册成功",
        "data": {
            "userId": 1,
            "nickname": user.nickname,
            "token": "mock_token"
        }
    }


@router.post("/login", response_model=dict)
def login(user: UserLoginRequest):
    return {
        "code": 200,
        "msg": "登录成功",
        "data": {
            "userId": 1,
            "nickname": user.nickname,
            "avatarUrl": None,
            "totalScore": 850,
            "levelUnlock": 5,
            "stickers": ["sticker_001", "sticker_002", "sticker_003"],
            "token": "mock_token"
        }
    }


@router.get("/info", response_model=dict)
def get_user_info(authorization: str = Header(None)):
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "userId": 1,
            "nickname": "测试用户",
            "avatarUrl": None,
            "totalScore": 850,
            "levelUnlock": 5,
            "stickers": ["sticker_001", "sticker_002", "sticker_003"]
        }
    }