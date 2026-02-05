package com.example.flipclock

import android.content.res.Configuration
import android.graphics.Color.colorToHSV
import android.graphics.Color.parseColor
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    currentBgColor: Int,
    palette: List<Int>,
    onColorSelected: (Int) -> Unit,
    onPaletteEdited: (Int, Int) -> Unit,
    isAutoBrightness: Boolean,
    onAutoBrightnessChanged: (Boolean) -> Unit,
    brightness: Float,
    onBrightnessChanged: (Float) -> Unit,
    luxString: String,
    showShadows: Boolean,
    onShadowsChanged: (Boolean) -> Unit,
    bgImageUri: String?,
    onBgImageSelected: (Uri?) -> Unit,
    bgOpacity: Float,
    onBgOpacityChanged: (Float) -> Unit,
    bgBlur: Float,
    onBgBlurChanged: (Float) -> Unit,
    bgStretch: Boolean,
    onBgStretchChanged: (Boolean) -> Unit,
    cardColors: List<Int>, // Принимаем список извне
    onCardPaletteEdited: (Int, Int) -> Unit, // Callback для редактирования
    currentCardColor: Int,
    onCardColorChanged: (Int) -> Unit,
    isDefaultLauncher: Boolean, // НОВОЕ: статус лаунчера
    onLauncherToggle: () -> Unit // НОВОЕ: переключение лаунчера
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerMode by remember { mutableStateOf<PickerMode>(PickerMode.Background) }
    var colorPickerIndex by remember { mutableIntStateOf(-1) }
    var colorPickerInitialColor by remember { mutableIntStateOf(0) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onBgImageSelected(uri)
    }

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
        scrollState.scrollTo(0)
    }

    if (showColorPicker) {
        HexColorPickerDialog(
            initialColor = colorPickerInitialColor,
            onDismiss = { showColorPicker = false },
            onColorConfirmed = { newColor ->
                if (colorPickerMode == PickerMode.Background) {
                    onPaletteEdited(colorPickerIndex, newColor)
                } else {
                    // ИСПРАВЛЕНИЕ: Обновляем палитру карточек по индексу
                    if (colorPickerIndex != -1) {
                        onCardPaletteEdited(colorPickerIndex, newColor)
                    } else {
                        onCardColorChanged(newColor)
                    }
                }
                showColorPicker = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .heightIn(max = 600.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Настройки", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        // Увеличенный крестик закрытия (+25%)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color(0xFF2C2C2E), CircleShape)
                                .size(30.dp) // Было 24dp, стало 30dp
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) // Иконка тоже чуть больше
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        Box(Modifier.size(1.dp).focusRequester(focusRequester).focusable())

                        SettingsSectionTitle("Оформление флипов")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreviewCard("Светлые", themeMode == ThemeMode.LIGHT, Color(0xFFE0E0E0), Color.White, { onThemeChanged(ThemeMode.LIGHT) }, Modifier.weight(1f))
                            ThemePreviewCard("Темные", themeMode == ThemeMode.DARK, Color(0xFF2C2C2E), Color(0xFF1C1C1E), { onThemeChanged(ThemeMode.DARK) }, Modifier.weight(1f))
                            ThemePreviewCard("Свой цвет", themeMode == ThemeMode.COLOR, Color(0xFFE91E63), Color(0xFF880E4F), { onThemeChanged(ThemeMode.COLOR) }, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)), shape = RoundedCornerShape(8.dp)) {
                            Column (modifier = Modifier.padding(12.dp)){
                                // Авто-тема Switch
                                SettingsOptionRow(
                                    "Как в системе",
                                    {
                                        if (themeMode == ThemeMode.AUTO) onThemeChanged(ThemeMode.DARK)
                                        else onThemeChanged(ThemeMode.AUTO)
                                    }
                                ) {
                                    Switch(
                                        checked = themeMode == ThemeMode.AUTO,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) onThemeChanged(ThemeMode.AUTO)
                                            else onThemeChanged(ThemeMode.DARK)
                                        },
                                        modifier = Modifier.scale(0.6f),
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF448AFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF48484A))
                                    )
                                }

                                if (themeMode == ThemeMode.COLOR) {
                                    Divider(color = Color.Gray.copy(0.2f), thickness = 0.5.dp)
                                    Column {

                                        Text("Цвет флипов (удерживайте для выбора)", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            cardColors.forEachIndexed { index, colorInt -> // Добавляем index
                                                ColorCircle(
                                                    color = Color(colorInt),
                                                    isSelected = currentCardColor == colorInt,
                                                    onClick = { onCardColorChanged(colorInt) },
                                                    onLongClick = {
                                                        colorPickerMode = PickerMode.Card
                                                        colorPickerIndex = index // ВАЖНО: Запоминаем индекс
                                                        colorPickerInitialColor = colorInt
                                                        showColorPicker = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Divider(color = Color.Gray.copy(0.2f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp), )
                                // ИЗМЕНЕНИЕ: Тень - Switch, название "Тень"
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsOptionRow("Тень", { onShadowsChanged(!showShadows) }) {
                                    Switch(checked = showShadows, onCheckedChange = onShadowsChanged, modifier = Modifier.scale(0.6f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF448AFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF48484A)))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsSectionTitle("Фон")
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (bgImageUri != null) {
                                    Text("Изображение установлено", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom=8.dp))

                                    LinearLabeledSlider(
                                        title = "Прозрачность",
                                        value = bgOpacity,
                                        onValueChange = onBgOpacityChanged
                                    )

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        LinearLabeledSlider(
                                            title = "Размытие",
                                            value = bgBlur,
                                            onValueChange = onBgBlurChanged
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color.Gray.copy(0.2f), thickness = 0.5.dp)

                                    // Растянуть на экран - Switch
                                    SettingsOptionRow(
                                        "Растянуть",
                                        { onBgStretchChanged(!bgStretch) }
                                    ) {
                                        Switch(
                                            checked = bgStretch,
                                            onCheckedChange = onBgStretchChanged,
                                            modifier = Modifier.scale(0.6f),
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF448AFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF48484A))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Button(
                                            onClick = { onBgImageSelected(null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(32.dp).weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("Удалить", fontSize = 11.sp) }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Button(
                                            onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(32.dp).weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("Сменить", fontSize = 11.sp) }
                                    }

                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        palette.forEachIndexed { index, colorInt ->
                                            ColorCircle(Color(colorInt), currentBgColor == colorInt, { onColorSelected(colorInt) }, {
                                                colorPickerMode = PickerMode.Background
                                                colorPickerIndex = index
                                                colorPickerInitialColor = colorInt
                                                showColorPicker = true
                                            })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Выбрать изображение", color = Color.LightGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsSectionTitle("Яркость", paddingBottom = 0.dp)
                            if (isAutoBrightness) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("($luxString)", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                LinearLabeledSlider(
                                    title = null,
                                    value = brightness,
                                    onValueChange = onBrightnessChanged
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                                SettingsOptionRow("Адаптивная яркость", { onAutoBrightnessChanged(!isAutoBrightness) }) {
                                    Switch(checked = isAutoBrightness, onCheckedChange = onAutoBrightnessChanged, modifier = Modifier.scale(0.6f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF448AFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF48484A)))
                                }
                            }
                        }
                        // === НОВАЯ СЕКЦИЯ: ЛАУНЧЕР ===
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsSectionTitle("Система", paddingBottom = 4.dp)
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                SettingsOptionRow(
                                    label = "Использовать как лаунчер",
                                    onClick = onLauncherToggle
                                ) {
                                    Switch(
                                        checked = isDefaultLauncher,
                                        onCheckedChange = { onLauncherToggle() },
                                        modifier = Modifier.scale(0.6f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF448AFF),
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFF48484A)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

enum class PickerMode { Background, Card }

// === КОМПОНЕНТЫ ВЫБОРА ЦВЕТА ===

@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    isLandscape: Boolean
) {
    val hsvState = remember {
        val hsv = FloatArray(3)
        colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(Triple(hsv[0], hsv[1], hsv[2]))
    }

    LaunchedEffect(initialColor) {
        val currentHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), currentHsv)
        if (Math.abs(hsvState.value.first - currentHsv[0]) > 1f ||
            Math.abs(hsvState.value.second - currentHsv[1]) > 0.01f ||
            Math.abs(hsvState.value.third - currentHsv[2]) > 0.01f) {
            hsvState.value = Triple(currentHsv[0], currentHsv[1], currentHsv[2])
        }
    }

    val (hue, sat, `val`) = hsvState.value

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = if (isLandscape) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().aspectRatio(1.3f)) {
            SaturationValuePanel(
                hue = hue,
                saturation = sat,
                value = `val`,
                onSatValChanged = { newSat, newVal ->
                    hsvState.value = Triple(hue, newSat, newVal)
                    onColorChanged(Color.hsv(hue, newSat, newVal))
                }
            )
        }

        Spacer(modifier = Modifier.height(if(isLandscape) 8.dp else 12.dp))

        HueSlider(
            hue = hue,
            onHueChanged = { newHue ->
                hsvState.value = Triple(newHue, sat, `val`)
                onColorChanged(Color.hsv(newHue, sat, `val`))
            }
        )
    }
}

@Composable
fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChanged: (Float, Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(8.dp))
    ) {
        val baseColor = Color.hsv(hue, 1f, 1f)

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                    val newVal = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatValChanged(newSat, newVal)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newSat = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newVal = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSatValChanged(newSat, newVal)
                }
            }
        ) {
            drawRect(color = baseColor)
            drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
            drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))

            val pxX = saturation * size.width
            val pxY = (1f - value) * size.height
            drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(pxX, pxY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.Black, radius = 8.dp.toPx(), center = Offset(pxX, pxY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(12.dp))
    ) {
        val rainbowColors = listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newHue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                    onHueChanged(newHue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newHue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                    onHueChanged(newHue)
                }
            }
        ) {
            drawRect(brush = Brush.horizontalGradient(colors = rainbowColors))

            val pxX = (hue / 360f) * size.width
            drawLine(
                color = Color.White,
                start = Offset(pxX, 0f),
                end = Offset(pxX, size.height),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(pxX, size.height / 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun HexColorPickerDialog(initialColor: Int, onDismiss: () -> Unit, onColorConfirmed: (Int) -> Unit) {
    var hexText by remember { mutableStateOf(String.format("%06X", (0xFFFFFF and initialColor))) }
    var currentColor by remember { mutableStateOf(Color(initialColor)) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(if (isLandscape) 0.9f else Float.NaN)
                    .wrapContentHeight(if(isLandscape) Alignment.CenterVertically else Alignment.Top)
                    .heightIn(max = 600.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(if(isLandscape) 12.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Выбор цвета", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(if(isLandscape) 8.dp else 16.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                                HsvColorPicker(
                                    initialColor = currentColor,
                                    onColorChanged = { newColor ->
                                        currentColor = newColor
                                        hexText = "%06X".format(newColor.toArgb() and 0xFFFFFF)
                                    },
                                    isLandscape = true
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.4f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(currentColor)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                )

                                OutlinedTextField(
                                    value = hexText,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6) {
                                            val filtered = newValue.uppercase().replace(Regex("[^0-9A-F]"), "")
                                            hexText = filtered
                                            try { if (filtered.length == 6) currentColor = Color(parseColor("#$filtered")) } catch(e: Exception){}
                                        }
                                    },
                                    label = { Text("HEX", fontSize = 10.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.Gray, focusedBorderColor = Color(0xFF448AFF), unfocusedBorderColor = Color.Gray),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { try { onColorConfirmed(currentColor.toArgb()) } catch(e: Exception){} },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF448AFF)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).fillMaxWidth()
                                    ) { Text("OK", fontSize = 12.sp) }
                                }
                            }
                        }
                    } else {
                        HsvColorPicker(
                            initialColor = currentColor,
                            onColorChanged = { newColor ->
                                currentColor = newColor
                                hexText = "%06X".format(newColor.toArgb() and 0xFFFFFF)
                            },
                            isLandscape = false
                        )

                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(currentColor).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)))
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { newValue ->
                                if (newValue.length <= 6) {
                                    val filtered = newValue.uppercase().replace(Regex("[^0-9A-F]"), "")
                                    hexText = filtered
                                    try { if (filtered.length == 6) currentColor = Color(parseColor("#$filtered")) } catch(e: Exception){}
                                }
                            },
                            label = { Text("HEX код", fontSize = 12.sp) }, singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.Gray, focusedBorderColor = Color(0xFF448AFF), unfocusedBorderColor = Color.Gray),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Gray) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { try { onColorConfirmed(currentColor.toArgb()) } catch(e: Exception){} },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF448AFF)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Применить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinearLabeledSlider(title: String? = null, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        if (title != null) Text(title, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(20.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF448AFF), inactiveTrackColor = Color.Gray.copy(alpha = 0.3f))
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = listOf("0", "25", "50", "75", "100")
            labels.forEach { label ->
                // Сделали цифры кликабельными
                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(20.dp)
                        .clickable {
                            onValueChange(label.toFloat() / 100f)
                        }
                )
            }
        }
    }
}

@Composable
fun SettingsOptionRow(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }.padding(vertical = 4.dp, horizontal = 4.dp).height(32.dp)) {
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
fun SettingsSectionTitle(text: String, paddingBottom: Dp = 4.dp) {
    Text(text, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = paddingBottom, start = 2.dp))
}

@Composable
fun ThemePreviewCard(title: String, isSelected: Boolean, colorPrimary: Color, colorAccent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.height(50.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) Color(0xFF448AFF) else Color.Transparent, RoundedCornerShape(8.dp)).background(colorPrimary).clickable { onClick() }) {
            Box(Modifier.align(Alignment.Center).size(20.dp).clip(RoundedCornerShape(4.dp)).background(colorAccent))
            if (isSelected) Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(12.dp).background(Color(0xFF448AFF), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(8.dp)) }
        }
        Spacer(Modifier.height(2.dp))
        Text(title, color = if (isSelected) Color(0xFF448AFF) else Color.Gray, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp)) {
        if (isSelected) Box(Modifier.size(30.dp).border(1.5.dp, Color(0xFF448AFF).copy(0.7f), CircleShape))
        Box(Modifier.size(22.dp).clip(CircleShape).background(color).combinedClickable(onClick = onClick, onLongClick = onLongClick))
    }
}