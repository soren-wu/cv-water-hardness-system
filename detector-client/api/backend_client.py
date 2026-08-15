"""Java 后端 API 客户端。

封装登录、获取任务、获取阈值模板、上传实验结果的 HTTP 调用。

对应需求 DET-01、DET-10、DET-12。
"""

from __future__ import annotations

from dataclasses import dataclass

import httpx


@dataclass
class UserInfo:
    user_id: int
    username: str
    real_name: str
    role: str
    token: str


class BackendClient:
    """后端 API 客户端。"""

    def __init__(self, base_url: str = "http://localhost:8080", timeout: float = 10.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self._token: str | None = None
        self._client = httpx.Client(timeout=timeout)

    @property
    def is_logged_in(self) -> bool:
        return self._token is not None

    def login(self, username: str, password: str) -> UserInfo:
        """登录，成功后保存 token。"""
        resp = self._client.post(
            f"{self.base_url}/api/auth/login",
            json={"username": username, "password": password},
        )
        resp.raise_for_status()
        body = resp.json()
        if body.get("code") != 200:
            raise RuntimeError(body.get("message", "登录失败"))

        data = body["data"]
        self._token = data["token"]
        return UserInfo(
            user_id=data["userId"],
            username=data["username"],
            real_name=data["realName"],
            role=data["role"],
            token=data["token"],
        )

    def _headers(self) -> dict:
        return {"Authorization": f"Bearer {self._token}"} if self._token else {}

    def get_tasks(self) -> list[dict]:
        """获取已发布的实验任务列表。"""
        resp = self._client.get(f"{self.base_url}/api/tasks", headers=self._headers())
        resp.raise_for_status()
        body = resp.json()
        if body.get("code") != 200:
            raise RuntimeError(body.get("message", "获取任务失败"))
        return body["data"].get("records", [])

    def get_default_threshold(self) -> dict | None:
        """获取默认 HSV 阈值模板。"""
        resp = self._client.get(
            f"{self.base_url}/api/threshold-templates/default", headers=self._headers()
        )
        if resp.status_code == 404:
            return None
        resp.raise_for_status()
        body = resp.json()
        return body.get("data")

    def submit_experiment(self, payload: dict) -> dict:
        """提交实验结果到 /api/experiments。"""
        resp = self._client.post(
            f"{self.base_url}/api/experiments", json=payload, headers=self._headers()
        )
        resp.raise_for_status()
        body = resp.json()
        if body.get("code") != 200:
            raise RuntimeError(body.get("message", "提交失败"))
        return body["data"]

    def upload_file(self, experiment_id: int, file_path: str, file_type: str = "KEYFRAME") -> dict:
        """上传关键帧/文件到 /api/files/upload。"""
        with open(file_path, "rb") as f:
            resp = self._client.post(
                f"{self.base_url}/api/files/upload",
                data={"experimentId": str(experiment_id), "fileType": file_type},
                files={"file": (file_path.split("/")[-1], f)},
                headers=self._headers(),
            )
        resp.raise_for_status()
        body = resp.json()
        if body.get("code") != 200:
            raise RuntimeError(body.get("message", "上传失败"))
        return body["data"]

    def close(self) -> None:
        self._client.close()
