"""颜色识别核心模块。

对 ROI 区域的 BGR 图像进行 HSV 分析，区分酒红色、蓝紫色、纯蓝色，
输出匹配颜色、匹配度和 HSV 统计值。

对应需求 DET-05、DET-06。
"""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum

import cv2
import numpy as np

from config import ThresholdConfig


class ColorState(str, Enum):
    """匹配颜色。"""
    RED = "RED"       # 酒红色 / 初始色
    PURPLE = "PURPLE"  # 蓝紫色 / 过渡色
    BLUE = "BLUE"      # 纯蓝色 / 终点色
    UNKNOWN = "UNKNOWN"  # 未匹配


@dataclass
class ColorResult:
    """单帧颜色识别结果。"""
    color: ColorState
    confidence: float        # 匹配度 0~100
    hue: float               # 平均色相 0~180 (OpenCV)
    saturation: float        # 平均饱和度 0~255
    brightness: float        # 平均明度 0~255
    red_ratio: float         # 酒红色像素占比 0~1
    purple_ratio: float      # 蓝紫色像素占比 0~1
    blue_ratio: float        # 纯蓝色像素占比 0~1

    @property
    def hue_degree(self) -> float:
        """色相转 0~360 度（便于展示）。"""
        return self.hue * 2.0


class ColorClassifier:
    """基于 HSV 阈值的滴定颜色分类器。"""

    def __init__(self, threshold: ThresholdConfig | None = None):
        self.threshold = threshold or ThresholdConfig()

    def classify(self, bgr: np.ndarray) -> ColorResult:
        """对单帧 BGR 图像（ROI 区域）分类。"""
        if bgr is None or bgr.size == 0:
            return self._empty_result()

        hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
        h, s, v = hsv[:, :, 0].astype(np.float32), \
                  hsv[:, :, 1].astype(np.float32), \
                  hsv[:, :, 2].astype(np.float32)

        # 饱和度/明度掩码（过滤灰白噪声）
        s_min = self.threshold.min_saturation * 255
        v_min = self.threshold.min_brightness * 255
        valid = (s >= s_min) & (v >= v_min)

        red_mask, purple_mask, blue_mask = self._build_masks(h, valid)

        total = valid.sum()
        if total == 0:
            return self._empty_result()

        red_ratio = float(red_mask.sum() / total)
        purple_ratio = float(purple_mask.sum() / total)
        blue_ratio = float(blue_mask.sum() / total)

        # 决定主颜色与匹配度
        ratios = {
            ColorState.RED: red_ratio,
            ColorState.PURPLE: purple_ratio,
            ColorState.BLUE: blue_ratio,
        }
        dominant = max(ratios, key=ratios.get)
        dominant_ratio = ratios[dominant]

        # 匹配度 = 主颜色占比 * 100；占比过低判为 UNKNOWN
        if dominant_ratio < 0.30:
            return ColorResult(
                color=ColorState.UNKNOWN,
                confidence=round(dominant_ratio * 100, 2),
                hue=float(h[valid].mean()),
                saturation=float(s[valid].mean()),
                brightness=float(v[valid].mean()),
                red_ratio=red_ratio, purple_ratio=purple_ratio, blue_ratio=blue_ratio,
            )

        return ColorResult(
            color=dominant,
            confidence=round(dominant_ratio * 100, 2),
            hue=float(h[valid].mean()),
            saturation=float(s[valid].mean()),
            brightness=float(v[valid].mean()),
            red_ratio=red_ratio, purple_ratio=purple_ratio, blue_ratio=blue_ratio,
        )

    def _build_masks(self, h: np.ndarray, valid: np.ndarray):
        """构建三种颜色的布尔掩码。色相 H 为 0~180，需换算阈值。"""
        t = self.threshold

        # 阈值从 0~360 度换算到 0~180
        red_min, red_max = t.red_h_min / 2.0, t.red_h_max / 2.0
        purple_min, purple_max = t.purple_h_min / 2.0, t.purple_h_max / 2.0
        blue_min, blue_max = t.blue_h_min / 2.0, t.blue_h_max / 2.0

        # 红色跨越 0 度，分两段 [330,360] ∪ [0,25]
        red_mask = ((h >= red_min) | (h <= red_max)) & valid

        purple_mask = (h >= purple_min) & (h <= purple_max) & valid
        blue_mask = (h >= blue_min) & (h <= blue_max) & valid

        return red_mask, purple_mask, blue_mask

    @staticmethod
    def _empty_result() -> ColorResult:
        return ColorResult(
            color=ColorState.UNKNOWN, confidence=0.0,
            hue=0.0, saturation=0.0, brightness=0.0,
            red_ratio=0.0, purple_ratio=0.0, blue_ratio=0.0,
        )
