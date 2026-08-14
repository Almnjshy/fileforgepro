package com.fileforge.pro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fileforge.pro.app.ui.FileForgeApp
import com.fileforge.pro.core.ui.theme.FileForgeTheme
import com.fileforge.pro.core.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileForgeTheme(themeMode = ThemeMode.DARK) {
                FileForgeApp()
            }
        }
    }
}
