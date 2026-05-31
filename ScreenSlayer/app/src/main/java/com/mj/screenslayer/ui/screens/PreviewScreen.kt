package com.mj.screenslayer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mj.screenslayer.viewmodel.MainViewModel
import com.mj.screenslayer.viewmodel.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(
    imageIndex: Int,
    navController: NavController,
    viewModel: MainViewModel
) {
    val images = viewModel.slots.collectAsStateWithLifecycle().value.filterNotNull()
    if (images.isEmpty()) {
        navController.popBackStack()
        return
    }

    val initialPage  = imageIndex.coerceIn(0, images.lastIndex)
    val pagerState   = rememberPagerState(initialPage = initialPage) { images.size }
    val snackbarState = remember { SnackbarHostState() }
    var pendingSnackbar by remember { mutableStateOf<String?>(null) }
    var showSheet    by remember { mutableStateOf(false) }

    LaunchedEffect(pendingSnackbar) {
        pendingSnackbar?.let { snackbarState.showSnackbar(it); pendingSnackbar = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text("${pagerState.currentPage + 1} / ${images.size}")
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                icon    = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                text    = { Text("Set Wallpaper") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { padding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            AsyncImage(
                model              = images[page],
                contentDescription = "Wallpaper preview",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
        }
    }

    // ── Wallpaper target bottom sheet ─────────────────────────────────────────
    if (showSheet) {
        val currentUri = images.getOrNull(pagerState.currentPage)

        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Set Wallpaper",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                WallpaperOptionCard(
                    icon    = Icons.Default.Lock,
                    label   = "Lock Screen",
                    onClick = {
                        showSheet = false
                        currentUri?.let {
                            viewModel.setWallpaper(it, WallpaperTarget.Lock) { ok ->
                                pendingSnackbar = if (ok) "Lock screen set!" else "Failed to set wallpaper"
                            }
                        }
                    }
                )
                WallpaperOptionCard(
                    icon    = Icons.Default.Home,
                    label   = "Home Screen",
                    onClick = {
                        showSheet = false
                        currentUri?.let {
                            viewModel.setWallpaper(it, WallpaperTarget.Home) { ok ->
                                pendingSnackbar = if (ok) "Home screen set!" else "Failed to set wallpaper"
                            }
                        }
                    }
                )
                WallpaperOptionCard(
                    icon    = Icons.Default.Wallpaper,
                    label   = "Both",
                    onClick = {
                        showSheet = false
                        currentUri?.let {
                            viewModel.setWallpaper(it, WallpaperTarget.Both) { ok ->
                                pendingSnackbar = if (ok) "Wallpaper set!" else "Failed to set wallpaper"
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperOptionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment   = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
