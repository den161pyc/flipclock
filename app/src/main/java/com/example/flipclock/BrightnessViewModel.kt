package com.example.flipclock

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

private val Context.dataStore by preferencesDataStore(name = "settings_v6")

// Заменили GLASS на COLOR
enum class ThemeMode { LIGHT, DARK, AUTO, COLOR }

class BrightnessViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    // Ключи
    private val KEY_IS_AUTO = booleanPreferencesKey("is_auto")
    private val KEY_MANUAL = floatPreferencesKey("manual_val")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_BG_COLOR = intPreferencesKey("bg_color")
    private val KEY_SHOW_SHADOW = booleanPreferencesKey("shadows")
    private val KEY_BG_IMAGE_URI = stringPreferencesKey("bg_image_uri")
    private val KEY_BG_OPACITY = floatPreferencesKey("bg_opacity")
    private val KEY_BG_BLUR = floatPreferencesKey("bg_blur")
    private val KEY_BG_STRETCH = booleanPreferencesKey("bg_stretch")
    // Новый ключ для цвета карточек
    private val KEY_CARD_COLOR = intPreferencesKey("card_color")

    private val KEYS_PALETTE = listOf(
        intPreferencesKey("p0"), intPreferencesKey("p1"), intPreferencesKey("p2"),
        intPreferencesKey("p3"), intPreferencesKey("p4")
    )
    // ДОБАВИТЬ: Ключи для палитры карточек
    private val KEYS_CARD_PALETTE = listOf(
        intPreferencesKey("cp0"), intPreferencesKey("cp1"), intPreferencesKey("cp2"),
        intPreferencesKey("cp3"), intPreferencesKey("cp4")
    )
    private val DEFAULT_PALETTE = listOf(
        0xFF448AFF.toInt(), 0xFFFFC107.toInt(), 0xFF4CAF50.toInt(),
        0xFF9C27B0.toInt(), 0xFFE91E63.toInt()
    )
    // ДОБАВИТЬ: Палитра по умолчанию
    private val DEFAULT_CARD_PALETTE = listOf(
        0xFFFFFFFF.toInt(),
        0xFF3A3A3A.toInt(),
        0xFF1E88E5.toInt(),
        0xFF43A047.toInt(),
        0xFFE53935.toInt()
    )
    private var currentLuxValue: Float = 0f
    private val _sensorLuxString = MutableStateFlow("Lux: --")
    val sensorLuxString = _sensorLuxString.asStateFlow()

    // Состояния
    private val _isAutoBrightness = MutableStateFlow(false)
    val isAutoBrightness = _isAutoBrightness.asStateFlow()

    private val _manualBrightness = MutableStateFlow(0.5f)
    val manualBrightness = _manualBrightness.asStateFlow()

    private val _currentScreenBrightness = MutableStateFlow(0.5f)
    val currentScreenBrightness = _currentScreenBrightness.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode = _themeMode.asStateFlow()

    private val _backgroundColor = MutableStateFlow(0xFF121212.toInt())
    val backgroundColor = _backgroundColor.asStateFlow()

    private val _showShadows = MutableStateFlow(true)
    val showShadows = _showShadows.asStateFlow()

    private val _palette = MutableStateFlow(DEFAULT_PALETTE)
    val palette = _palette.asStateFlow()

    private val _cardPalette = MutableStateFlow(DEFAULT_CARD_PALETTE)
    val cardPalette = _cardPalette.asStateFlow()

    private val _bgImageUri = MutableStateFlow<String?>(null)
    val bgImageUri = _bgImageUri.asStateFlow()

    private val _bgOpacity = MutableStateFlow(0.5f)
    val bgOpacity = _bgOpacity.asStateFlow()

    private val _bgBlur = MutableStateFlow(0f)
    val bgBlur = _bgBlur.asStateFlow()

    private val _bgStretch = MutableStateFlow(true)
    val bgStretch = _bgStretch.asStateFlow()

    // Цвет карточек (по умолчанию темно-серый, как в Dark теме)
    private val _cardColor = MutableStateFlow(0xFF3A3A3A.toInt())
    val cardColor = _cardColor.asStateFlow()

    // Батарея
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging = _isCharging.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        viewModelScope.launch {
            getApplication<Application>().applicationContext.dataStore.data.collect { prefs ->
                _isAutoBrightness.value = prefs[KEY_IS_AUTO] ?: false
                _manualBrightness.value = prefs[KEY_MANUAL] ?: 0.5f
                val modeStr = prefs[KEY_THEME] ?: ThemeMode.DARK.name
                _themeMode.value = try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.DARK }
                _backgroundColor.value = prefs[KEY_BG_COLOR] ?: 0xFF121212.toInt()
                _showShadows.value = prefs[KEY_SHOW_SHADOW] ?: true
                _bgImageUri.value = prefs[KEY_BG_IMAGE_URI]
                _bgOpacity.value = prefs[KEY_BG_OPACITY] ?: 0.5f
                _bgBlur.value = prefs[KEY_BG_BLUR] ?: 0f
                _bgStretch.value = prefs[KEY_BG_STRETCH] ?: true
                _cardColor.value = prefs[KEY_CARD_COLOR] ?: 0xFF3A3A3A.toInt()

                val loadedPalette = KEYS_PALETTE.mapIndexed { index, key ->
                    prefs[key] ?: DEFAULT_PALETTE[index]
                }
                _palette.value = loadedPalette
                if (_isAutoBrightness.value && sensorManager != null) registerSensor()

                // ДОБАВИТЬ: Загрузка палитры карточек
                val loadedCardPalette = KEYS_CARD_PALETTE.mapIndexed { index, key ->
                    prefs[key] ?: DEFAULT_CARD_PALETTE[index]
                }
                _cardPalette.value = loadedCardPalette

                recalculateBrightness()
            }
        }
    }

    fun registerBatteryReceiver(context: Context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun unregisterBatteryReceiver(context: Context) {
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) { e.printStackTrace() }
    }

    fun setBgImage(uri: Uri?) {
        val uriString = uri?.toString()
        _bgImageUri.value = uriString
        if (uri != null) {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) { e.printStackTrace() }
        }
        viewModelScope.launch {
            getApplication<Application>().applicationContext.dataStore.edit {
                if (uriString != null) it[KEY_BG_IMAGE_URI] = uriString else it.remove(KEY_BG_IMAGE_URI)
            }
        }
    }
    fun setBgOpacity(value: Float) { _bgOpacity.value = value; saveFloat(KEY_BG_OPACITY, value) }
    fun setBgBlur(value: Float) { _bgBlur.value = value; saveFloat(KEY_BG_BLUR, value) }
    fun setBgStretch(enabled: Boolean) { _bgStretch.value = enabled; saveBoolean(KEY_BG_STRETCH, enabled) }
    // Функция установки цвета карточек
    fun setCardColor(color: Int) {
        _cardColor.value = color
        saveInt(KEY_CARD_COLOR, color)
    }
    // ДОБАВИТЬ: Функция обновления цвета в палитре карточек
    fun updateCardPaletteColor(index: Int, newColor: Int) {
        val current = _cardPalette.value.toMutableList()
        if (index in current.indices) {
            current[index] = newColor
            _cardPalette.value = current
            setCardColor(newColor) // Сразу применяем выбранный цвет
            viewModelScope.launch {
                getApplication<Application>().applicationContext.dataStore.edit { prefs ->
                    prefs[KEYS_CARD_PALETTE[index]] = newColor
                }
            }
        }
    }
    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode; saveString(KEY_THEME, mode.name) }
    fun setBackgroundColor(color: Int) { _backgroundColor.value = color; saveInt(KEY_BG_COLOR, color) }
    fun toggleShadows(enabled: Boolean) { _showShadows.value = enabled; saveBoolean(KEY_SHOW_SHADOW, enabled) }

    fun updatePaletteColor(index: Int, newColor: Int) {
        val current = _palette.value.toMutableList()
        if (index in current.indices) {
            current[index] = newColor
            _palette.value = current
            setBackgroundColor(newColor)
            viewModelScope.launch { getApplication<Application>().applicationContext.dataStore.edit { prefs -> prefs[KEYS_PALETTE[index]] = newColor } }
        }
    }

    fun toggleAutoBrightness(enabled: Boolean) {
        _isAutoBrightness.value = enabled
        if (enabled) registerSensor() else { unregisterSensor(); _currentScreenBrightness.value = _manualBrightness.value }
        recalculateBrightness()
        saveBoolean(KEY_IS_AUTO, enabled)
    }

    fun setManualBrightness(value: Float) {
        _manualBrightness.value = value
        recalculateBrightness()
        saveFloat(KEY_MANUAL, value)
    }

    fun initSensor(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (_isAutoBrightness.value) registerSensor()
        }
    }

    private fun registerSensor() { sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI) }
    private fun unregisterSensor() { sensorManager?.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentLuxValue = event.values[0]
            _sensorLuxString.value = "Lux: ${currentLuxValue.toInt()}"
            recalculateBrightness()
        }
    }

    private fun recalculateBrightness() {
        if (_isAutoBrightness.value) {
            // 1. Базовая яркость от датчика.
            // Увеличили делитель до 3000f, чтобы растянуть диапазон.
            // Теперь 100% яркости от датчика будет только при очень ярком свете (3000 люкс).
            // Это дает запас для работы вашего "смещения" вверх.
            val sensorBrightness = min(1f, currentLuxValue / 3000f)

            // 2. Вычисляем смещение (Offset).
            // Слайдер (manualBrightness) выдает от 0.0 до 1.0.
            // Мы превращаем это в диапазон от -0.5 до +0.5.
            // 0.5 на слайдере = 0 смещения (как решил датчик).
            val offset = _manualBrightness.value - 0.5f

            // 3. Складываем и ограничиваем результат (не меньше 1% и не больше 100%)
            val finalBrightness = (sensorBrightness + offset).coerceIn(0.01f, 1f)

            _currentScreenBrightness.value = finalBrightness
        } else {
            // В ручном режиме слайдер работает как обычная регулировка (0..100%)
            _currentScreenBrightness.value = _manualBrightness.value.coerceAtLeast(0.01f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onCleared() { super.onCleared(); unregisterSensor() }

    private fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) { viewModelScope.launch { getApplication<Application>().applicationContext.dataStore.edit { it[key] = value } } }
    private fun saveFloat(key: Preferences.Key<Float>, value: Float) { viewModelScope.launch { getApplication<Application>().applicationContext.dataStore.edit { it[key] = value } } }
    private fun saveInt(key: Preferences.Key<Int>, value: Int) { viewModelScope.launch { getApplication<Application>().applicationContext.dataStore.edit { it[key] = value } } }
    private fun saveString(key: Preferences.Key<String>, value: String) { viewModelScope.launch { getApplication<Application>().applicationContext.dataStore.edit { it[key] = value } } }
}