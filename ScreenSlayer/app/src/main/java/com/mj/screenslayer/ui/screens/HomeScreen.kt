package com.mj.screenslayer.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mj.screenslayer.service.ScreenSlayerLiveWallpaper
import com.mj.screenslayer.viewmodel.MainViewModel
import com.mj.screenslayer.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context    = LocalContext.current
    val slots      by viewModel.slots.collectAsStateWithLifecycle()
    val themeMode  by viewModel.themeMode.collectAsStateWithLifecycle()
    val slotCount  by viewModel.slotCount.collectAsStateWithLifecycle()

    val filledCount = slots.count { it != null }

    var showOverflowMenu    by remember { mutableStateOf(false) }
    var showClearAllDialog  by remember { mutableStateOf(false) }
    var showSlotCountDialog by remember { mutableStateOf(false) }
    val snackbarState       = remember { SnackbarHostState() }
    var pendingSnackbar     by remember { mutableStateOf<String?>(null) }
    var editingSlot         by remember { mutableIntStateOf(-1) }

    // ── Live-wallpaper setup detection ────────────────────────────────────────
    var isLiveWallpaperActive by remember { mutableStateOf(false) }

    fun recheckLiveWallpaper() {
        val wm = WallpaperManager.getInstance(context)
        isLiveWallpaperActive =
            wm.wallpaperInfo?.component?.packageName == context.packageName
    }

    LifecycleResumeEffect(Unit) {
        recheckLiveWallpaper()
        onPauseOrDispose {}
    }

    val liveWallpaperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { recheckLiveWallpaper() }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && editingSlot >= 0) {
            viewModel.setSlots(editingSlot, uris)
            if (uris.size > 1) pendingSnackbar = "${uris.size} images added from slot ${editingSlot + 1}"
        }
        editingSlot = -1
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFolder(it) { count ->
                pendingSnackbar = if (count > 0) "Imported $count image${if (count != 1) "s" else ""} from folder"
                                  else "No images found in that folder"
            }
        }
    }

    LaunchedEffect(pendingSnackbar) {
        pendingSnackbar?.let { snackbarState.showSnackbar(it); pendingSnackbar = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenSlayer") },
                actions = {
                    if (filledCount > 0) {
                        IconButton(onClick = {
                            viewModel.setRandomWallpaper { ok ->
                                pendingSnackbar =
                                    if (ok) "Random wallpaper applied!" else "Failed to set wallpaper"
                            }
                        }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Random wallpaper")
                        }
                    }
                    IconButton(onClick = { viewModel.cycleTheme() }) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.Light  -> Icons.Default.LightMode
                                ThemeMode.Dark   -> Icons.Default.DarkMode
                                ThemeMode.System -> Icons.Default.SettingsBrightness
                            },
                            contentDescription = "Toggle theme"
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded         = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Wallpaper appearance") },
                                leadingIcon = { Icon(Icons.Default.Tune, null) },
                                onClick     = {
                                    showOverflowMenu = false
                                    navController.navigate("wallpapersettings")
                                }
                            )
                            DropdownMenuItem(
                                text        = { Text("Lock screen settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick     = {
                                    showOverflowMenu = false
                                    navController.navigate("lockscreensettings")
                                }
                            )
                            DropdownMenuItem(
                                text        = { Text("Number of slots  ($slotCount)") },
                                leadingIcon = { Icon(Icons.Default.GridView, null) },
                                onClick     = {
                                    showOverflowMenu = false
                                    showSlotCountDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text        = { Text("Import folder") },
                                leadingIcon = { Icon(Icons.Default.Folder, null) },
                                onClick     = {
                                    showOverflowMenu = false
                                    folderPicker.launch(null)
                                }
                            )
                            if (filledCount > 0) {
                                DropdownMenuItem(
                                    text        = { Text("Clear all slots") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showClearAllDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { padding ->
        LazyVerticalGrid(
            columns               = GridCells.Fixed(5),
            modifier              = Modifier.fillMaxSize().padding(padding),
            contentPadding        = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            // ── Setup banner (spans all 5 columns) ────────────────────────────
            if (!isLiveWallpaperActive) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SetupBanner(
                        onEnable = {
                            runCatching {
                                liveWallpaperLauncher.launch(
                                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(
                                                context.packageName,
                                                ScreenSlayerLiveWallpaper::class.java.name
                                            )
                                        )
                                    }
                                )
                            }
                        },
                        onAddImages = {
                            editingSlot = slots.indexOfFirst { it == null }.takeIf { it >= 0 }
                                ?: slots.size
                            imagePicker.launch("image/*")
                        },
                        onAddFolder = { folderPicker.launch(null) }
                    )
                }
            }

            itemsIndexed(slots) { index, uri ->
                WallpaperSlot(
                    index   = index,
                    uri     = uri,
                    onTap   = { editingSlot = index; imagePicker.launch("image/*") },
                    onClear = { viewModel.clearSlot(index) }
                )
            }
        }
    }

    // ── Slot count dialog ─────────────────────────────────────────────────────
    if (showSlotCountDialog) {
        SlotCountDialog(
            current  = slotCount,
            min      = MainViewModel.MIN_SLOTS,
            max      = MainViewModel.MAX_SLOTS,
            onDismiss = { showSlotCountDialog = false },
            onConfirm = { newCount ->
                showSlotCountDialog = false
                viewModel.setSlotCount(newCount)
                pendingSnackbar = "Slots updated to $newCount"
            }
        )
    }

    // ── Clear all dialog ──────────────────────────────────────────────────────
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon    = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title   = { Text("Clear all slots?") },
            text    = { Text("All $filledCount wallpaper${if (filledCount != 1) "s" else ""} will be removed. Original files are unaffected.") },
            confirmButton = {
                TextButton(onClick = { showClearAllDialog = false; viewModel.clearAll() }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Lock-screen setup banner ──────────────────────────────────────────────────

@Composable
private fun SetupBanner(
    onEnable:    () -> Unit,
    onAddImages: () -> Unit,
    onAddFolder: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Lock screen not active",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                "Tap Enable to set ScreenSlayer as your live wallpaper. " +
                "Then add images below — they rotate on your lock screen automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Add images
                OutlinedButton(
                    onClick  = onAddImages,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Images")
                }

                // Add folder
                OutlinedButton(
                    onClick  = onAddFolder,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Folder")
                }

                // Enable live wallpaper
                Button(onClick = onEnable) {
                    Text("Enable")
                }
            }
        }
    }
}

// ── Slot count picker dialog ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotCountDialog(
    current: Int,
    min: Int,
    max: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var count by remember { mutableIntStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.GridView, contentDescription = null) },
        title = { Text("Number of wallpaper slots") },
        text  = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Set how many slots appear on the home screen ($min–$max).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Counter row ───────────────────────────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    FilledIconButton(
                        onClick  = { if (count > min) count-- },
                        enabled  = count > min
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text  = "$count",
                        style = MaterialTheme.typography.displaySmall
                    )

                    FilledIconButton(
                        onClick  = { if (count < max) count++ },
                        enabled  = count < max
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // ── Preset chips ──────────────────────────────────────────────
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 50, 100, 250, 500, 1000).forEach { preset ->
                        FilterChip(
                            selected = count == preset,
                            onClick  = { count = preset },
                            label    = { Text("$preset") }
                        )
                    }
                }

                if (count < current) {
                    Text(
                        "Slots ${count + 1}–$current and their wallpapers will be removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(count) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Wallpaper slot tile ───────────────────────────────────────────────────────

@Composable
private fun WallpaperSlot(
    index: Int,
    uri: Uri?,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .aspectRatio(0.65f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model              = uri,
                contentDescription = "Slot ${index + 1}",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            Text(
                text     = "${index + 1}",
                color    = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
            IconButton(
                onClick  = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear slot",
                    tint     = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add wallpaper",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text  = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
