"""颜色分类器单元测试。

用合成的纯色图片验证酒红/蓝紫/纯蓝分类。不依赖摄像头。
"""

from __future__ import annotations

import numpy as np
import pytest

from vision.color_classifier import ColorClassifier, ColorState


def make_bgr(h_degree: int, s: float = 0.8, v: float = 0.8):
    """按色相生成 BGR 纯色图。h_degree 为 0~360。"""
    h = h_degree / 2.0
    hsv = np.full((50, 50, 3), [h, s * 255, v * 255], dtype=np.uint8)
    import cv2
    return cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)


@pytest.fixture
def classifier():
    return ColorClassifier()


def test_blue_detected(classifier):
    """纯蓝色（H=210）应识别为 BLUE。"""
    bgr = make_bgr(210)
    result = classifier.classify(bgr)
    assert result.color == ColorState.BLUE
    assert result.confidence > 90


def test_red_detected(classifier):
    """酒红色（H=340，跨越 0 度）应识别为 RED。"""
    bgr = make_bgr(340)
    result = classifier.classify(bgr)
    assert result.color == ColorState.RED


def test_purple_detected(classifier):
    """蓝紫色（H=260）应识别为 PURPLE。"""
    bgr = make_bgr(260)
    result = classifier.classify(bgr)
    assert result.color == ColorState.PURPLE


def test_gray_is_unknown(classifier):
    """低饱和灰色应识别为 UNKNOWN。"""
    gray = np.full((50, 50, 3), 128, dtype=np.uint8)
    result = classifier.classify(gray)
    assert result.color == ColorState.UNKNOWN
