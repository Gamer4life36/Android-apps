package com.mj.screenslayer.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mj.screenslayer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LockScreenSettingsScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val screenTriggerEnabled by viewModel.screenTriggerEnabled.collectAsStateWithLifecycle()
    val shakeEnabled         by viewModel.shakeEnabled.collectAsStateWithLifecycle()
    val shakeSensitivity     by viewModel.shakeSensitivity.collectAsStateWithLifecycle()
    val shuffleMode          by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Write permission ──────────────────────────────────────────────────────
    var canWrite by remember { mutableStateOf(Settings.System.canWrite(context)) }

    // ── Screen timeout ────────────────────────────────────────────────────────
    var currentTimeout by remember {
        mutableIntStateOf(
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            }.getOrDefault(60_000)
        )
    }

    // ── Tap to wake ───────────────────────────────────────────────────────────
    var tapToWake by remember {
        mutableStateOf(
            runCatching {
                Settings.System.getInt(context.contentResolver, "tap_to_wake", 0) == 1
            }.getOrDefault(false)
        )
    }

    // ── Lock screen notifications (read-only, deep link to change) ────────────
    val showNotifications = remember {
        runCatching {
            Settings.Secure.getInt(
                context.contentResolver,
                "lock_screen_show_notifications", 1
            ) == 1
        }.getOrDefault(true)
    }

    // ── Screen secure state ───────────────────────────────────────────────────
    val isDeviceSecure = remember {
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    // Re-check on resume from system settings
    LifecycleResumeEffect(Unit) {
        canWrite = Settings.System.canWrite(context)
        if (canWrite) {
            currentTimeout = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            }.getOrDefault(60_000)
            tapToWake = runCatching {
                Settings.System.getInt(context.contentResolver, "tap_to_wake", 0) == 1
            }.getOrDefault(false)
        }
        onPauseOrDispose {}
    }

    val timeoutOptions = listOf(
        15_000        to "15 sec",
        30_000        to "30 sec",
        60_000        to "1 min",
        120_000       to "2 min",
        300_000       to "5 min",
        600_000       to "10 min",
        1_800_000     to "30 min",
        Int.MAX_VALUE to "Never"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Lock Screen") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── WALLPAPER ─────────────────────────────────────────────────────
            SectionLabel("WALLPAPER")
            Spacer(Modifier.height(4.dp))

            SettingsRow(
                icon     = Icons.Default.Wallpaper,
                title    = "Wallpaper",
                subtitle = "Manage lock screen wallpaper slots",
                onClick  = { navController.popBackStack() }
            )
            SettingsRow(
                icon     = Icons.Default.Tune,
                title    = "Wallpaper appearance",
                subtitle = "Scale, dim, and colour adjustments",
                onClick  = { navController.navigate("wallpapersettings") }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── ROTATION ──────────────────────────────────────────────────────
            SectionLabel("ROTATION")
            Spacer(Modifier.height(4.dp))

            // Change on unlock
            ToggleRow(
                icon     = Icons.Default.LockOpen,
                title    = "Change on unlock",
                subtitle = "Apply next wallpaper each time you unlock",
                checked  = screenTriggerEnabled,
                onChange = { viewModel.setScreenTriggerEnabled(it) }
            )

            // Shake to change
            ToggleRow(
                icon     = Icons.Default.Vibration,
                title    = "Shake to change",
                subtitle = "Shake your phone to switch to the next wallpaper",
                checked  = shakeEnabled,
                onChange = { viewModel.setShakeEnabled(it) }
            )
            if (shakeEnabled) {
                Row(
                    modifier              = Modifier.padding(start = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Sensitivity:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("Low" to 20f, "Medium" to 14f, "High" to 10f).forEach { (label, thresh) ->
                        FilterChip(
                            selected = shakeSensitivity == thresh,
                            onClick  = { viewModel.setShakeSensitivity(thresh) },
                            label    = { Text(label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Wallpaper order
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Wallpaper order", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "How images are selected during rotation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier              = Modifier.padding(start = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected     = !shuffleMode,
                        onClick      = { viewModel.setShuffleMode(false) },
                        leadingIcon  = { Icon(Icons.Default.Sort, null, Modifier.size(18.dp)) },
                        label        = { Text("Sequential") }
                    )
                    FilterChip(
                        selected     = shuffleMode,
                        onClick      = { viewModel.setShuffleMode(true) },
                        leadingIcon  = { Icon(Icons.Default.Shuffle, null, Modifier.size(18.dp)) },
                        label        = { Text("Random") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── APPEARANCE ────────────────────────────────────────────────────
            SectionLabel("APPEARANCE")
            Spacer(Modifier.height(4.dp))

            SettingsRow(
                icon     = Icons.Default.AccessTime,
                title    = "Clock style",
                subtitle = "Font, layout, and clock type",
                onClick  = {
                    runCatching {
                        context.startActivity(Intent("android.settings.LOCK_SCREEN_SETTINGS"))
                    }.onFailure {
                        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                    }
                }
            )
            SettingsRow(
                icon     = Icons.Default.Notifications,
                title    = "Notifications",
                subtitle = if (showNotifications) "Showing on lock screen" else "Hidden on lock screen",
                onClick  = {
                    runCatching {
                        context.startActivity(Intent("android.settings.LOCK_SCREEN_SETTINGS"))
                    }.onFailure {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            )
            SettingsRow(
                icon     = Icons.Default.Apps,
                title    = "Shortcuts",
                subtitle = "Shortcut buttons on the lock screen",
                onClick  = {
                    runCatching {
                        context.startActivity(Intent("android.settings.LOCK_SCREEN_SETTINGS"))
                    }.onFailure {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── DISPLAY ───────────────────────────────────────────────────────
            SectionLabel("DISPLAY")
            Spacer(Modifier.height(4.dp))

            // Tap to wake
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tap to wake", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Wake the screen with a tap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canWrite) {
                    Switch(
                        checked         = tapToWake,
                        onCheckedChange = { enabled ->
                            runCatching {
                                Settings.System.putInt(
                                    context.contentResolver, "tap_to_wake", if (enabled) 1 else 0
                                )
                            }
                            tapToWake = enabled
                        }
                    )
                } else {
                    Switch(checked = tapToWake, onCheckedChange = null, enabled = false)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Screen timeout
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Screen timeout", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "How long before the screen turns off automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!canWrite) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Permission required", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Grant 'Modify system settings' so ScreenSlayer can " +
                                        "change the screen timeout on your behalf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }) { Text("Grant permission") }
                        }
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        timeoutOptions.forEach { (ms, label) ->
                            FilterChip(
                                selected = currentTimeout == ms,
                                onClick  = {
                                    Settings.System.putInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_OFF_TIMEOUT,
                                        ms
                                    )
                                    currentTimeout = ms
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── SECURITY ──────────────────────────────────────────────────────
            SectionLabel("SECURITY")
            Spacer(Modifier.height(4.dp))

            SettingsRow(
                icon     = if (isDeviceSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                title    = "Screen lock type",
                subtitle = if (isDeviceSecure) "Secured" else "No lock — tap to set one",
                onClick  = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            )
            SettingsRow(
                icon     = Icons.Default.Fingerprint,
                title    = "Biometrics",
                subtitle = "Fingerprint and face recognition",
                onClick  = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            )
            SettingsRow(
                icon     = Icons.Default.PrivacyTip,
                title    = "Secure lock settings",
                subtitle = "Lock instantly, smart lock, and more",
                onClick  = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Toggle row ────────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
