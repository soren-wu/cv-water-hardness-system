"""检测端全局配置。

所有可调参数集中在此，方便管理员通过阈值模板下发、或本地微调。
"""

from dataclasses import dataclass, field


@dataclass
class ThresholdConfig:
    """HSV 颜色阈值模板，与后端 threshold_templates 表对应。"""
    # 酒红色（红色跨越 0 度，用两段表示；下界 315 覆盖偏紫酒红）
    red_h_min: float = 315.0
    red_h_max: float = 25.0
    # 蓝紫色
    purple_h_min: float = 235.0
    purple_h_max: float = 315.0
    # 纯蓝色
    blue_h_min: float = 185.0
    blue_h_max: float = 235.0
    # 最低饱和度 / 明度（过滤低饱和灰白噪声）
    min_saturation: float = 0.08
    min_brightness: float = 0.12
    # 终点稳定时长（秒）
    stable_duration_seconds: int = 30


@dataclass
class DetectorConfig:
    """检测端运行参数。"""
    # 采样频率（帧/秒）
    sample_fps: float = 5.0
    # 高斯滤波核大小（奇数）
    gaussian_kernel: int = 5
    # 后端地址
    backend_url: str = "http://localhost:8080"
    # 本地缓存目录
    cache_dir: str = "./cache"
    # 导出目录
    export_dir: str = "./exports"
    # 阈值模板
    threshold: ThresholdConfig = field(default_factory=ThresholdConfig)


# 默认配置实例
DEFAULT_CONFIG = DetectorConfig()
