"""检测端环境自检脚本。

一键检查：依赖 → 核心算法 → 后端连通 → 摄像头。
运行：python check_env.py
"""

from __future__ import annotations

import sys


def check_imports() -> list[str]:
    """检查核心依赖是否安装。"""
    modules = {
        "PySide6": "桌面 GUI 界面",
        "cv2": "OpenCV 图像处理",
        "numpy": "数值计算",
        "httpx": "后端 API 调用",
        "pyqtgraph": "HSV 实时曲线",
    }
    missing = []
    for name, desc in modules.items():
        try:
            __import__(name)
            print(f"  [✓] {name:12s} {desc}")
        except ImportError:
            print(f"  [✗] {name:12s} {desc} —— 未安装")
            missing.append(name)
    return missing


def check_algorithm() -> bool:
    """检查核心算法（颜色分类 + 状态机）。"""
    import numpy as np
    import cv2
    from vision.color_classifier import ColorClassifier, ColorState
    from detection.endpoint_state_machine import EndpointStateMachine, DetectorState

    clf = ColorClassifier()
    # 合成纯蓝色图片
    hsv = np.full((30, 30, 3), [105, 204, 204], dtype=np.uint8)
    blue = cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)
    r = clf.classify(blue)
    if r.color != ColorState.BLUE:
        print("  [✗] 颜色分类异常")
        return False

    sm = EndpointStateMachine()
    sm.start()
    sm.feed(r)
    if sm.state not in (DetectorState.CANDIDATE_ENDPOINT,):
        print("  [✗] 状态机异常")
        return False
    print("  [✓] 颜色分类 + 状态机正常")
    return True


def check_backend(url: str = "http://localhost:8080") -> bool:
    """检查后端连通性。"""
    from api.backend_client import BackendClient
    try:
        c = BackendClient(url)
        u = c.login("student01", "123456")
        print(f"  [✓] 后端连接正常，登录 {u.real_name}（{u.role}）")
        c.close()
        return True
    except Exception as e:
        print(f"  [✗] 后端连接失败：{e}")
        print("      请确认后端已启动：cd backend && .\\mvnw.cmd spring-boot:run --server.port=8080")
        return False


def check_camera() -> bool:
    """检查摄像头。"""
    try:
        import cv2
        from vision.camera import list_cameras
        cams = list_cameras()
        if cams:
            print(f"  [✓] 检测到摄像头：索引 {cams}")
            return True
        print("  [✗] 未检测到摄像头")
        return False
    except Exception as e:
        print(f"  [✗] 摄像头检查失败：{e}")
        return False


def main():
    print("=" * 50)
    print("水硬度滴定检测端 - 环境自检")
    print("=" * 50)

    print("\n[1/4] 检查依赖")
    missing = check_imports()
    if missing:
        print(f"\n缺少依赖：{', '.join(missing)}")
        print("安装：pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple")
        return 1

    print("\n[2/4] 检查核心算法")
    if not check_algorithm():
        return 1

    print("\n[3/4] 检查后端")
    if not check_backend():
        return 1

    print("\n[4/4] 检查摄像头")
    cam_ok = check_camera()

    print("\n" + "=" * 50)
    if cam_ok:
        print("自检通过 ✅ 可运行 python main.py 启动检测端")
    else:
        print("自检基本通过，但无摄像头。接入摄像头后运行 python main.py")
    print("=" * 50)
    return 0


if __name__ == "__main__":
    sys.exit(main())
