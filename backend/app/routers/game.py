from fastapi import APIRouter, Depends, Header

router = APIRouter(prefix="/api/game", tags=["game"])


@router.post("/record", response_model=dict)
def submit_record(record, authorization: str = Header(None)):
    return {
        "code": 200,
        "msg": "记录成功",
        "data": {
            "recordId": 1,
            "score": record.score if hasattr(record, 'score') else 85,
            "isPass": record.isPass if hasattr(record, 'isPass') else True,
            "stickerUnlocked": "sticker_001",
            "newStickers": []
        }
    }


@router.get("/records", response_model=dict)
def get_records(page: int = 1, pageSize: int = 10, authorization: str = Header(None)):
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": [
                {"id": 1, "levelId": 1, "levelName": "比心挑战", "score": 85, "isPass": True, "createdAt": "2026-05-30T10:00:00"},
                {"id": 2, "levelId": 2, "levelName": "举手欢呼", "score": 72, "isPass": True, "createdAt": "2026-05-29T15:30:00"},
                {"id": 3, "levelId": 3, "levelName": "深蹲挑战", "score": 60, "isPass": False, "createdAt": "2026-05-28T20:15:00"}
            ],
            "total": 3,
            "page": page,
            "pageSize": pageSize
        }
    }


@router.get("/leaderboard/{level_id}", response_model=dict)
def get_leaderboard(level_id: int):
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "levelId": level_id,
            "levelName": "测试关卡",
            "ranking": [
                {"rank": 1, "nickname": "用户A", "score": 98, "avatarUrl": None},
                {"rank": 2, "nickname": "用户B", "score": 92, "avatarUrl": None},
                {"rank": 3, "nickname": "测试用户", "score": 85, "avatarUrl": None},
                {"rank": 4, "nickname": "用户C", "score": 78, "avatarUrl": None},
                {"rank": 5, "nickname": "用户D", "score": 65, "avatarUrl": None}
            ]
        }
    }