"""视频预览标签，支持鼠标框选 ROI。

在 QLabel 上显示 OpenCV 帧，支持拖动框选 ROI 区域。

对应需求 DET-03、DET-04。
"""

from __future__ import annotations

import cv2
import numpy as np
from PySide6.QtCore import Qt, Signal
from PySide6.QtGui import QImage, QPixmap, QPainter, QPen, QColor
from PySide6.QtWidgets import QLabel

from vision.roi import ROI


class VideoLabel(QLabel):
    """显示视频帧并支持 ROI 框选。"""

    roi_changed = Signal(object)  # 发出 ROI 对象

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setMinimumSize(640, 480)
        self.setAlignment(Qt.AlignCenter)
        self.setStyleSheet("background:#222;")
        self._frame: np.ndarray | None = None
        self._display: QPixmap | None = None
        self._roi = ROI()
        self._roi_enabled = True
        self._dragging = False
        self._drag_start = (0, 0)

    def set_frame(self, bgr: np.ndarray | None):
        """设置要显示的帧。"""
        self._frame = bgr
        if bgr is None:
            self._display = None
        else:
            self._display = self._to_pixmap(bgr)
        self.update()

    def set_roi(self, roi: ROI):
        self._roi = roi
        self.update()

    def _to_pixmap(self, bgr: np.ndarray) -> QPixmap:
        rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
        h, w, ch = rgb.shape
        img = QImage(rgb.data, w, h, ch * w, QImage.Format_RGB888)
        return QPixmap.fromImage(img)

    def _frame_to_label_scale(self):
        """返回 (scale_x, scale_y, offset_x, offset_y) 用于坐标换算。"""
        if self._frame is None:
            return 1.0, 1.0, 0, 0
        fh, fw = self._frame.shape[:2]
        lw, lh = self.width(), self.height()
        scale = min(lw / fw, lh / fh)
        ox = (lw - fw * scale) / 2
        oy = (lh - fh * scale) / 2
        return scale, scale, ox, oy

    def mousePressEvent(self, event):
        if not self._roi_enabled or self._frame is None:
            return
        if event.button() == Qt.LeftButton:
            self._dragging = True
            self._drag_start = (event.position().x(), event.position().y())
            # 以点击点为 ROI 起点
            sx, sy, ox, oy = self._frame_to_label_scale()
            fx = int((event.position().x() - ox) / sx)
            fy = int((event.position().y() - oy) / sy)
            self._roi = ROI(x=fx, y=fy, w=0, h=0)
            self.roi_changed.emit(self._roi)
            self.update()

    def mouseMoveEvent(self, event):
        if not self._dragging or self._frame is None:
            return
        sx, sy, ox, oy = self._frame_to_label_scale()
        x0, y0 = self._drag_start
        x1, y1 = event.position().x(), event.position().y()
        fx0 = int((x0 - ox) / sx)
        fy0 = int((y0 - oy) / sy)
        fx1 = int((x1 - ox) / sx)
        fy1 = int((y1 - oy) / sy)
        self._roi = ROI(
            x=min(fx0, fx1), y=min(fy0, fy1),
            w=abs(fx1 - fx0), h=abs(fy1 - fy0),
        )
        self.roi_changed.emit(self._roi)
        self.update()

    def mouseReleaseEvent(self, event):
        self._dragging = False

    def paintEvent(self, event):
        super().paintEvent(event)
        if self._display is None or self._frame is None:
            return
        painter = QPainter(self)
        # 居中绘制
        sx, sy, ox, oy = self._frame_to_label_scale()
        fh, fw = self._frame.shape[:2]
        target = self._display.scaled(
            int(fw * sx), int(fh * sy), Qt.KeepAspectRatio, Qt.SmoothTransformation
        )
        painter.drawPixmap(int(ox), int(oy), target)

        # 绘制 ROI 框
        if self._roi.is_valid:
            rx = int(ox + self._roi.x * sx)
            ry = int(oy + self._roi.y * sy)
            rw = int(self._roi.w * sx)
            rh = int(self._roi.h * sy)
            pen = QPen(QColor(22, 199, 132), 2)
            painter.setPen(pen)
            painter.drawRect(rx, ry, rw, rh)
        painter.end()
