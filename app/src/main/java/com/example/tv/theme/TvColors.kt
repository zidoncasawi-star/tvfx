package com.example.tv.theme

import androidx.compose.ui.graphics.Color

// نفس لوحة ألوان تطبيق سطح المكتب حرفياً (flixapp/desktop/style.css :root) — الهدف أن تبدو
// شاشات التلفاز امتداداً بصرياً لنفس تطبيق سطح المكتب، لا تصميماً جديداً منفصلاً.
val TvRed = Color(0xFFE50914)          // --red
val TvBg = Color(0xFF08080A)           // --bg
val TvPanel = Color(0xFF121216)        // --panel
val TvCard = Color(0xFF18181C)         // --card
val TvCardAlt = Color(0xFF202024)      // --card-alt
val TvTextGray = Color(0xFFA3A3A3)     // --text-gray
val TvBorder = Color(0x14FFFFFF)       // rgba(255,255,255,0.08)
val TvTextWhite = Color(0xFFFFFFFF)

// حالات التركيز (D-pad focus) — التلفاز ليس له hover، فالتركيز هو المعادل الوظيفي
val TvFocusBorder = TvRed
val TvFocusScale = 1.08f
