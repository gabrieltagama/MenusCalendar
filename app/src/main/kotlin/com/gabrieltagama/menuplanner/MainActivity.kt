package com.gabrieltagama.menuplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.ui.MenuPlannerAppRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the whole Compose UI, drawn edge to edge.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MenuPlannerTheme {
                MenuPlannerAppRoot()
            }
        }
    }
}
