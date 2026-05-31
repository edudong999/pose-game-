from fastapi import APIRouter, Depends, Header

router = APIRouter(prefix="/api/stickers", tags=["stickers"])


@router.get("", response_model=dict)
def get_my_stickers(authorization: str = Header(None)):
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": [
                {"id": 1, "name": "爱心贴纸", "imageUrl": "https://xxx/sticker_heart.png", "unlockLevel": 1},
                {"id": 2, "name": "星星贴纸", "imageUrl": "https://xxx/sticker_star.png", "unlockLevel": 2},
                {"id": 3, "name": "彩虹贴纸", "imageUrl": "https://xxx/sticker_rainbow.png", "unlockLevel": 3}
            ],
            "total": 3
        }
    }


@router.get("/all", response_model=dict)
def get_all_stickers():
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "list": [
                {"id": 1, "name": "爱心贴纸", "imageUrl": "https://xxx/sticker_heart.png", "unlockLevel": 1, "isUnlocked": True},
                {"id": 2, "name": "星星贴纸", "imageUrl": "https://xxx/sticker_star.png", "unlockLevel": 2, "isUnlocked": True},
                {"id": 3, "name": "皇冠贴纸", "imageUrl": "https://xxx/sticker_crown.png", "unlockLevel": 5, "isUnlocked": False},
                {"id": 4, "name": "彩虹贴纸", "imageUrl": "https://xxx/sticker_rainbow.png", "unlockLevel": 3, "isUnlocked": True},
                {"id": 5, "name": "火焰贴纸", "imageUrl": "https://xxx/sticker_fire.png", "unlockLevel": 4, "isUnlocked": False},
                {"id": 6, "name": "钻石贴纸", "imageUrl": "https://xxx/sticker_diamond.png", "unlockLevel": 6, "isUnlocked": False},
                {"id": 7, "name": "月亮贴纸", "imageUrl": "https://xxx/sticker_moon.png", "unlockLevel": 7, "isUnlocked": False},
                {"id": 8, "name": "太阳贴纸", "imageUrl": "https://xxx/sticker_sun.png", "unlockLevel": 8, "isUnlocked": False},
                {"id": 9, "name": "花朵贴纸", "imageUrl": "https://xxx/sticker_flower.png", "unlockLevel": 9, "isUnlocked": False},
                {"id": 10, "name": "金星贴纸", "imageUrl": "https://xxx/sticker_goldstar.png", "unlockLevel": 10, "isUnlocked": False}
            ]
        }
    }