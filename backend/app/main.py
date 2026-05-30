from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .routers import user_router, level_router, pose_router, game_router, sticker_router
from .database import engine
from . import models

models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="AI Pose Game API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(user_router)
app.include_router(level_router)
app.include_router(pose_router)
app.include_router(game_router)
app.include_router(sticker_router)


@app.get("/")
def root():
    return {"message": "AI Pose Game API", "version": "1.0.0"}


@app.get("/health")
def health():
    return {"status": "ok"}


def init_levels():
    from .database import SessionLocal
    from .models import Level

    db = SessionLocal()
    existing = db.query(Level).count()
    if existing > 0:
        db.close()
        return

    levels = [
        {"name": "比心挑战", "description": "双手比心，爱心满满", "pose_type": "heart", "pass_score": 60, "sticker_reward": "sticker_001"},
        {"name": "举手欢呼", "description": "双手高举，欢呼庆祝", "pose_type": "hands_up", "pass_score": 60, "sticker_reward": "sticker_002"},
        {"name": "深蹲挑战", "description": "标准深蹲姿势", "pose_type": "squat", "pass_score": 65, "sticker_reward": "sticker_003"},
        {"name": "歪头杀", "description": "萌萌歪头杀", "pose_type": "tilt_head", "pass_score": 55, "sticker_reward": "sticker_004"},
        {"name": "招手问候", "description": "热情招手打招呼", "pose_type": "wave", "pass_score": 60, "sticker_reward": "sticker_005"},
        {"name": "比耶胜利", "description": "胜利手势V", "pose_type": "peace", "pass_score": 60, "sticker_reward": "sticker_006"},
        {"name": "点赞鼓励", "description": "给个赞吧", "pose_type": "thumb_up", "pass_score": 55, "sticker_reward": "sticker_007"},
        {"name": "跳舞狂欢", "description": "跟着节奏摇摆", "pose_type": "dance", "pass_score": 70, "sticker_reward": "sticker_008"},
        {"name": "伸展运动", "description": "伸个懒腰", "pose_type": "stretch", "pass_score": 60, "sticker_reward": "sticker_009"},
        {"name": "跳跃挑战", "description": "原地跳跃", "pose_type": "jump", "pass_score": 65, "sticker_reward": "sticker_010"},
    ]

    for i, level_data in enumerate(levels, 1):
        target_pose = {
            "keypoints": [
                {"x": 0.5, "y": 0.1, "name": "nose"},
                {"x": 0.3, "y": 0.3, "name": "left_shoulder"},
                {"x": 0.7, "y": 0.3, "name": "right_shoulder"},
            ]
        }
        level = Level(
            id=i,
            name=level_data["name"],
            description=level_data["description"],
            pose_type=level_data["pose_type"],
            target_pose=target_pose,
            pass_score=level_data["pass_score"],
            sticker_reward=level_data["sticker_reward"]
        )
        db.add(level)

    db.commit()
    db.close()


def init_stickers():
    from .database import SessionLocal
    from .models import Sticker

    db = SessionLocal()
    existing = db.query(Sticker).count()
    if existing > 0:
        db.close()
        return

    stickers = [
        {"id": 1, "name": "爱心贴纸", "image_url": "https://xxx/sticker_heart.png", "unlock_level": 1},
        {"id": 2, "name": "星星贴纸", "image_url": "https://xxx/sticker_star.png", "unlock_level": 2},
        {"id": 3, "name": "皇冠贴纸", "image_url": "https://xxx/sticker_crown.png", "unlock_level": 5},
        {"id": 4, "name": "彩虹贴纸", "image_url": "https://xxx/sticker_rainbow.png", "unlock_level": 3},
        {"id": 5, "name": "火焰贴纸", "image_url": "https://xxx/sticker_fire.png", "unlock_level": 4},
        {"id": 6, "name": "钻石贴纸", "image_url": "https://xxx/sticker_diamond.png", "unlock_level": 6},
        {"id": 7, "name": "月亮贴纸", "image_url": "https://xxx/sticker_moon.png", "unlock_level": 7},
        {"id": 8, "name": "太阳贴纸", "image_url": "https://xxx/sticker_sun.png", "unlock_level": 8},
        {"id": 9, "name": "花朵贴纸", "image_url": "https://xxx/sticker_flower.png", "unlock_level": 9},
        {"id": 10, "name": "星星贴纸", "image_url": "https://xxx/sticker_goldstar.png", "unlock_level": 10},
    ]

    for s in stickers:
        sticker = Sticker(**s)
        db.add(sticker)

    db.commit()
    db.close()


@app.on_event("startup")
def startup_event():
    init_levels()
    init_stickers()