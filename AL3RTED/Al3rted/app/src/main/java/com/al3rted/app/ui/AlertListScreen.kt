package com.al3rted.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.al3rted.app.data.Alert
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    viewModel: AlertViewModel,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alert) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE) }
    val use24h = prefs.getBoolean("use24h", false)
    val alerts by viewModel.alerts.collectAsState()
    var showThemePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Al3rted", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showThemePicker = true }) {
                        Text("🎨", style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(
                        onClick = onSettings,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("⚙", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No alerts yet.\nTap + to create one.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(
                        alert = alert,
                        use24h = use24h,
                        onToggle = { enabled -> viewModel.toggle(alert, enabled) },
                        onEdit = { onEdit(alert) },
                        onDelete = { viewModel.delete(alert) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showThemePicker) {
        ThemePickerSheet(
            current = currentTheme,
            onSelect = onThemeChange,
            onDismiss = { showThemePicker = false }
        )
    }
}

@Composable
private fun AlertRow(
    alert: Alert,
    use24h: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                alert.name.ifBlank { "Unnamed Alert" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                formatTime(alert.hourOfDay, alert.minute, use24h),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                formatDays(alert.days),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = alert.enabled, onCheckedChange = onToggle)
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text("Del")
        }
    }
}

private fun formatTime(hour: Int, minute: Int, use24h: Boolean): String {
    return if (use24h) {
        "%02d:%02d".format(hour, minute)
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        "%d:%02d %s".format(h, minute, amPm)
    }
}

private fun formatDays(days: Set<Int>): String {
    if (days.isEmpty()) return "No days selected"
    val names = mapOf(
        Calendar.SUNDAY to "Sun", Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed", Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat"
    )
    val ordered = (Calendar.SUNDAY..Calendar.SATURDAY).filter { it in days }
    return if (ordered.size == 7) "Every day"
    else ordered.mapNotNull { names[it] }.joinToString(", ")
}
