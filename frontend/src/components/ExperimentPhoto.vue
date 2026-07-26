<template>
  <div class="experiment-photo" aria-label="纯蓝色滴定终点示意图">
    <svg viewBox="0 0 160 190" role="img">
      <defs>
        <linearGradient id="lab" x1="0" x2="1">
          <stop offset="0" stop-color="#dfe6ec" />
          <stop offset=".55" stop-color="#f4f6f7" />
          <stop offset="1" stop-color="#c8d2d9" />
        </linearGradient>
        <linearGradient id="blueLiquid" x1="0" y1="0" x2="0" y2="1">
          <stop stop-color="#0d58ee" />
          <stop offset="1" stop-color="#042c98" />
        </linearGradient>
      </defs>
      <rect width="160" height="190" fill="url(#lab)" />
      <path d="M0 43h160M0 66h160M20 0v190M130 0v190" stroke="#b8c2c9" stroke-width="3" opacity=".65" />
      <rect x="12" y="41" width="16" height="69" rx="2" fill="#143d80" opacity=".9" />
      <rect x="134" y="59" width="17" height="58" rx="2" fill="#2b66ca" opacity=".75" />
      <rect x="0" y="153" width="160" height="37" fill="#d1d4d4" />
      <path d="M64 18h32M69 18v50l-30 78c-5 14 4 22 18 22h46c14 0 23-8 18-22L91 68V18" fill="#f9feff" fill-opacity=".45" stroke="#89989f" stroke-width="3" />
      <path d="M47 130c20 5 43-4 67 0l9 24c3 9-6 15-19 15H56c-13 0-22-6-19-15z" fill="url(#blueLiquid)" opacity=".95" />
      <path d="M49 136c19 5 42-5 63 0M58 107h42M54 119h51" stroke="#edf8ff" stroke-width="2" opacity=".75" />
      <path d="M64 37h32M67 47h26" stroke="#9ca9ae" stroke-width="2" />
      <text x="61" y="124" fill="#eef7ff" font-size="9">150</text>
      <text x="56" y="110" fill="#6f7d81" font-size="9">200</text>
      <ellipse cx="80" cy="174" rx="48" ry="7" fill="#7c858a" opacity=".25" />
    </svg>
  </div>
</template>
