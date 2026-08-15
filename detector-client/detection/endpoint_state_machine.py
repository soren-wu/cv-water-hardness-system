"""滴定终点状态机。

根据逐帧颜色识别结果，维护状态流转，处理纯蓝色 30 秒稳定性验证。
候选终点期间若返色，取消计时。

对应需求 DET-07。
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from enum import Enum

from config import ThresholdConfig
from vision.color_classifier import ColorResult, ColorState


class DetectorState(str, Enum):
    """检测端状态（对应需求 3.3 状态机）。"""
    IDLE = "IDLE"                       # 待机
    READY = "READY"                     # 采集准备
    INITIAL = "INITIAL"                 # 初始色（酒红）
    TITRATING = "TITRATING"             # 滴定中
    NEAR_ENDPOINT = "NEAR_ENDPOINT"     # 临近终点（紫蓝）
    CANDIDATE_ENDPOINT = "CANDIDATE_ENDPOINT"  # 候选终点（纯蓝，计时中）
    ENDPOINT = "ENDPOINT"               # 有效终点（稳定达标）
    PAUSED = "PAUSED"                   # 暂停
    ERROR = "ERROR"                     # 异常


@dataclass
class StateEvent:
    """状态切换事件（对应 state_events 表）。"""
    event_type: str
    message: str
    timestamp: float = field(default_factory=time.time)


class EndpointStateMachine:
    """终点判定状态机。"""

    def __init__(self, threshold: ThresholdConfig | None = None):
        self.threshold = threshold or ThresholdConfig()
        self.state = DetectorState.IDLE
        self._candidate_start: float | None = None
        self.events: list[StateEvent] = []
        self.endpoint_time: float | None = None

    @property
    def stable_duration(self) -> float:
        """当前候选终点已稳定的秒数。"""
        if self._candidate_start is None:
            return 0.0
        return time.time() - self._candidate_start

    @property
    def required_duration(self) -> int:
        return self.threshold.stable_duration_seconds

    def reset(self) -> None:
        """重置到待机状态。"""
        self.state = DetectorState.IDLE
        self._candidate_start = None
        self.endpoint_time = None
        self.events.clear()

    def start(self) -> None:
        """开始采集，进入采集准备状态。"""
        self._transition(DetectorState.READY, "开始采集，等待框选 ROI")

    def pause(self) -> None:
        self._transition(DetectorState.PAUSED, "暂停检测")

    def resume(self) -> None:
        self._transition(DetectorState.READY, "恢复检测")

    def error(self, message: str) -> None:
        self._transition(DetectorState.ERROR, message)

    def feed(self, result: ColorResult) -> bool:
        """输入一帧颜色结果，返回 True 表示本轮刚确认有效终点。"""
        # 终点已确认后不再重复判定
        if self.state in (DetectorState.ENDPOINT, DetectorState.PAUSED, DetectorState.ERROR):
            return False

        color = result.color

        # 颜色异常（UNKNOWN）不推进状态
        if color is ColorState.UNKNOWN:
            return False

        if self.state is DetectorState.IDLE:
            self._transition(DetectorState.READY, "检测开始")

        # 纯蓝色 → 进入/维持候选终点
        if color is ColorState.BLUE:
            if self.state is not DetectorState.CANDIDATE_ENDPOINT:
                self._candidate_start = time.time()
                self._transition(
                    DetectorState.CANDIDATE_ENDPOINT,
                    f"检测到纯蓝色，开始稳定计时（目标 {self.required_duration}s）",
                )
            else:
                # 已达稳定时长 → 确认终点
                if self.stable_duration >= self.required_duration:
                    self.endpoint_time = time.time()
                    self._transition(DetectorState.ENDPOINT, "纯蓝色稳定达标，确认滴定终点")
                    return True
            return False

        # 非纯蓝色 → 取消候选计时，回到对应颜色状态
        if self.state is DetectorState.CANDIDATE_ENDPOINT:
            self._candidate_start = None
            self._transition(color_state(color), "出现返色，取消稳定计时")

        # 其他颜色状态直接映射
        target = color_state(color)
        if self.state is not target:
            self._transition(target, f"颜色状态 → {target}")

        return False

    def _transition(self, new_state: DetectorState, message: str) -> None:
        if new_state is self.state:
            return
        self.state = new_state
        self.events.append(StateEvent(event_type="STATE_CHANGE", message=message))


def color_state(color: ColorState) -> DetectorState:
    """颜色 → 状态机状态映射。"""
    return {
        ColorState.RED: DetectorState.INITIAL,
        ColorState.PURPLE: DetectorState.NEAR_ENDPOINT,
        ColorState.BLUE: DetectorState.CANDIDATE_ENDPOINT,
        ColorState.UNKNOWN: DetectorState.TITRATING,
    }[color]
