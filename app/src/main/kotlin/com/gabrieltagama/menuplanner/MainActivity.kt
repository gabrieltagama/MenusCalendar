package com.gabrieltagama.menuplanner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.core.ui.theme.isDarkTheme
import com.gabrieltagama.menuplanner.ui.MenuPlannerAppRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the whole Compose UI, drawn edge to edge. It applies the theme mode
 * chosen in the settings and keeps the system bar icons readable when it differs from the device.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme }
                )
                onDispose {}
            }
            MenuPlannerTheme(darkTheme = darkTheme) {
                MenuPlannerAppRoot(viewModel = viewModel)
            }
        }
    }

    private companion object {
        val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
