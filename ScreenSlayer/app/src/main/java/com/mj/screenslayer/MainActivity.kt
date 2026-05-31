package com.mj.screenslayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.mj.screenslayer.ui.theme.ScreenSlayerTheme
import com.mj.screenslayer.viewmodel.MainViewModel
import com.mj.screenslayer.viewmodel.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.Light  -> false
                ThemeMode.Dark   -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            ScreenSlayerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, mainViewModel = vm)
            }
        }
    }
}
