package com.al3rted.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.al3rted.app.data.Alert
import com.al3rted.app.ui.*

class MainActivity : ComponentActivity() {

    private val viewModel: AlertViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFullScreenIntentPermission()
        requestMediaAudioPermission()
        setContent {
            Al3rtedThemed(viewModel)
        }
    }

    private fun requestMediaAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 42)
        }
    }

    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }
}

@Composable
private fun Al3rtedThemed(viewModel: AlertViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(AppTheme.fromOrdinal(prefs.getInt("theme", 0))) }
    val rainbowHue = rememberRainbowHue()
    val colors = remember(theme, rainbowHue) { themeColors(theme, rainbowHue) }

    MaterialTheme(colorScheme = colors) {
        Surface {
            Al3rtedApp(
                viewModel = viewModel,
                currentTheme = theme,
                onThemeChange = { t ->
                    theme = t
                    prefs.edit().putInt("theme", t.ordinal).apply()
                }
            )
        }
    }
}

private sealed interface Screen {
    data object List : Screen
    data class Edit(val alert: Alert?) : Screen
    data object Settings : Screen
}

@Composable
private fun Al3rtedApp(
    viewModel: AlertViewModel,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    var screen by remember { mutableStateOf<Screen>(Screen.List) }

    when (val s = screen) {
        is Screen.List -> AlertListScreen(
            viewModel = viewModel,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onAdd = { screen = Screen.Edit(null) },
            onEdit = { screen = Screen.Edit(it) },
            onSettings = { screen = Screen.Settings }
        )
        is Screen.Edit -> AlertEditScreen(
            existing = s.alert,
            viewModel = viewModel,
            onBack = { screen = Screen.List }
        )
        is Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.List }
        )
    }
}
