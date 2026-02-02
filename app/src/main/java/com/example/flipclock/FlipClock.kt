package com.example.flipclock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlin.math.ceil

// ... (Остальной код: ThemeColors, константы, FlipClockScreen и т.д. остаются без изменений) ...

// === НОВЫЙ СТИЛЬ ИНДИКАТОРА: ВДАВЛЕННЫЕ КНОПКИ ===
@Composable
fun BatteryIndicator(
    level: Int,
    isCharging: Boolean,
    showShadows: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val activeSegmentsCount = ceil(level / 20.0).toInt().coerceIn(0, 5)

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f, // Пульсация для обводки
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Фон для индикаторов (светло-серый, как на кнопках с картинки)
    val buttonBgColor = Color(0xFFEEEEEE)
    val pillShape = RoundedCornerShape(100)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor.copy(alpha = 0.8f))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (i in 5 downTo 1) {
                val isActive = i <= activeSegmentsCount

                // Цвет обводки в зависимости от уровня
                val borderColor = when {
                    level == 100 && isActive -> Color(0xFF4CAF50)
                    level <= 20 && i == 1 && isActive -> Color(0xFFF44336)
                    isActive -> Color(0xFFFFC107)
                    else -> Color(0xFF424242) // Темно-серый для неактивных (как слева на картинке)
                }

                val isBlinking = isCharging && isActive && (i == activeSegmentsCount)
                val currentBorderAlpha = if (isBlinking) blinkAlpha else 1f
                val borderThickness = if (isActive) 2.dp else 1.5.dp // Активная обводка чуть толще

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.9f)
                ) {
                    // 1. ТЕНЬ (ГЛУБИНА) - внешняя тень всего элемента (если включено)
                    if (showShadows) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationY = 1.5.dp.toPx() }
                                .shadow(3.dp, pillShape, false, Color.Black.copy(alpha=0.6f), Color.Black)
                        )
                    }

                    // 2. ОСНОВНОЕ ТЕЛО ИНДИКАТОРА
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Обводка: Цветная (активный) или серая (неактивный), пульсирует при зарядке
                            .border(borderThickness, borderColor.copy(alpha = currentBorderAlpha), pillShape)
                            .clip(pillShape)
                            // Фон: Светло-серый, имитация поверхности кнопки
                            .background(buttonBgColor)
                    ) {
                        // 3. ВНУТРЕННЯЯ ТЕНЬ (Эффект вдавленности)
                        // Рисуем градиент, который темнее сверху и светлее снизу, создавая объем
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.25f), // Тень сверху
                                            Color.Transparent,               // Середина
                                            Color.White.copy(alpha = 0.4f)   // Свет снизу
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ... (Остальной код: luminance, FlipCard, NumberTile и т.д. остаются без изменений) ...