from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import os

# pose.py 用 print(..., file=sys.stderr) 直出，避免 uvicorn 子进程 logger 丢失。

from .routers import user_router, level_router, pose_router, game_router, sticker_router
from .database import engine
from . import models

models.Base.metadata.create_all(bind=engine)

os.makedirs("static/records", exist_ok=True)

app = FastAPI(title="AI Pose Game API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory="static"), name="static")

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

    # Target poses for each pose type - normalized coordinates (0-1)
    # Full 33 keypoints for each pose type
    TARGET_POSES = {
        "heart": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 双手在胸前比心
                {"x": 0.25, "y": 0.35, "z": 0.05, "name": "left_elbow"},
                {"x": 0.77, "y": 0.35, "z": -0.05, "name": "right_elbow"},
                {"x": 0.42, "y": 0.45, "z": 0.08, "name": "left_wrist"},
                {"x": 0.60, "y": 0.45, "z": -0.08, "name": "right_wrist"},
                {"x": 0.40, "y": 0.47, "z": 0.10, "name": "left_thumb"},
                {"x": 0.62, "y": 0.47, "z": -0.10, "name": "right_thumb"},
                {"x": 0.43, "y": 0.48, "z": 0.10, "name": "left_index"},
                {"x": 0.59, "y": 0.48, "z": -0.10, "name": "right_index"},
                {"x": 0.44, "y": 0.49, "z": 0.10, "name": "left_middle"},
                {"x": 0.58, "y": 0.49, "z": -0.10, "name": "right_middle"},
                {"x": 0.45, "y": 0.50, "z": 0.10, "name": "left_ring"},
                {"x": 0.57, "y": 0.50, "z": -0.10, "name": "right_ring"},
                {"x": 0.46, "y": 0.51, "z": 0.10, "name": "left_pinky"},
                {"x": 0.56, "y": 0.51, "z": -0.10, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "hands_up": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 双手举过头顶
                {"x": 0.30, "y": 0.12, "z": 0.08, "name": "left_elbow"},
                {"x": 0.72, "y": 0.12, "z": -0.08, "name": "right_elbow"},
                {"x": 0.28, "y": 0.05, "z": 0.10, "name": "left_wrist"},
                {"x": 0.74, "y": 0.05, "z": -0.10, "name": "right_wrist"},
                {"x": 0.26, "y": 0.04, "z": 0.12, "name": "left_thumb"},
                {"x": 0.76, "y": 0.04, "z": -0.12, "name": "right_thumb"},
                {"x": 0.27, "y": 0.05, "z": 0.12, "name": "left_index"},
                {"x": 0.75, "y": 0.05, "z": -0.12, "name": "right_index"},
                {"x": 0.28, "y": 0.06, "z": 0.12, "name": "left_middle"},
                {"x": 0.74, "y": 0.06, "z": -0.12, "name": "right_middle"},
                {"x": 0.29, "y": 0.07, "z": 0.12, "name": "left_ring"},
                {"x": 0.73, "y": 0.07, "z": -0.12, "name": "right_ring"},
                {"x": 0.30, "y": 0.08, "z": 0.12, "name": "left_pinky"},
                {"x": 0.72, "y": 0.08, "z": -0.12, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "squat": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.15, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.14, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.14, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.14, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.14, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.14, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.14, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.16, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.16, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.18, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.18, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.25, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.25, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 手臂自然下垂
                {"x": 0.32, "y": 0.38, "z": 0.05, "name": "left_elbow"},
                {"x": 0.70, "y": 0.38, "z": -0.05, "name": "right_elbow"},
                {"x": 0.35, "y": 0.50, "z": 0.08, "name": "left_wrist"},
                {"x": 0.67, "y": 0.50, "z": -0.08, "name": "right_wrist"},
                {"x": 0.34, "y": 0.52, "z": 0.10, "name": "left_thumb"},
                {"x": 0.68, "y": 0.52, "z": -0.10, "name": "right_thumb"},
                {"x": 0.35, "y": 0.53, "z": 0.10, "name": "left_index"},
                {"x": 0.67, "y": 0.53, "z": -0.10, "name": "right_index"},
                {"x": 0.36, "y": 0.54, "z": 0.10, "name": "left_middle"},
                {"x": 0.66, "y": 0.54, "z": -0.10, "name": "right_middle"},
                {"x": 0.37, "y": 0.55, "z": 0.10, "name": "left_ring"},
                {"x": 0.65, "y": 0.55, "z": -0.10, "name": "right_ring"},
                {"x": 0.38, "y": 0.56, "z": 0.10, "name": "left_pinky"},
                {"x": 0.64, "y": 0.56, "z": -0.10, "name": "right_pinky"},
                # body (6) - 深蹲姿势
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.40, "y": 0.65, "z": 0.0, "name": "left_knee"},
                {"x": 0.62, "y": 0.65, "z": 0.0, "name": "right_knee"},
                {"x": 0.40, "y": 0.80, "z": 0.0, "name": "left_ankle"},
                {"x": 0.62, "y": 0.80, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "tilt_head": {
            "keypoints": [
                # head (11) - 头歪向一侧
                {"x": 0.55, "y": 0.12, "z": 0.0, "name": "nose"},
                {"x": 0.52, "y": 0.11, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.50, "y": 0.11, "z": 0.0, "name": "left_eye"},
                {"x": 0.48, "y": 0.11, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.58, "y": 0.10, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.60, "y": 0.10, "z": 0.0, "name": "right_eye"},
                {"x": 0.62, "y": 0.10, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.45, "y": 0.13, "z": 0.0, "name": "left_ear"},
                {"x": 0.65, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.52, "y": 0.16, "z": 0.0, "name": "mouth_left"},
                {"x": 0.60, "y": 0.15, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.25, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.25, "z": 0.0, "name": "right_shoulder"},
                # arms (14)
                {"x": 0.32, "y": 0.38, "z": 0.05, "name": "left_elbow"},
                {"x": 0.70, "y": 0.38, "z": -0.05, "name": "right_elbow"},
                {"x": 0.35, "y": 0.50, "z": 0.08, "name": "left_wrist"},
                {"x": 0.67, "y": 0.50, "z": -0.08, "name": "right_wrist"},
                {"x": 0.34, "y": 0.52, "z": 0.10, "name": "left_thumb"},
                {"x": 0.68, "y": 0.52, "z": -0.10, "name": "right_thumb"},
                {"x": 0.35, "y": 0.53, "z": 0.10, "name": "left_index"},
                {"x": 0.67, "y": 0.53, "z": -0.10, "name": "right_index"},
                {"x": 0.36, "y": 0.54, "z": 0.10, "name": "left_middle"},
                {"x": 0.66, "y": 0.54, "z": -0.10, "name": "right_middle"},
                {"x": 0.37, "y": 0.55, "z": 0.10, "name": "left_ring"},
                {"x": 0.65, "y": 0.55, "z": -0.10, "name": "right_ring"},
                {"x": 0.38, "y": 0.56, "z": 0.10, "name": "left_pinky"},
                {"x": 0.64, "y": 0.56, "z": -0.10, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "wave": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 右手挥手
                {"x": 0.32, "y": 0.35, "z": 0.05, "name": "left_elbow"},
                {"x": 0.70, "y": 0.18, "z": -0.08, "name": "right_elbow"},
                {"x": 0.35, "y": 0.45, "z": 0.08, "name": "left_wrist"},
                {"x": 0.80, "y": 0.10, "z": -0.12, "name": "right_wrist"},
                {"x": 0.34, "y": 0.47, "z": 0.10, "name": "left_thumb"},
                {"x": 0.82, "y": 0.08, "z": -0.14, "name": "right_thumb"},
                {"x": 0.35, "y": 0.48, "z": 0.10, "name": "left_index"},
                {"x": 0.83, "y": 0.09, "z": -0.14, "name": "right_index"},
                {"x": 0.36, "y": 0.49, "z": 0.10, "name": "left_middle"},
                {"x": 0.82, "y": 0.10, "z": -0.14, "name": "right_middle"},
                {"x": 0.37, "y": 0.50, "z": 0.10, "name": "left_ring"},
                {"x": 0.81, "y": 0.11, "z": -0.14, "name": "right_ring"},
                {"x": 0.38, "y": 0.51, "z": 0.10, "name": "left_pinky"},
                {"x": 0.80, "y": 0.12, "z": -0.14, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "peace": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - V字手势
                {"x": 0.32, "y": 0.30, "z": 0.05, "name": "left_elbow"},
                {"x": 0.70, "y": 0.30, "z": -0.05, "name": "right_elbow"},
                {"x": 0.28, "y": 0.18, "z": 0.10, "name": "left_wrist"},
                {"x": 0.74, "y": 0.18, "z": -0.10, "name": "right_wrist"},
                {"x": 0.26, "y": 0.16, "z": 0.12, "name": "left_thumb"},
                {"x": 0.76, "y": 0.16, "z": -0.12, "name": "right_thumb"},
                {"x": 0.27, "y": 0.17, "z": 0.12, "name": "left_index"},
                {"x": 0.75, "y": 0.17, "z": -0.12, "name": "right_index"},
                {"x": 0.28, "y": 0.18, "z": 0.12, "name": "left_middle"},
                {"x": 0.74, "y": 0.18, "z": -0.12, "name": "right_middle"},
                {"x": 0.29, "y": 0.19, "z": 0.12, "name": "left_ring"},
                {"x": 0.73, "y": 0.19, "z": -0.12, "name": "right_ring"},
                {"x": 0.30, "y": 0.20, "z": 0.12, "name": "left_pinky"},
                {"x": 0.72, "y": 0.20, "z": -0.12, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "thumb_up": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 右手点赞
                {"x": 0.32, "y": 0.35, "z": 0.05, "name": "left_elbow"},
                {"x": 0.68, "y": 0.35, "z": -0.05, "name": "right_elbow"},
                {"x": 0.35, "y": 0.45, "z": 0.08, "name": "left_wrist"},
                {"x": 0.70, "y": 0.42, "z": -0.10, "name": "right_wrist"},
                {"x": 0.34, "y": 0.42, "z": 0.12, "name": "left_thumb"},
                {"x": 0.72, "y": 0.38, "z": -0.15, "name": "right_thumb"},
                {"x": 0.36, "y": 0.46, "z": 0.10, "name": "left_index"},
                {"x": 0.71, "y": 0.43, "z": -0.10, "name": "right_index"},
                {"x": 0.37, "y": 0.47, "z": 0.10, "name": "left_middle"},
                {"x": 0.70, "y": 0.44, "z": -0.10, "name": "right_middle"},
                {"x": 0.38, "y": 0.48, "z": 0.10, "name": "left_ring"},
                {"x": 0.69, "y": 0.45, "z": -0.10, "name": "right_ring"},
                {"x": 0.39, "y": 0.49, "z": 0.10, "name": "left_pinky"},
                {"x": 0.68, "y": 0.46, "z": -0.10, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "dance": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2) - 身体倾斜
                {"x": 0.36, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.66, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 跳舞姿势
                {"x": 0.28, "y": 0.30, "z": 0.08, "name": "left_elbow"},
                {"x": 0.74, "y": 0.30, "z": -0.08, "name": "right_elbow"},
                {"x": 0.25, "y": 0.42, "z": 0.12, "name": "left_wrist"},
                {"x": 0.78, "y": 0.38, "z": -0.12, "name": "right_wrist"},
                {"x": 0.23, "y": 0.44, "z": 0.14, "name": "left_thumb"},
                {"x": 0.80, "y": 0.36, "z": -0.14, "name": "right_thumb"},
                {"x": 0.24, "y": 0.45, "z": 0.14, "name": "left_index"},
                {"x": 0.79, "y": 0.37, "z": -0.14, "name": "right_index"},
                {"x": 0.25, "y": 0.46, "z": 0.14, "name": "left_middle"},
                {"x": 0.78, "y": 0.38, "z": -0.14, "name": "right_middle"},
                {"x": 0.26, "y": 0.47, "z": 0.14, "name": "left_ring"},
                {"x": 0.77, "y": 0.39, "z": -0.14, "name": "right_ring"},
                {"x": 0.27, "y": 0.48, "z": 0.14, "name": "left_pinky"},
                {"x": 0.76, "y": 0.40, "z": -0.14, "name": "right_pinky"},
                # body (6) - 腿弯曲
                {"x": 0.40, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.62, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.38, "y": 0.68, "z": 0.0, "name": "left_knee"},
                {"x": 0.64, "y": 0.65, "z": 0.0, "name": "right_knee"},
                {"x": 0.40, "y": 0.88, "z": 0.0, "name": "left_ankle"},
                {"x": 0.62, "y": 0.85, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "stretch": {
            "keypoints": [
                # head (11)
                {"x": 0.50, "y": 0.10, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.09, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.09, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.09, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.09, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.09, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.09, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.11, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.11, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.14, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.14, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.22, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.22, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 双手向上伸展
                {"x": 0.30, "y": 0.15, "z": 0.08, "name": "left_elbow"},
                {"x": 0.72, "y": 0.15, "z": -0.08, "name": "right_elbow"},
                {"x": 0.28, "y": 0.08, "z": 0.10, "name": "left_wrist"},
                {"x": 0.74, "y": 0.08, "z": -0.10, "name": "right_wrist"},
                {"x": 0.26, "y": 0.06, "z": 0.12, "name": "left_thumb"},
                {"x": 0.76, "y": 0.06, "z": -0.12, "name": "right_thumb"},
                {"x": 0.27, "y": 0.07, "z": 0.12, "name": "left_index"},
                {"x": 0.75, "y": 0.07, "z": -0.12, "name": "right_index"},
                {"x": 0.28, "y": 0.08, "z": 0.12, "name": "left_middle"},
                {"x": 0.74, "y": 0.08, "z": -0.12, "name": "right_middle"},
                {"x": 0.29, "y": 0.09, "z": 0.12, "name": "left_ring"},
                {"x": 0.73, "y": 0.09, "z": -0.12, "name": "right_ring"},
                {"x": 0.30, "y": 0.10, "z": 0.12, "name": "left_pinky"},
                {"x": 0.72, "y": 0.10, "z": -0.12, "name": "right_pinky"},
                # body (6)
                {"x": 0.42, "y": 0.50, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.50, "z": 0.0, "name": "right_hip"},
                {"x": 0.42, "y": 0.70, "z": 0.0, "name": "left_knee"},
                {"x": 0.60, "y": 0.70, "z": 0.0, "name": "right_knee"},
                {"x": 0.42, "y": 0.90, "z": 0.0, "name": "left_ankle"},
                {"x": 0.60, "y": 0.90, "z": 0.0, "name": "right_ankle"},
            ]
        },
        "jump": {
            "keypoints": [
                # head (11) - 最高点
                {"x": 0.50, "y": 0.06, "z": 0.0, "name": "nose"},
                {"x": 0.47, "y": 0.05, "z": 0.0, "name": "left_eye_inner"},
                {"x": 0.45, "y": 0.05, "z": 0.0, "name": "left_eye"},
                {"x": 0.43, "y": 0.05, "z": 0.0, "name": "left_eye_outer"},
                {"x": 0.55, "y": 0.05, "z": 0.0, "name": "right_eye_inner"},
                {"x": 0.57, "y": 0.05, "z": 0.0, "name": "right_eye"},
                {"x": 0.59, "y": 0.05, "z": 0.0, "name": "right_eye_outer"},
                {"x": 0.40, "y": 0.07, "z": 0.0, "name": "left_ear"},
                {"x": 0.62, "y": 0.07, "z": 0.0, "name": "right_ear"},
                {"x": 0.46, "y": 0.10, "z": 0.0, "name": "mouth_left"},
                {"x": 0.56, "y": 0.10, "z": 0.0, "name": "mouth_right"},
                # shoulders (2)
                {"x": 0.38, "y": 0.15, "z": 0.0, "name": "left_shoulder"},
                {"x": 0.64, "y": 0.15, "z": 0.0, "name": "right_shoulder"},
                # arms (14) - 跳起时手臂向上
                {"x": 0.30, "y": 0.08, "z": 0.08, "name": "left_elbow"},
                {"x": 0.72, "y": 0.08, "z": -0.08, "name": "right_elbow"},
                {"x": 0.28, "y": 0.03, "z": 0.10, "name": "left_wrist"},
                {"x": 0.74, "y": 0.03, "z": -0.10, "name": "right_wrist"},
                {"x": 0.26, "y": 0.02, "z": 0.12, "name": "left_thumb"},
                {"x": 0.76, "y": 0.02, "z": -0.12, "name": "right_thumb"},
                {"x": 0.27, "y": 0.03, "z": 0.12, "name": "left_index"},
                {"x": 0.75, "y": 0.03, "z": -0.12, "name": "right_index"},
                {"x": 0.28, "y": 0.04, "z": 0.12, "name": "left_middle"},
                {"x": 0.74, "y": 0.04, "z": -0.12, "name": "right_middle"},
                {"x": 0.29, "y": 0.05, "z": 0.12, "name": "left_ring"},
                {"x": 0.73, "y": 0.05, "z": -0.12, "name": "right_ring"},
                {"x": 0.30, "y": 0.06, "z": 0.12, "name": "left_pinky"},
                {"x": 0.72, "y": 0.06, "z": -0.12, "name": "right_pinky"},
                # body (6) - 跳起时腿弯曲
                {"x": 0.42, "y": 0.40, "z": 0.0, "name": "left_hip"},
                {"x": 0.60, "y": 0.40, "z": 0.0, "name": "right_hip"},
                {"x": 0.40, "y": 0.55, "z": 0.0, "name": "left_knee"},
                {"x": 0.62, "y": 0.55, "z": 0.0, "name": "right_knee"},
                {"x": 0.40, "y": 0.70, "z": 0.0, "name": "left_ankle"},
                {"x": 0.62, "y": 0.70, "z": 0.0, "name": "right_ankle"},
            ]
        }
    }

    # 部位权重配置 - 每个动作的重点部位不同
    WEIGHT_CONFIGS = {
        "heart": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},    # 重点在手部比心
        "hands_up": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},  # 重点在举手
        "squat": {"head": 0.10, "shoulders": 0.10, "arms": 0.20, "body": 0.60},     # 重点在下蹲
        "tilt_head": {"head": 0.50, "shoulders": 0.15, "arms": 0.20, "body": 0.15}, # 重点在头部
        "wave": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},      # 重点在挥手
        "peace": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},   # 重点在V手势
        "thumb_up": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},# 重点在拇指
        "dance": {"head": 0.15, "shoulders": 0.20, "arms": 0.35, "body": 0.30},   # 全身协调
        "stretch": {"head": 0.10, "shoulders": 0.20, "arms": 0.40, "body": 0.30}, # 重点在伸展
        "jump": {"head": 0.10, "shoulders": 0.10, "arms": 0.30, "body": 0.50},      # 重点在跳跃
    }

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

    WEIGHT_CONFIGS = {
        "heart": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},
        "hands_up": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},
        "squat": {"head": 0.10, "shoulders": 0.10, "arms": 0.20, "body": 0.60},
        "tilt_head": {"head": 0.50, "shoulders": 0.15, "arms": 0.20, "body": 0.15},
        "wave": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},
        "peace": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},
        "thumb_up": {"head": 0.10, "shoulders": 0.15, "arms": 0.50, "body": 0.25},
        "dance": {"head": 0.15, "shoulders": 0.20, "arms": 0.35, "body": 0.30},
        "stretch": {"head": 0.10, "shoulders": 0.20, "arms": 0.40, "body": 0.30},
        "jump": {"head": 0.10, "shoulders": 0.10, "arms": 0.30, "body": 0.50},
    }

    for i, level_data in enumerate(levels, 1):
        pose_type = level_data["pose_type"]
        target_pose = TARGET_POSES.get(pose_type, TARGET_POSES["heart"])
        weight_config = WEIGHT_CONFIGS.get(pose_type)
        level = Level(
            id=i,
            name=level_data["name"],
            description=level_data["description"],
            pose_type=level_data["pose_type"],
            target_pose=target_pose,
            pass_score=level_data["pass_score"],
            sticker_reward=level_data["sticker_reward"],
            weight_config=weight_config
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