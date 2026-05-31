package com.al3rted.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.al3rted.app.data.Alert
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertEditScreen(
    existing: Alert?,
    viewModel: AlertViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var hour by remember { mutableStateOf(existing?.hourOfDay ?: 8) }
    var minute by remember { mutableStateOf(existing?.minute ?: 0) }
    var days by remember { mutableStateOf(existing?.days ?: setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var use24h by remember { mutableStateOf(prefs.getBoolean("use24h", false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Alert" else "Edit Alert", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Alert name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Time format toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Time format", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !use24h,
                        onClick = {
                            use24h = false
                            prefs.edit().putBoolean("use24h", false).apply()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("12h") }
                    SegmentedButton(
                        selected = use24h,
                        onClick = {
                            use24h = true
                            prefs.edit().putBoolean("use24h", true).apply()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("24h") }
                }
            }

            Text("Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            if (use24h) {
                // 24h mode: hour 0-23, minute 0-59
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeSpinner(value = hour, range = 0..23, onValueChange = { hour = it })
                    Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    TimeSpinner(value = minute, range = 0..59, onValueChange = { minute = it })
                }
            } else {
                // 12h mode: hour 1-12 + AM/PM
                val display12h = if (hour % 12 == 0) 12 else hour % 12
                val isAm = hour < 12
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeSpinner(
                        value = display12h,
                        range = 1..12,
                        onValueChange = { h ->
                            hour = if (isAm) { if (h == 12) 0 else h } else { if (h == 12) 12 else h + 12 }
                        }
                    )
                    Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    TimeSpinner(value = minute, range = 0..59, onValueChange = { minute = it })
                    Spacer(Modifier.width(12.dp))
                    // AM/PM toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { if (!isAm) hour -= 12 },
                            modifier = Modifier.width(60.dp).height(44.dp),
                            colors = if (isAm) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) else ButtonDefaults.filledTonalButtonColors()
                        ) { Text("AM", fontWeight = FontWeight.Bold) }
                        FilledTonalButton(
                            onClick = { if (isAm) hour += 12 },
                            modifier = Modifier.width(60.dp).height(44.dp),
                            colors = if (!isAm) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) else ButtonDefaults.filledTonalButtonColors()
                        ) { Text("PM", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Text("Repeat on", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            DaySelector(selected = days, onChanged = { days = it })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val alert = (existing ?: Alert()).copy(
                        name = name.trim().ifBlank { "Alert" },
                        hourOfDay = hour,
                        minute = minute,
                        days = days,
                        enabled = enabled
                    )
                    viewModel.save(alert)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = days.isNotEmpty()
            ) {
                Text("SAVE ALERT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimeSpinner(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = { onValueChange(if (value >= range.last) range.first else value + 1) },
            modifier = Modifier.size(48.dp)
        ) { Text("▲") }

        Text(
            text = "%02d".format(value),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp).padding(vertical = 4.dp)
        )

        FilledTonalIconButton(
            onClick = { onValueChange(if (value <= range.first) range.last else value - 1) },
            modifier = Modifier.size(48.dp)
        ) { Text("▼") }
    }
}

@Composable
private fun DaySelector(selected: Set<Int>, onChanged: (Set<Int>) -> Unit) {
    val days = listOf(
        Calendar.SUNDAY to "Su", Calendar.MONDAY to "Mo", Calendar.TUESDAY to "Tu",
        Calendar.WEDNESDAY to "We", Calendar.THURSDAY to "Th", Calendar.FRIDAY to "Fr",
        Calendar.SATURDAY to "Sa"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { (day, label) ->
            val isSelected = day in selected
            FilterChip(
                selected = isSelected,
                onClick = { onChanged(if (isSelected) selected - day else selected + day) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Clip) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
