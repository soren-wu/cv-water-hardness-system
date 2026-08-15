# 水硬度滴定终点颜色检测端

基于 Python + OpenCV + PySide6 的桌面视觉检测端，用于实时识别 EDTA 水硬度滴定过程中溶液颜色变化，辅助判断滴定终点。

## 功能

- 学生账号登录（对接 Java 后端）
- USB / 内置摄像头实时采集与预览
- 鼠标框选 ROI 区域
- HSV 颜色识别：区分酒红色、蓝紫色、纯蓝色
- 纯蓝色 30 秒稳定性验证（终点判定）
- 终点确认提醒 + 关键帧保存
- HSV 实时曲线（PyQtGraph）
- 实验结果上传后端 + 网络异常本地缓存补传
- CSV 数据导出

## 目录结构

```
detector-client/
├─ main.py                      # 入口
├─ ui/
│  ├─ login_dialog.py           # 登录对话框
│  ├─ video_label.py            # 视频预览 + ROI 框选
│  └─ main_window.py            # 主窗口
├─ vision/
│  ├─ camera.py                 # 摄像头封装
│  ├─ roi.py                    # ROI 模型
│  ├─ preprocessing.py          # 高斯滤波等预处理
│  └─ color_classifier.py       # 颜色识别核心
├─ detection/
│  └─ endpoint_state_machine.py # 终点状态机（30 秒稳定）
├─ api/
│  └─ backend_client.py         # 后端 API 客户端
├─ storage/
│  ├─ local_cache.py            # 断网缓存补传
│  └─ exporter.py               # CSV 导出
├─ config/
│  └─ __init__.py               # 阈值模板与运行配置
├─ tests/                       # 核心算法单元测试
└─ requirements.txt
```

## 安装

```bash
cd detector-client
pip install -r requirements.txt
```

## 运行

先确保 Java 后端已在 8080 端口启动，然后：

```bash
python main.py
```

登录账号 `student01` / `123456`。

## 测试（核心算法，无需摄像头）

```bash
pytest tests/ -v
```

## 打包 exe

```bash
pyinstaller --noconsole --onefile main.py
```

## 状态机

```
待机 → 采集准备 → 初始色(酒红) → 滴定中 → 临近终点(紫蓝) → 候选终点(纯蓝) → 有效终点(稳定30s)
```

候选终点期间若返色，取消计时回到对应颜色状态。
