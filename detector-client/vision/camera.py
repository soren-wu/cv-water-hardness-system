"""摄像头采集模块。

封装 OpenCV VideoCapture，支持 USB 摄像头和内置摄像头，线程安全读取。

对应需求 DET-02、DET-03。
"""

from __future__ import annotations

import threading
import time

import cv2
import numpy as np


class Camera:
    """线程安全的摄像头封装，后台线程持续读取，避免阻塞 UI。"""

    def __init__(self, index: int = 0, width: int = 1280, height: int = 720):
        self.index = index
        self.width = width
        self.height = height
        self._cap: cv2.VideoCapture | None = None
        self._frame: np.ndarray | None = None
        self._running = False
        self._lock = threading.Lock()
        self._thread: threading.Thread | None = None

    def open(self) -> bool:
        """打开摄像头。"""
        self._cap = cv2.VideoCapture(self.index)
        self._cap.set(cv2.CAP_PROP_FRAME_WIDTH, self.width)
        self._cap.set(cv2.CAP_PROP_FRAME_HEIGHT, self.height)
        if not self._cap.isOpened():
            self._cap = None
            return False
        self._running = True
        self._thread = threading.Thread(target=self._loop, daemon=True)
        self._thread.start()
        return True

    def _loop(self) -> None:
        while self._running and self._cap is not None:
            ok, frame = self._cap.read()
            if ok:
                with self._lock:
                    self._frame = frame
            else:
                time.sleep(0.01)

    def read(self) -> np.ndarray | None:
        """读取最新一帧。"""
        with self._lock:
            return None if self._frame is None else self._frame.copy()

    def close(self) -> None:
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=1.0)
        if self._cap is not None:
            self._cap.release()
            self._cap = None

    @property
    def is_opened(self) -> bool:
        return self._cap is not None and self._cap.isOpened()


def list_cameras(max_index: int = 5) -> list[int]:
    """枚举可用摄像头索引。"""
    available = []
    for i in range(max_index):
        cap = cv2.VideoCapture(i)
        if cap.isOpened():
            available.append(i)
        cap.release()
    return available
