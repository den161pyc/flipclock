package com.example.flipclock

import android.os.Bundle
import android.view.WindowManager // <--- Убедитесь, что этот импорт есть
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BrightnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Включаем постоянную активность экрана (ЧТОБЫ НЕ ГАС)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Настройка полного экрана
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 3. Слушатель изменения яркости
        lifecycleScope.launch {
            viewModel.currentScreenBrightness.collect { brightness ->
                val layoutParams = window.attributes
                layoutParams.screenBrightness = brightness
                window.attributes = layoutParams
            }
        }

        setContent {
            MaterialTheme {
                Surface {
                    FlipClockScreen(viewModel = viewModel)
                }
            }
        }
    }
}