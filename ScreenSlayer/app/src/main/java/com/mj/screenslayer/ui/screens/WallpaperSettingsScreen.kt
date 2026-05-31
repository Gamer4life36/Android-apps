package com.mj.screenslayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mj.screenslayer.model.ScaleMode
import com.mj.screenslayer.model.WallpaperSettings
import com.mj.screenslayer.ui.components.SmoothSlider
import com.mj.screenslayer.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSettingsScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val settings by viewModel.wallpaperSettings.collectAsStateWithLifecycle()

    // Local draft — only committed on Save
    var localScaleMode by remember(settings) { mutableStateOf(settings.scaleMode) }
    var localDim       by remember(settings) { mutableFloatStateOf(settings.dimPercent.toFloat()) }
    var localGrayscale by remember(settings) { mutableStateOf(settings.grayscale) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Wallpaper Appearance") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {

                // ── Scale mode ────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Scale mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val scaleModes = listOf(
                        ScaleMode.FILL    to ("Fill"    to "Zoom and crop to cover the entire screen"),
                        ScaleMode.FIT     to ("Fit"     to "Show the full image with letterbox bars"),
                        ScaleMode.STRETCH to ("Stretch" to "Stretch to fill — may distort")
                    )

                    scaleModes.forEach { (mode, info) ->
                        val (label, desc) = info
                        OutlinedCard(
                            onClick  = { localScaleMode = mode },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier              = Modifier.padding(
                                    horizontal = 16.dp, vertical = 12.dp
                                ),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = localScaleMode == mode,
                                    onClick  = { localScaleMode = mode }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ── Dim slider ────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dim",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${localDim.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SmoothSlider(
                        value         = localDim,
                        onValueChange = { localDim = it },
                        valueRange    = 0f..70f,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Darken the wallpaper so text and notifications are easier to read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // ── Grayscale toggle ──────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)) {
                        Text("Grayscale", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Convert wallpaper to black & white",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked         = localGrayscale,
                        onCheckedChange = { localGrayscale = it }
                    )
                }
            }

            // ── Fixed Save button at bottom ───────────────────────────────────
            Button(
                onClick = {
                    viewModel.saveWallpaperSettings(
                        WallpaperSettings(
                            scaleMode  = localScaleMode,
                            dimPercent = localDim.roundToInt(),
                            grayscale  = localGrayscale
                        )
                    )
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Save")
            }
        }
    }
}
