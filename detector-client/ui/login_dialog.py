"""登录对话框。

学生输入账号密码登录后端，登录成功后进入主界面。

对应需求 DET-01。
"""

from __future__ import annotations

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QDialog, QLabel, QLineEdit, QPushButton, QVBoxLayout, QHBoxLayout, QMessageBox,
)

from api.backend_client import BackendClient, UserInfo


class LoginDialog(QDialog):
    def __init__(self, client: BackendClient, parent=None):
        super().__init__(parent)
        self.client = client
        self.user: UserInfo | None = None
        self.setWindowTitle("登录 - 水硬度滴定检测端")
        self.setFixedSize(360, 240)
        self._build_ui()

    def _build_ui(self):
        layout = QVBoxLayout(self)
        layout.setSpacing(12)

        title = QLabel("水硬度滴定终点检测端")
        title.setStyleSheet("font-size:18px;font-weight:bold;")
        title.setAlignment(Qt.AlignCenter)
        layout.addWidget(title)

        form = QVBoxLayout()
        self.username_input = QLineEdit()
        self.username_input.setPlaceholderText("账号（如 student01）")
        self.password_input = QLineEdit()
        self.password_input.setPlaceholderText("密码")
        self.password_input.setEchoMode(QLineEdit.Password)
        form.addWidget(self.username_input)
        form.addWidget(self.password_input)
        layout.addLayout(form)

        btn_row = QHBoxLayout()
        self.login_btn = QPushButton("登录")
        self.login_btn.setFixedHeight(36)
        self.cancel_btn = QPushButton("取消")
        self.cancel_btn.setFixedHeight(36)
        btn_row.addWidget(self.login_btn)
        btn_row.addWidget(self.cancel_btn)
        layout.addLayout(btn_row)

        hint = QLabel("默认账号 student01 / 123456")
        hint.setStyleSheet("color:#888;font-size:11px;")
        hint.setAlignment(Qt.AlignCenter)
        layout.addWidget(hint)

        self.login_btn.clicked.connect(self._do_login)
        self.cancel_btn.clicked.connect(self.reject)
        self.password_input.returnPressed.connect(self._do_login)

    def _do_login(self):
        username = self.username_input.text().strip()
        password = self.password_input.text().strip()
        if not username or not password:
            QMessageBox.warning(self, "提示", "请输入账号和密码")
            return

        self.login_btn.setEnabled(False)
        self.login_btn.setText("登录中...")
        try:
            self.user = self.client.login(username, password)
            self.accept()
        except Exception as e:
            QMessageBox.warning(self, "登录失败", str(e))
        finally:
            self.login_btn.setEnabled(True)
            self.login_btn.setText("登录")
