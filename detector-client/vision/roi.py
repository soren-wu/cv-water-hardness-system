"""ROI 区域框选模型。

维护框选区域坐标 (x, y, w, h)，支持在 UI 上拖动、缩放。

对应需求 DET-04。
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ROI:
    """ROI 区域，坐标为像素值 (x, y, w, h)。"""
    x: int = 0
    y: int = 0
    w: int = 0
    h: int = 0

    @property
    def is_valid(self) -> bool:
        return self.w > 0 and self.h > 0

    def to_tuple(self) -> tuple[int, int, int, int]:
        return (self.x, self.y, self.w, self.h)

    def clamp(self, frame_width: int, frame_height: int) -> None:
        """将 ROI 限制在画面范围内。"""
        self.x = max(0, min(self.x, frame_width))
        self.y = max(0, min(self.y, frame_height))
        self.w = max(0, min(self.w, frame_width - self.x))
        self.h = max(0, min(self.h, frame_height - self.y))


def default_center_roi(frame_width: int, frame_height: int, ratio: float = 0.5) -> ROI:
    """默认取画面中心 ratio 比例区域作为 ROI。"""
    w = int(frame_width * ratio)
    h = int(frame_height * ratio)
    x = (frame_width - w) // 2
    y = (frame_height - h) // 2
    return ROI(x, y, w, h)
