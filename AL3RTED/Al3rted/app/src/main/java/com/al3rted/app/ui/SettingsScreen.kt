package com.al3rted.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.al3rted.app.data.AlertType
import com.al3rted.app.data.AppSettings
import com.al3rted.app.data.VibrationPattern
import kotlin.math.roundToInt

private val SNOOZE_OPTIONS = listOf(1, 5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE) }

    var settings by remember { mutableStateOf(AppSettings.load(context)) }
    var use24h   by remember { mutableStateOf(prefs.getBoolean("use24h", false)) }

    fun save(new: AppSettings) {
        settings = new
        AppSettings.save(context, new)
    }

    val soundName = remember(settings.soundUri) { resolveSoundName(context, settings.soundUri) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            save(settings.copy(soundUri = uri?.toString() ?: ""))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── SOUND ──────────────────────────────────────────────
            SettingsCard("Sound") {
                // Picker
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(soundName, style = MaterialTheme.typography.bodyLarge)
                        Text("Current alert sound",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert Sound")
                            if (settings.soundUri.isNotEmpty())
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(settings.soundUri))
                        }
                        ringtoneLauncher.launch(intent)
                    }) { Text("Change") }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Volume
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Volume", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${settings.volume}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.volume > 100) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.volume.toFloat(),
                    onValueChange = { save(settings.copy(volume = it.roundToInt())) },
                    valueRange = 0f..150f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = if (settings.volume > 100) MaterialTheme.colorScheme.error
                                          else MaterialTheme.colorScheme.primary
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mute", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Max", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+50% Boost", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
                if (settings.volume > 100) {
                    Text(
                        "Boosted +${settings.volume - 100}% past system max (LoudnessEnhancer)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Fade In
                SettingRow(
                    label = "Fade In",
                    description = "Gradually increase from silent over 30 seconds"
                ) {
                    Switch(checked = settings.fadeIn,
                        onCheckedChange = { save(settings.copy(fadeIn = it)) })
                }
            }

            // ── ALERT TYPE ─────────────────────────────────────────
            SettingsCard("Alert Type") {
                AlertType.entries.forEach { type ->
                    RadioRow(
                        label = type.label,
                        description = type.description,
                        selected = settings.alertType == type,
                        onClick = { save(settings.copy(alertType = type)) }
                    )
                }
            }

            // ── VIBRATION PATTERN ──────────────────────────────────
            SettingsCard("Vibration Pattern") {
                val hasVibration = settings.alertType == AlertType.SOUND_AND_VIBRATION ||
                                   settings.alertType == AlertType.VIBRATION_ONLY
                if (!hasVibration) {
                    Text(
                        "Enable vibration in Alert Type to use patterns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VibrationPattern.entries.forEach { pattern ->
                    RadioRow(
                        label = pattern.label,
                        description = pattern.description,
                        selected = settings.vibrationPattern == pattern,
                        onClick = { save(settings.copy(vibrationPattern = pattern)) },
                        enabled = hasVibration
                    )
                }
            }

            // ── SNOOZE ─────────────────────────────────────────────
            SettingsCard("Snooze Duration") {
                Text("How long to snooze when tapped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                // Two rows of 3 chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SNOOZE_OPTIONS.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { mins ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.snoozeDuration == mins,
                                    onClick  = { save(settings.copy(snoozeDuration = mins)) },
                                    label    = {
                                        Text(
                                            if (mins == 1) "1 min" else "$mins min",
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── FLASHLIGHT ─────────────────────────────────────────
            SettingsCard("Flashlight") {
                SettingRow(
                    label = "Strobe Flashlight",
                    description = "Flash the camera LED during the alert"
                ) {
                    Switch(checked = settings.flashlight,
                        onCheckedChange = { save(settings.copy(flashlight = it)) })
                }
            }

            // ── GENERAL ────────────────────────────────────────────
            SettingsCard("General") {
                SettingRow(
                    label = "24-Hour Time",
                    description = "Show time in 24h format (14:30 instead of 2:30 PM)"
                ) {
                    Switch(
                        checked = use24h,
                        onCheckedChange = {
                            use24h = it
                            prefs.edit().putBoolean("use24h", it).apply()
                        }
                    )
                }
            }
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    content: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
private fun RadioRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun resolveSoundName(context: Context, uri: String): String {
    if (uri.isEmpty()) return "Default Alarm"
    return try {
        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) ?: "Unknown Sound"
    } catch (_: Exception) { "Unknown Sound" }
}
