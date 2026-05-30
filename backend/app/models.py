from sqlalchemy import Column, Integer, String, DateTime, Boolean, JSON
from datetime import datetime
from .database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    nickname = Column(String(50), unique=True, index=True, nullable=False)
    password = Column(String(100), nullable=False)
    avatar_url = Column(String(200), nullable=True)
    total_score = Column(Integer, default=0)
    level_unlock = Column(Integer, default=1)
    stickers = Column(JSON, default=list)
    created_at = Column(DateTime, default=datetime.utcnow)


class Level(Base):
    __tablename__ = "levels"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)
    description = Column(String(200), nullable=True)
    pose_type = Column(String(50), nullable=False)
    target_pose = Column(JSON, nullable=False)
    pass_score = Column(Integer, default=60)
    sticker_reward = Column(String(100), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class GameRecord(Base):
    __tablename__ = "game_records"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, nullable=False)
    level_id = Column(Integer, nullable=False)
    score = Column(Integer, nullable=False)
    is_pass = Column(Boolean, default=False)
    media_url = Column(String(200), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class Sticker(Base):
    __tablename__ = "stickers"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)
    image_url = Column(String(200), nullable=False)
    unlock_level = Column(Integer, default=1)