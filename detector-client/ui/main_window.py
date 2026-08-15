"""检测端主窗口。

集成摄像头预览、ROI 框选、实时颜色识别、终点状态机、HSV 曲线和结果上传。

对应需求 DET-03 ~ DET-10。
"""

from __future__ import annotations

import time
from datetime import datetime

import cv2
import numpy as np
import pyqtgraph as pg
from PySide6.QtCore import QTimer, Qt
from PySide6.QtGui import QColor
from PySide6.QtWidgets import (
    QMainWindow, QWidget, QHBoxLayout, QVBoxLayout, QLabel, QPushButton,
    QGroupBox, QGridLayout, QMessageBox, QComboBox,
)

from api.backend_client import BackendClient, UserInfo
from config import DetectorConfig, ThresholdConfig
from detection.endpoint_state_machine import EndpointStateMachine, DetectorState
from storage.exporter import export_samples_csv, export_events_csv
from storage.local_cache import LocalCache
from ui.video_label import VideoLabel
from vision.camera import Camera, list_cameras
from vision.color_classifier import ColorClassifier, ColorState
from vision.preprocessing import denoise, crop_roi
from vision.roi import ROI, default_center_roi


# 状态 → 中文标签
STATE_LABELS = {
    DetectorState.IDLE: "待机",
    DetectorState.READY: "采集准备",
    DetectorState.INITIAL: "初始色（酒红）",
    DetectorState.TITRATING: "滴定中",
    DetectorState.NEAR_ENDPOINT: "临近终点（紫蓝）",
    DetectorState.CANDIDATE_ENDPOINT: "候选终点（计时中）",
    DetectorState.ENDPOINT: "有效终点 ✅",
    DetectorState.PAUSED: "已暂停",
    DetectorState.ERROR: "异常",
}


class MainWindow(QMainWindow):
    def __init__(self, client: BackendClient, user: UserInfo, config: DetectorConfig | None = None):
        super().__init__()
        self.client = client
        self.user = user
        self.config = config or DetectorConfig()

        self.classifier = ColorClassifier(self.config.threshold)
        self.state_machine = EndpointStateMachine(self.config.threshold)
        self.camera = Camera(index=0)
        self.cache = LocalCache(self.config.cache_dir)

        # 运行状态
        self._samples: list[dict] = []      # HSV 采样记录
        self._keyframes: list[str] = []     # 关键帧路径
        self._hue_curve: list[float] = []
        self._sat_curve: list[float] = []
        self._val_curve: list[float] = []

        self.setWindowTitle(f"水硬度滴定检测端 - {user.real_name}")
        self.resize(1180, 680)
        self._build_ui()

        # 检测定时器（5 fps）
        self.timer = QTimer(self)
        self.timer.timeout.connect(self._detect_loop)
        self.timer.start(int(1000 / self.config.sample_fps))

        self._setup_camera()

    # ---------- UI ----------
    def _build_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        root = QHBoxLayout(central)

        # 左侧：视频预览
        left = QVBoxLayout()
        self.video_label = VideoLabel()
        left.addWidget(self.video_label)
        left.addWidget(QLabel("拖动鼠标框选 ROI 区域，分析仅针对 ROI 内溶液"))
        root.addLayout(left, stretch=3)

        # 右侧：状态面板
        right = QVBoxLayout()
        right.setSpacing(10)

        status_box = QGroupBox("检测状态")
        grid = QGridLayout(status_box)
        self.status_label = QLabel("待机")
        self.status_label.setStyleSheet("font-size:20px;font-weight:bold;color:#0756c5;")
        grid.addWidget(QLabel("当前状态："), 0, 0)
        grid.addWidget(self.status_label, 0, 1)

        self.timer_label = QLabel("0.0 s / 30 s")
        grid.addWidget(QLabel("稳定计时："), 1, 0)
        grid.addWidget(self.timer_label, 1, 1)

        self.confidence_label = QLabel("--")
        grid.addWidget(QLabel("匹配度："), 2, 0)
        grid.addWidget(self.confidence_label, 2, 1)

        self.hsv_label = QLabel("H:-- S:-- V:--")
        grid.addWidget(QLabel("HSV 均值："), 3, 0)
        grid.addWidget(self.hsv_label, 3, 1)

        self.ratio_label = QLabel("红:-- 紫:-- 蓝:--")
        grid.addWidget(QLabel("颜色占比："), 4, 0)
        grid.addWidget(self.ratio_label, 4, 1)
        right.addWidget(status_box)

        # 控制按钮
        ctrl_box = QGroupBox("控制")
        ctrl = QVBoxLayout(ctrl_box)
        cam_row = QHBoxLayout()
        cam_row.addWidget(QLabel("摄像头："))
        self.camera_combo = QComboBox()
        cam_row.addWidget(self.camera_combo)
        ctrl.addLayout(cam_row)

        btn_row = QHBoxLayout()
        self.start_btn = QPushButton("开始检测")
        self.pause_btn = QPushButton("暂停")
        self.upload_btn = QPushButton("上传结果")
        self.upload_btn.setEnabled(False)
        btn_row.addWidget(self.start_btn)
        btn_row.addWidget(self.pause_btn)
        btn_row.addWidget(self.upload_btn)
        ctrl.addLayout(btn_row)
        right.addWidget(ctrl_box)

        # HSV 曲线
        curve_box = QGroupBox("HSV 实时曲线")
        curve_layout = QVBoxLayout(curve_box)
        self.plot_widget = pg.PlotWidget()
        self.plot_widget.setBackground("#fff")
        self.plot_widget.addLegend()
        self.curve_h = self.plot_widget.plot(pen=pg.mkPen("#1478ff", width=2), name="H")
        self.curve_s = self.plot_widget.plot(pen=pg.mkPen("#19a556", width=2), name="S")
        self.curve_v = self.plot_widget.plot(pen=pg.mkPen("#7f20ed", width=2), name="V")
        self.plot_widget.setMinimumHeight(180)
        curve_layout.addWidget(self.plot_widget)
        right.addWidget(curve_box, stretch=1)

        root.addLayout(right, stretch=2)

        # 事件绑定
        self.start_btn.clicked.connect(self._start)
        self.pause_btn.clicked.connect(self._pause)
        self.upload_btn.clicked.connect(self._upload)
        self.camera_combo.currentIndexChanged.connect(self._switch_camera)
        self.video_label.roi_changed.connect(self._on_roi_changed)

    # ---------- 摄像头 ----------
    def _setup_camera(self):
        cams = list_cameras()
        self.camera_combo.addItems([f"摄像头 {i}" for i in cams] if cams else ["摄像头 0"])
        if cams:
            self.camera = Camera(index=cams[0])
            self._open_camera()

    def _open_camera(self):
        if not self.camera.open():
            QMessageBox.warning(self, "提示", "无法打开摄像头，请检查设备连接")
            self.state_machine.error("摄像头打开失败")

    def _switch_camera(self):
        self.camera.close()
        idx = self.camera_combo.currentIndex()
        self.camera = Camera(index=idx)
        self._open_camera()

    # ---------- 控制 ----------
    def _start(self):
        if not self.camera.is_opened:
            self._open_camera()
        self.state_machine.start()
        self.upload_btn.setEnabled(False)

    def _pause(self):
        self.state_machine.pause()

    def _on_roi_changed(self, roi: ROI):
        if not roi.is_valid:
            return
        self._roi = roi

    # ---------- 检测循环 ----------
    def _detect_loop(self):
        frame = self.camera.read()
        if frame is None:
            self.video_label.set_frame(None)
            return

        # ROI 裁剪 + 去噪 + 分类
        roi = getattr(self, "_roi", None)
        if roi is None or not roi.is_valid:
            roi = default_center_roi(frame.shape[1], frame.shape[0])
            self._roi = roi
            self.video_label.set_roi(roi)

        region = crop_roi(frame, roi.to_tuple())
        region = denoise(region, self.config.gaussian_kernel)
        result = self.classifier.classify(region)

        # 状态机推进
        is_endpoint = self.state_machine.feed(result)

        # 记录采样
        self._samples.append({
            "time": datetime.now().strftime("%H:%M:%S"),
            "hue": round(result.hue_degree, 2),
            "saturation": round(result.saturation, 4),
            "brightness": round(result.brightness, 4),
            "confidence": result.confidence,
            "state": self.state_machine.state.value,
            "red_ratio": round(result.red_ratio, 4),
            "purple_ratio": round(result.purple_ratio, 4),
            "blue_ratio": round(result.blue_ratio, 4),
        })

        # 更新曲线
        self._hue_curve.append(result.hue_degree)
        self._sat_curve.append(result.saturation)
        self._val_curve.append(result.brightness)
        if len(self._hue_curve) > 300:
            self._hue_curve.pop(0)
            self._sat_curve.pop(0)
            self._val_curve.pop(0)
        self.curve_h.setData(self._hue_curve)
        self.curve_s.setData(self._sat_curve)
        self.curve_v.setData(self._val_curve)

        # 更新 UI
        self._update_ui(result)

        # 画面显示（绘制 ROI）
        self.video_label.set_frame(frame)

        # 终点确认
        if is_endpoint:
            self._on_endpoint(frame, result)

    def _update_ui(self, result):
        state = self.state_machine.state
        self.status_label.setText(STATE_LABELS.get(state, state.value))
        self.status_label.setStyleSheet(f"font-size:20px;font-weight:bold;color:{self._state_color(state)};")

        stable = self.state_machine.stable_duration
        self.timer_label.setText(f"{stable:.1f} s / {self.state_machine.required_duration} s")

        self.confidence_label.setText(f"{result.confidence:.1f}%")
        self.hsv_label.setText(f"H:{result.hue_degree:.0f}° S:{result.saturation:.0f} V:{result.brightness:.0f}")
        self.ratio_label.setText(
            f"红:{result.red_ratio:.0%} 紫:{result.purple_ratio:.0%} 蓝:{result.blue_ratio:.0%}"
        )

    @staticmethod
    def _state_color(state: DetectorState) -> str:
        return {
            DetectorState.ENDPOINT: "#0dab68",
            DetectorState.CANDIDATE_ENDPOINT: "#0756c5",
            DetectorState.NEAR_ENDPOINT: "#6332a1",
            DetectorState.INITIAL: "#8f1f2b",
            DetectorState.TITRATING: "#d87906",
            DetectorState.ERROR: "#f34d52",
            DetectorState.PAUSED: "#888",
        }.get(state, "#0756c5")

    # ---------- 终点处理 ----------
    def _on_endpoint(self, frame, result):
        """有效终点确认：提醒 + 保存关键帧 + 允许上传。"""
        self.upload_btn.setEnabled(True)
        # 保存关键帧
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        keyframe = f"{self.config.export_dir}/keyframe_{ts}.jpg"
        import os
        os.makedirs(self.config.export_dir, exist_ok=True)
        cv2.imwrite(keyframe, frame)
        self._keyframes.append(keyframe)

        QMessageBox.information(
            self, "滴定终点",
            f"已确认滴定终点！\n匹配度 {result.confidence:.1f}%\n稳定时长 {self.state_machine.required_duration}s\n关键帧已保存",
        )

    # ---------- 上传 ----------
    def _upload(self):
        """上传实验结果到后端。"""
        samples = self._samples
        if not samples:
            QMessageBox.warning(self, "提示", "暂无实验数据")
            return

        # 取最后一条作为终点结果
        last = samples[-1]
        payload = {
            "taskId": None,
            "experimentName": "EDTA 水硬度滴定",
            "sampleName": "水样",
            "detectMode": "CAMERA",
            "recognitionStatus": self.state_machine.state.value,
            "recognitionLabel": STATE_LABELS.get(self.state_machine.state, ""),
            "matchedColor": self._matched_color(self.state_machine.state),
            "confidence": last["confidence"],
            "hue": last["hue"],
            "saturation": last["saturation"],
            "brightness": last["brightness"],
            "redRatio": last["red_ratio"],
            "purpleRatio": last["purple_ratio"],
            "blueRatio": last["blue_ratio"],
            "stableDurationSeconds": self.state_machine.required_duration,
            "submitStatus": "SUBMITTED",
            "remark": f"检测端自动识别，共 {len(samples)} 个采样点",
        }

        try:
            exp = self.client.submit_experiment(payload)
            exp_id = exp.get("id")
            # 上传关键帧
            for kf in self._keyframes:
                try:
                    self.client.upload_file(exp_id, kf, "KEYFRAME")
                except Exception:
                    pass  # 关键帧上传失败不阻塞主流程
            # 导出本地 CSV
            export_samples_csv(samples, f"{self.config.export_dir}/samples.csv")
            export_events_csv(
                [{"time": datetime.now().strftime("%H:%M:%S"),
                  "type": e.event_type, "message": e.message}
                 for e in self.state_machine.events],
                f"{self.config.export_dir}/events.csv",
            )
            QMessageBox.information(self, "上传成功", f"实验结果已上传（记录 ID {exp_id}）")
        except Exception as e:
            # 网络异常 → 本地缓存
            self.cache.save(payload, self._keyframes)
            QMessageBox.warning(self, "网络异常", f"上传失败，已本地缓存，网络恢复后补传。\n{str(e)}")

    @staticmethod
    def _matched_color(state: DetectorState) -> str:
        return {
            DetectorState.ENDPOINT: "BLUE",
            DetectorState.CANDIDATE_ENDPOINT: "BLUE",
            DetectorState.NEAR_ENDPOINT: "PURPLE",
            DetectorState.INITIAL: "RED",
        }.get(state, "UNKNOWN")

    # ---------- 关闭 ----------
    def closeEvent(self, event):
        self.timer.stop()
        self.camera.close()
        self.client.close()
        super().closeEvent(event)
