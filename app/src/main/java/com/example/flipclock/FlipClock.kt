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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.ceil

// --- ТЕМЫ И КОНСТАНТЫ ---

data class ThemeColors(
    val cardGradientTop: Color,
    val cardGradientBottom: Color,
    val text: Color,
    val shineColor: Color
)

val DarkThemeColors = ThemeColors(
    cardGradientTop = Color(0xFF3A3A3A),
    cardGradientBottom = Color(0xFF202020),
    text = Color(0xFFE0E0E0),
    shineColor = Color(0xFFFFFFFF).copy(alpha = 0.08f)
)

val LightThemeColors = ThemeColors(
    cardGradientTop = Color(0xFFF2F2F7),
    cardGradientBottom = Color(0xFFD1D1D6),
    text = Color(0xFF1C1C1E),
    shineColor = Color(0xFFFFFFFF).copy(alpha = 0.6f)
)

val CardCornerRadius = 12.dp
const val CARD_ASPECT_RATIO = 0.7f

const val GAP_SMALL_RATIO = 0.05f
const val GAP_BIG_RATIO = 0.35f
const val HOLDER_WIDTH_RATIO = 0.05f

// --- ОСНОВНОЙ ЭКРАН ---

@Composable
fun FlipClockScreen(
    viewModel: BrightnessViewModel = viewModel()
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var showSettings by remember { mutableStateOf(false) }

    val themeMode by viewModel.themeMode.collectAsState()
    val bgColorInt by viewModel.backgroundColor.collectAsState()
    val palette by viewModel.palette.collectAsState()
    val isAutoBrightness by viewModel.isAutoBrightness.collectAsState()
    val manualVal by viewModel.manualBrightness.collectAsState()
    val luxStr by viewModel.sensorLuxString.collectAsState()
    val showShadows by viewModel.showShadows.collectAsState()

    val bgImageUri by viewModel.bgImageUri.collectAsState()
    val bgOpacity by viewModel.bgOpacity.collectAsState()
    val bgBlur by viewModel.bgBlur.collectAsState()
    val bgStretch by viewModel.bgStretch.collectAsState()

    val cardColorInt by viewModel.cardColor.collectAsState()

    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()

    val isSystemDark = isSystemInDarkTheme()

    // В режиме COLOR мы считаем тему темной, если яркость выбранного цвета низкая
    val isColorModeDark = if (themeMode == ThemeMode.COLOR) {
        Color(cardColorInt).luminance() < 0.5f
    } else false

    val isDarkThemeActive = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.COLOR -> isColorModeDark // Системные бары зависят от яркости флипов
        ThemeMode.AUTO -> isSystemDark
    }

    // Формируем цветовую тему флипов
    val currentTheme = when (themeMode) {
        ThemeMode.COLOR -> {
            val baseColor = Color(cardColorInt)
            // Определяем цвет текста по контрасту
            val textColor = if (baseColor.luminance() > 0.5f) Color.Black else Color.White
            // Создаем градиент: верх - выбранный цвет, низ - чуть темнее
            ThemeColors(
                cardGradientTop = baseColor,
                cardGradientBottom = baseColor.copy(
                    red = baseColor.red * 0.85f,
                    green = baseColor.green * 0.85f,
                    blue = baseColor.blue * 0.85f
                ),
                text = textColor,
                shineColor = if (textColor == Color.White) Color.White.copy(0.1f) else Color.Black.copy(0.05f)
            )
        }
        else -> if (isDarkThemeActive) DarkThemeColors else LightThemeColors
    }

    val currentBgColor = Color(bgColorInt)
    val context = LocalContext.current

    val batteryContainerColor = currentTheme.cardGradientTop

    val bgBitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, key1 = bgImageUri) {
        value = if (bgImageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(bgImageUri))
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                } catch (e: Exception) { null }
            }
        } else { null }
    }

    LaunchedEffect(Unit) {
        viewModel.initSensor(context)
        viewModel.registerBatteryReceiver(context)
        hideSystemBars(context)
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.unregisterBatteryReceiver(context) }
    }

    val hours = currentTime.get(Calendar.HOUR_OF_DAY)
    val minutes = currentTime.get(Calendar.MINUTE)
    val h1 = hours / 10; val h2 = hours % 10
    val m1 = minutes / 10; val m2 = minutes % 10

    val separatorColor = if (bgBitmap != null) Color.Black.copy(alpha = 0.4f) else currentBgColor

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bgBitmap != null) {
            Box(Modifier.fillMaxSize().background(Color.Black))
            var imageModifier = Modifier.fillMaxSize().graphicsLayer { alpha = bgOpacity }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (bgBlur > 0f) { imageModifier = imageModifier.blur((bgBlur * 50).dp) }
            }
            Image(
                bitmap = bgBitmap!!,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = if (bgStretch) ContentScale.Crop else ContentScale.Fit
            )
        } else {
            Box(Modifier.fillMaxSize().background(currentBgColor))
        }

        val singleDigitTotalWidthRatio = 1f + (2 * HOLDER_WIDTH_RATIO)
        val totalWidthUnits = (4 * singleDigitTotalWidthRatio) + (2 * GAP_SMALL_RATIO) + GAP_BIG_RATIO

        val maxAvailableWidth = maxWidth * 0.96f
        val maxAvailableHeight = maxHeight * 0.90f

        val cardWidthByWidth = maxAvailableWidth / totalWidthUnits
        val cardHeightByWidth = cardWidthByWidth / CARD_ASPECT_RATIO

        val cardHeightByHeight = maxAvailableHeight
        val cardWidthByHeight = cardHeightByHeight * CARD_ASPECT_RATIO

        val finalCardHeight = if (cardHeightByWidth <= maxAvailableHeight) cardHeightByWidth else cardHeightByHeight
        val finalCardWidth = if (cardHeightByWidth <= maxAvailableHeight) cardWidthByWidth else cardWidthByHeight

        val density = LocalDensity.current
        val fontSize = with(density) { (finalCardHeight * 0.85f).toSp() }
        val smallGap = finalCardWidth * GAP_SMALL_RATIO
        val bigGap = finalCardWidth * GAP_BIG_RATIO

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FlipCard(h1, finalCardWidth, finalCardHeight, fontSize, currentTheme, showShadows, separatorColor)
            Spacer(modifier = Modifier.width(smallGap))
            FlipCard(h2, finalCardWidth, finalCardHeight, fontSize, currentTheme, showShadows, separatorColor)

            Box(
                modifier = Modifier
                    .width(bigGap)
                    .height(finalCardHeight * 0.65f),
                contentAlignment = Alignment.Center
            ) {
                BatteryIndicator(
                    level = batteryLevel,
                    isCharging = isCharging,
                    showShadows = showShadows,
                    backgroundColor = batteryContainerColor,
                    flipTheme = currentTheme,
                    modifier = Modifier.fillMaxSize()
                )
            }

            FlipCard(m1, finalCardWidth, finalCardHeight, fontSize, currentTheme, showShadows, separatorColor)
            Spacer(modifier = Modifier.width(smallGap))
            FlipCard(m2, finalCardWidth, finalCardHeight, fontSize, currentTheme, showShadows, separatorColor)
        }

        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (bgBitmap != null) Color.White.copy(0.7f) else if (currentBgColor.luminance() > 0.5f) Color.Black.copy(0.6f) else Color.White.copy(0.6f),
                modifier = Modifier.size(32.dp)
            )
        }

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false; hideSystemBars(context) },
                themeMode = themeMode, onThemeChanged = { viewModel.setThemeMode(it) },
                currentBgColor = bgColorInt, palette = palette,
                onColorSelected = { viewModel.setBackgroundColor(it) },
                onPaletteEdited = { index, color -> viewModel.updatePaletteColor(index, color) },
                isAutoBrightness = isAutoBrightness, onAutoBrightnessChanged = { viewModel.toggleAutoBrightness(it) },
                brightness = manualVal, onBrightnessChanged = { viewModel.setManualBrightness(it) },
                luxString = luxStr, showShadows = showShadows, onShadowsChanged = { viewModel.toggleShadows(it) },
                bgImageUri = bgImageUri, onBgImageSelected = { viewModel.setBgImage(it) },
                bgOpacity = bgOpacity, onBgOpacityChanged = { viewModel.setBgOpacity(it) },
                bgBlur = bgBlur, onBgBlurChanged = { viewModel.setBgBlur(it) },
                bgStretch = bgStretch, onBgStretchChanged = { viewModel.setBgStretch(it) },
                currentCardColor = cardColorInt,
                onCardColorChanged = { viewModel.setCardColor(it) }
            )
        }
    }
}

// === КОНТУРНЫЙ ИНДИКАТОР ===

@Composable
fun BatteryIndicator(
    level: Int,
    isCharging: Boolean,
    showShadows: Boolean,
    backgroundColor: Color,
    flipTheme: ThemeColors,
    modifier: Modifier = Modifier
) {
    val activeSegmentsCount = ceil(level / 20.0).toInt().coerceIn(0, 5)

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pillShape = RoundedCornerShape(100)
    val containerShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .then(
                if (showShadows) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = containerShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha=0.5f),
                        spotColor = Color.Black
                    )
                } else Modifier
            )
            .clip(containerShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), containerShape)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (i in 5 downTo 1) {
                val isActive = i <= activeSegmentsCount

                val glowColor = when {
                    level == 100 -> Color(0xFF4CAF50)
                    level <= 20 && i == 1 -> Color(0xFFF44336)
                    else -> Color(0xFFFFB74D)
                }

                val isBlinking = isCharging && isActive && (i == activeSegmentsCount)
                val currentGlowAlpha = if (isBlinking) pulseAlpha else 1f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(currentGlowAlpha)
                                .shadow(12.dp, pillShape, false, glowColor, glowColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(currentGlowAlpha)
                                .background(glowColor, pillShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha=0.3f), pillShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .shadow(2.dp, pillShape)
                            .clip(pillShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(flipTheme.cardGradientTop, flipTheme.cardGradientBottom)
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    colors = listOf(Color.White.copy(0.15f), Color.Transparent),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- ФЛИП-КАРТА ---

@Composable
fun FlipCard(
    value: Int,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    theme: ThemeColors,
    showShadows: Boolean,
    separatorColor: Color
) {
    var animatedValue by remember { mutableIntStateOf(value) }
    var previousValue by remember { mutableIntStateOf(value) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(value) {
        if (value != animatedValue) {
            previousValue = animatedValue
            animatedValue = value
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
            )
        }
    }

    val formattedCurrent = animatedValue.toString()
    val formattedPrevious = previousValue.toString()

    val cardBrush = Brush.verticalGradient(listOf(theme.cardGradientTop, theme.cardGradientBottom))

    val stackCount = 3
    val stackOffset = 1.5.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentSize()
    ) {
        Box(
            modifier = Modifier
                .width(width * HOLDER_WIDTH_RATIO)
                .height(height * 0.15f)
                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                .background(Color(0xFF202020))
        )

        Box(modifier = Modifier.size(width, height)) {

            if (showShadows) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = 8.dp.toPx() }
                        .shadow(16.dp, RoundedCornerShape(CardCornerRadius), false, Color.Black.copy(alpha=0.5f), Color.Black)
                )
            }

            Box(modifier = Modifier.fillMaxSize().zIndex(-1f)) {
                for (i in 1..stackCount) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height / 2)
                            .align(Alignment.TopCenter)
                            .offset(y = -stackOffset * i)
                            .clip(RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius))
                            .background(cardBrush)
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f * i)))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Black.copy(alpha = 0.3f)).align(Alignment.TopCenter))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height / 2)
                            .align(Alignment.BottomCenter)
                            .offset(y = stackOffset * i)
                            .clip(RoundedCornerShape(bottomStart = CardCornerRadius, bottomEnd = CardCornerRadius))
                            .background(cardBrush)
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f * i)))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Black.copy(alpha = 0.3f)).align(Alignment.BottomCenter))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(CardCornerRadius))) {
                NumberTile(formattedCurrent, TileType.BOTTOM, fontSize, theme)
                if (rotation.value < 90f) {
                    NumberTile(formattedPrevious, TileType.TOP, fontSize, theme)
                } else {
                    NumberTile(formattedCurrent, TileType.TOP, fontSize, theme)
                }

                val flipRotation = if (rotation.value < 90f) rotation.value else rotation.value - 180f

                if (rotation.value > 0f && rotation.value < 180f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 32.dp.toPx()
                                rotationX = -flipRotation
                            }
                            .zIndex(2f)
                    ) {
                        if (rotation.value < 90f) {
                            NumberTile(formattedPrevious, TileType.TOP, fontSize, theme)
                        } else {
                            NumberTile(formattedCurrent, TileType.BOTTOM, fontSize, theme)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(separatorColor)
                        .align(Alignment.Center)
                        .zIndex(3f)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(width * HOLDER_WIDTH_RATIO)
                .height(height * 0.15f)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(Color(0xFF202020))
        )
    }
}

enum class TileType { TOP, BOTTOM, FULL }

@Composable
fun NumberTile(
    number: String,
    type: TileType,
    fontSize: TextUnit,
    theme: ThemeColors,
    modifier: Modifier = Modifier
) {
    val shape = remember(type) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                return when (type) {
                    TileType.TOP -> Outline.Rectangle(Rect(0f, 0f, size.width, size.height / 2))
                    TileType.BOTTOM -> Outline.Rectangle(Rect(0f, size.height / 2, size.width, size.height))
                    TileType.FULL -> Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
                }
            }
        }
    }

    val backgroundBrush = remember(theme) {
        Brush.verticalGradient(listOf(theme.cardGradientTop, theme.cardGradientBottom))
    }
    val shineBrush = remember(theme) {
        Brush.linearGradient(
            colors = listOf(theme.shineColor, Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier.fillMaxSize().clip(shape).background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().background(shineBrush))
        Text(
            text = number,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = theme.text,
            textAlign = TextAlign.Center
        )
    }
}

fun Color.luminance(): Float = (0.299 * red + 0.587 * green + 0.114 * blue).toFloat()

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun hideSystemBars(context: Context) {
    val activity = context.findActivity() ?: return
    val window = activity.window
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
}