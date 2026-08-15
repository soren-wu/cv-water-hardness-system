"""终点状态机单元测试。

验证纯蓝色 30 秒稳定判定的状态流转和返色处理。
"""

from __future__ import annotations

import pytest

from detection.endpoint_state_machine import EndpointStateMachine, DetectorState
from vision.color_classifier import ColorResult, ColorState


def make_result(color: ColorState) -> ColorResult:
    return ColorResult(color=color, confidence=95.0, hue=105.0,
                       saturation=200.0, brightness=180.0,
                       red_ratio=0.0, purple_ratio=0.0, blue_ratio=0.95)


@pytest.fixture
def sm():
    machine = EndpointStateMachine()
    machine.start()
    return machine


def test_initial_flow(sm):
    """红色 → 紫 → 蓝 的正常流转。"""
    sm.feed(make_result(ColorState.RED))
    assert sm.state == DetectorState.INITIAL

    sm.feed(make_result(ColorState.PURPLE))
    assert sm.state == DetectorState.NEAR_ENDPOINT

    sm.feed(make_result(ColorState.BLUE))
    assert sm.state == DetectorState.CANDIDATE_ENDPOINT


def test_stable_endpoint(sm):
    """蓝色稳定达到阈值后确认终点。"""
    sm.feed(make_result(ColorState.BLUE))
    assert sm.state == DetectorState.CANDIDATE_ENDPOINT

    # 手动推进候选起点到过去
    sm._candidate_start = __import__("time").time() - sm.required_duration - 1
    is_endpoint = sm.feed(make_result(ColorState.BLUE))
    assert is_endpoint is True
    assert sm.state == DetectorState.ENDPOINT


def test_color_revert_cancels_timer(sm):
    """候选终点返色时取消计时。"""
    sm.feed(make_result(ColorState.BLUE))
    assert sm.state == DetectorState.CANDIDATE_ENDPOINT

    sm.feed(make_result(ColorState.RED))
    assert sm.state == DetectorState.INITIAL
    assert sm._candidate_start is None
