"""图像预处理模块。

对 ROI 区域做高斯滤波去噪，供颜色识别使用。

对应需求 DET-05。
"""

from __future__ import annotations

import cv2
import numpy as np


def denoise(bgr: np.ndarray, kernel_size: int = 5) -> np.ndarray:
    """高斯滤波去噪。kernel_size 必须为奇数。"""
    if kernel_size % 2 == 0:
        kernel_size += 1
    return cv2.GaussianBlur(bgr, (kernel_size, kernel_size), 0)


def resize_to(bgr: np.ndarray, max_width: int = 640) -> np.ndarray:
    """等比例缩放到最大宽度，降低计算量。"""
    h, w = bgr.shape[:2]
    if w <= max_width:
        return bgr
    scale = max_width / w
    return cv2.resize(bgr, (max_width, int(h * scale)))


def crop_roi(bgr: np.ndarray, roi: tuple[int, int, int, int]) -> np.ndarray:
    """按 (x, y, w, h) 裁剪 ROI 区域。"""
    x, y, w, h = roi
    if w <= 0 or h <= 0:
        return bgr
    hh, ww = bgr.shape[:2]
    x, y = max(0, x), max(0, y)
    w, h = min(w, ww - x), min(h, hh - y)
    return bgr[y:y + h, x:x + w]
