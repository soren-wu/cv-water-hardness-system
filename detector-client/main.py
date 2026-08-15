"""检测端入口。

启动流程：显示登录对话框 → 登录成功 → 进入主窗口。

运行：python main.py
"""

from __future__ import annotations

import sys

from PySide6.QtWidgets import QApplication

from api.backend_client import BackendClient
from config import DetectorConfig
from ui.login_dialog import LoginDialog
from ui.main_window import MainWindow


def main():
    app = QApplication(sys.argv)

    config = DetectorConfig()
    client = BackendClient(config.backend_url)

    # 登录
    login = LoginDialog(client)
    if login.exec() != LoginDialog.Accepted or login.user is None:
        client.close()
        return 0

    # 主窗口
    window = MainWindow(client, login.user, config)
    window.show()
    return app.exec()


if __name__ == "__main__":
    sys.exit(main())
