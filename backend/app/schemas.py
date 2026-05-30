from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime


# User Schemas
class UserCreate(BaseModel):
    nickname: str
    password: str


class UserLogin(BaseModel):
    nickname: str
    password: str


class UserResponse(BaseModel):
    id: int
    nickname: str
    avatar_url: Optional[str] = None
    total_score: int = 0
    level_unlock: int = 1
    stickers: List[str] = []

    class Config:
        from_attributes = True


class UserLoginResponse(BaseModel):
    userId: int
    nickname: str
    avatarUrl: Optional[str] = None
    totalScore: int = 0
    levelUnlock: int = 1
    stickers: List[str] = []
    token: str


# Level Schemas
class Keypoint(BaseModel):
    x: float
    y: float
    name: Optional[str] = None
    confidence: Optional[float] = None


class TargetPose(BaseModel):
    keypoints: List[Keypoint]
    imageUrl: Optional[str] = None


class LevelResponse(BaseModel):
    id: int
    name: str
    description: Optional[str] = None
    poseType: str
    passScore: int = 60
    stickerReward: Optional[str] = None
    isUnlocked: bool = False
    isPassed: bool = False

    class Config:
        from_attributes = True


class LevelDetailResponse(BaseModel):
    id: int
    name: str
    description: Optional[str] = None
    poseType: str
    targetPose: TargetPose
    passScore: int = 60
    stickerReward: Optional[str] = None


# Pose Schemas
class PoseTypeResponse(BaseModel):
    type: str
    name: str
    icon: str


class PoseRecognizeResponse(BaseModel):
    score: int
    isPass: bool
    detectedPose: Optional[TargetPose] = None
    targetPose: Optional[TargetPose] = None
    matchDetails: Optional[dict] = None
    stickerReward: Optional[str] = None


# Game Schemas
class GameRecordCreate(BaseModel):
    levelId: int
    score: int
    isPass: bool
    mediaUrl: Optional[str] = None


class GameRecordResponse(BaseModel):
    id: int
    levelId: int
    levelName: Optional[str] = None
    score: int
    isPass: bool
    createdAt: datetime

    class Config:
        from_attributes = True


class LeaderboardEntry(BaseModel):
    rank: int
    nickname: str
    score: int
    avatarUrl: Optional[str] = None


# Sticker Schemas
class StickerResponse(BaseModel):
    id: int
    name: str
    imageUrl: str
    unlockLevel: int
    isUnlocked: Optional[bool] = None

    class Config:
        from_attributes = True