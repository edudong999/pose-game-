from fastapi import APIRouter, Depends, File, UploadFile, HTTPException, Request, Query, Body
from sqlalchemy.orm import Session
from typing import List, Union
from pydantic import BaseModel
import json

from ..database import get_db
from ..models import Level
from ..schemas import Keypoint, TargetPose

router = APIRouter(prefix="/api/pose", tags=["pose"])

# MediaPipe Pose keypoint names (33 landmarks from MediaPipe)
KEYPOINT_NAMES = [
    "nose", "left_eye_inner", "left_eye", "left_eye_outer",
    "right_eye_inner", "right_eye", "right_eye_outer",
    "left_ear", "right_ear", "mouth_left", "mouth_right",
    "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
    "left_wrist", "right_wrist", "left_thumb", "right_thumb",
    "left_index", "right_index", "left_middle", "right_middle",
    "left_ring", "right_ring", "left_pinky", "right_pinky",
    "left_hip", "right_hip", "left_knee", "right_knee",
    "left_ankle", "right_ankle"
]

# Body part groups for detailed scoring (using all 33 keypoints).
# 元素若是 list[list] 视为子部位组：先算每个子部位分，再等权平均
# （不让数量多但语义弱的点稀释语义重的点，例如 arms 的 10 个手指
# 不能压过 4 个肘/腕）。
BODY_PARTS = {
    "head": ["nose", "left_eye_inner", "left_eye", "left_eye_outer",
             "right_eye_inner", "right_eye", "right_eye_outer",
             "left_ear", "right_ear", "mouth_left", "mouth_right"],
    "shoulders": ["left_shoulder", "right_shoulder"],
    "arms": [
        # forearm：肘 + 腕，"胳膊伸哪"的主体
        ["left_elbow", "right_elbow", "left_wrist", "right_wrist"],
        # hand：5×2 手指，决定手型（开掌/握拳/指向前方）
        ["left_thumb", "right_thumb", "left_index", "right_index",
         "left_middle", "right_middle", "left_ring", "right_ring",
         "left_pinky", "right_pinky"],
    ],
    "body": ["left_hip", "right_hip", "left_knee", "right_knee",
             "left_ankle", "right_ankle"]
}


def extract_keypoints(data: Union[List[dict], dict]) -> List[dict]:
    """
    Extract keypoints from request data.
    Accepts both formats:
    - List[dict]: [{"name": "nose", "x": 0.5, ...}, ...]
    - Dict: {"keypoints": [{"name": "nose", "x": 0.5, ...}, ...]}
    """
    if isinstance(data, list):
        return data
    elif isinstance(data, dict):
        return data.get("keypoints", [])
    return []


def _body_scale(kpts: list) -> float:
    """身体尺度（用于 z 归一化）。
    MediaPipe 的 z 是相对髋中心的深度，量级随相机距离/人离相机的远近变化。
    用肩宽（或髋宽）作为参考，把 z 变成"以身体宽度为单位的相对深度"，
    与 x, y (0-1 归一化) 同一量级，避免深一寸就被算法当成大偏离。
    肩/髋都拿不到时兜底用 0.3（接近标准肩宽归一化值），防除零。
    """
    by_name = {k.get("name"): k for k in kpts if isinstance(k, dict)}
    for a_name, b_name in (("left_shoulder", "right_shoulder"),
                            ("left_hip", "right_hip")):
        a = by_name.get(a_name)
        b = by_name.get(b_name)
        if a and b:
            dx = a.get("x", 0) - b.get("x", 0)
            dy = a.get("y", 0) - b.get("y", 0)
            d = (dx**2 + dy**2) ** 0.5
            if d > 0.01:
                return d
    return 0.3


def calculate_pose_similarity(detected: dict, target: dict, weight_config: dict = None) -> tuple:
    """
    Calculate similarity between detected pose and target pose.
    Returns (total_score, head_score, shoulders_score, arms_score, body_score)

    weight_config: optional dict with custom weights, e.g.:
        {"head": 0.15, "shoulders": 0.20, "arms": 0.35, "body": 0.30}
    If not provided, uses default weights.
    """
    if not target or not detected:
        return 50, 50, 50, 50, 50

    det_kpts = detected.get("keypoints", [])
    tgt_kpts = target.get("keypoints", []) if isinstance(target, dict) else []

    if not det_kpts or not tgt_kpts:
        return 50, 50, 50, 50, 50

    # Build target keypoint lookup
    tgt_lookup = {kp.get("name"): kp for kp in tgt_kpts if isinstance(kp, dict)}

    # 各自的身体尺度，z 除以这个值做归一化
    det_scale = _body_scale(det_kpts)
    tgt_scale = _body_scale(tgt_kpts)

    def keypoint_distance(name: str, det_kpts: list, tgt_lookup: dict) -> float:
        det_kp = next((k for k in det_kpts if k.get("name") == name), None)
        tgt_kp = tgt_lookup.get(name)
        if not det_kp or not tgt_kp:
            return 1.0  # Max distance (worst)
        dx = det_kp.get("x", 0) - tgt_kp.get("x", 0)
        dy = det_kp.get("y", 0) - tgt_kp.get("y", 0)
        # z 归一化：除以各自身体尺度（det_scale / tgt_scale），
        # 让深度差异按"身体宽度"衡量，不再被相机距离放大。
        dz = det_kp.get("z", 0) / det_scale - tgt_kp.get("z", 0) / tgt_scale
        return (dx**2 + dy**2 + dz**2) ** 0.5

    def part_score(part_names: list, det_kpts: list, tgt_lookup: dict) -> int:
        # 子部位：先算每个子部位分，再对子部位等权平均。
        # 例如 arms = [forearm, hand]，前臂分和手型分各占 50%，
        # 避免 10 个手指的"分高"把前臂"分低"拉成及格。
        if part_names and isinstance(part_names[0], list):
            sub_scores = [part_score(sub, det_kpts, tgt_lookup) for sub in part_names]
            return int(sum(sub_scores) / len(sub_scores)) if sub_scores else 50

        distances = [keypoint_distance(name, det_kpts, tgt_lookup) for name in part_names]
        # Convert distance to similarity (0-1 range)
        # For 3D distance, max possible is sqrt(3) ≈ 1.73, so use factor 1.5
        similarities = [max(0, 1 - d * 1.5) for d in distances]
        return int(sum(similarities) / len(similarities) * 100) if similarities else 50

    head_score = part_score(BODY_PARTS["head"], det_kpts, tgt_lookup)
    shoulders_score = part_score(BODY_PARTS["shoulders"], det_kpts, tgt_lookup)
    arms_score = part_score(BODY_PARTS["arms"], det_kpts, tgt_lookup)
    body_score = part_score(BODY_PARTS["body"], det_kpts, tgt_lookup)

    # Use custom weights if provided, otherwise use defaults
    if weight_config:
        head_w = weight_config.get("head", 0.15)
        shoulders_w = weight_config.get("shoulders", 0.20)
        arms_w = weight_config.get("arms", 0.35)
        body_w = weight_config.get("body", 0.30)
    else:
        head_w, shoulders_w, arms_w, body_w = 0.15, 0.20, 0.35, 0.30

    # Ensure weights sum to 1
    total_weight = head_w + shoulders_w + arms_w + body_w
    if total_weight > 0:
        head_w /= total_weight
        shoulders_w /= total_weight
        arms_w /= total_weight
        body_w /= total_weight

    # Weighted average
    total_score = int(head_score * head_w + shoulders_score * shoulders_w + arms_score * arms_w + body_score * body_w)

    return total_score, head_score, shoulders_score, arms_score, body_score


def parse_target_pose(target_pose_data) -> dict:
    """Parse target pose from database format"""
    if isinstance(target_pose_data, dict):
        return target_pose_data
    elif isinstance(target_pose_data, str):
        try:
            return json.loads(target_pose_data)
        except:
            return None
    return None


class PoseRecognizeRequest(BaseModel):
    keypoints: List[dict]


@router.post("/recognize")
async def recognize_pose(
    level_id: int = Query(...),
    request_data: PoseRecognizeRequest = Body(...),
    db: Session = Depends(get_db)
):
    """
    Recognize pose from keypoints.
    Accepts JSON body with keypoints array.
    """
    level = db.query(Level).filter(Level.id == level_id).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    # Parse target pose
    target_pose = parse_target_pose(level.target_pose)

    keypoints = request_data.keypoints

    if not keypoints:
        raise HTTPException(status_code=400, detail="关键点数据为空")

    detected_pose = {"keypoints": keypoints}

    if target_pose is None:
        return {
            "code": 200,
            "msg": "识别成功",
            "data": {
                "score": 70,
                "isPass": False,
                "detectedPose": detected_pose,
                "targetPose": None,
                "matchDetails": {"head": 70, "shoulders": 70, "arms": 70, "body": 70},
                "stickerReward": None
            }
        }

    # Calculate similarity scores
    total_score, head_score, shoulders_score, arms_score, body_score = calculate_pose_similarity(
        detected_pose, target_pose, level.weight_config
    )

    is_pass = total_score >= level.pass_score

    return {
        "code": 200,
        "msg": "识别成功",
        "data": {
            "score": total_score,
            "isPass": is_pass,
            "detectedPose": detected_pose,
            "targetPose": target_pose,
            "matchDetails": {
                "head": head_score,
                "shoulders": shoulders_score,
                "arms": arms_score,
                "body": body_score
            },
            "stickerReward": level.sticker_reward if is_pass else None
        }
    }


@router.post("/analyze/realtime")
async def analyze_realtime_pose(
    level_id: int = Query(...),
    request: Request = None,
    db: Session = Depends(get_db)
):
    """
    Real-time pose analysis endpoint.
    Receives keypoint coordinates from Android app and returns similarity scores.
    """
    level = db.query(Level).filter(Level.id == level_id).first()
    if not level:
        raise HTTPException(status_code=404, detail="关卡不存在")

    # Read body directly
    body = await request.json()

    # Android app sends {"keypoints": [...]} - extract keypoints
    keypoints = body.get("keypoints", []) if isinstance(body, dict) else []

    if not keypoints:
        raise HTTPException(status_code=400, detail="关键点数据为空")

    target_pose = parse_target_pose(level.target_pose)
    detected_pose = {"keypoints": keypoints}

    total_score, head_score, shoulders_score, arms_score, body_score = calculate_pose_similarity(
        detected_pose, target_pose, level.weight_config
    )

    return {
        "code": 200,
        "msg": "分析成功",
        "data": {
            "score": total_score,
            "head_score": head_score,
            "shoulder_score": shoulders_score,
            "arm_score": arms_score,
            "body_score": body_score,
            "isPass": total_score >= level.pass_score
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