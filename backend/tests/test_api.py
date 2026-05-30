import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture(scope="module")
def client():
    from fastapi.testclient import TestClient
    from app.main import app
    return TestClient(app)


def test_root(client):
    response = client.get("/")
    assert response.status_code == 200
    assert "message" in response.json()


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_register(client):
    response = client.post("/api/user/register", json={"nickname": "testuser", "password": "123456"})
    assert response.status_code == 200
    data = response.json()
    assert data["code"] == 200
    assert "token" in data["data"]


def test_login(client):
    client.post("/api/user/register", json={"nickname": "logintest", "password": "123456"})
    response = client.post("/api/user/login", json={"nickname": "logintest", "password": "123456"})
    assert response.status_code == 200
    data = response.json()
    assert data["code"] == 200
    assert data["msg"] == "登录成功"


def test_get_levels(client):
    response = client.get("/api/levels")
    assert response.status_code == 200
    data = response.json()
    assert "list" in data["data"]
    assert len(data["data"]["list"]) >= 10


def test_pose_types(client):
    response = client.get("/api/levels/pose/types")
    assert response.status_code == 200
    data = response.json()
    assert len(data["data"]["list"]) == 10


def test_leaderboard(client):
    response = client.get("/api/game/leaderboard/1")
    assert response.status_code == 200
    data = response.json()
    assert "ranking" in data["data"]


def test_get_all_stickers(client):
    response = client.get("/api/stickers/all")
    assert response.status_code == 200
    data = response.json()
    assert "list" in data["data"]