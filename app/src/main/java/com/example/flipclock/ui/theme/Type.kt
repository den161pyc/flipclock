package com.example.flipclock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font // Добавить импорт
import androidx.compose.ui.unit.sp
import com.example.flipclock.R // Добавить импорт R

// 1. Создаем переменную для вашего шрифта
val ClockFontFamily = FontFamily(
    Font(R.font.clock_font) // Замените clock_font на имя вашего файла без расширения
)
// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = ClockFontFamily, // 2. Можно назначить его по умолчанию для стилей
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)