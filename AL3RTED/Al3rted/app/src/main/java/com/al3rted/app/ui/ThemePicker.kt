package com.al3rted.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val rainbowHue = rememberRainbowHue()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Choose Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AppTheme.entries) { theme ->
                    ThemeSwatch(
                        theme = theme,
                        rainbowHue = rainbowHue,
                        selected = theme == current,
                        onSelect = { onSelect(theme); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    rainbowHue: Float,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = themeColors(theme, rainbowHue)
    val borderMod = if (selected)
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
    else
        Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(14.dp))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(borderMod)
        ) {
            if (theme == AppTheme.RAINBOW) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00),
                                Color(0xFF00FF00), Color(0xFF0088FF), Color(0xFF8800FF),
                                Color(0xFFFF0088), Color(0xFFFF0000)
                            )
                        )
                    )
                )
            } else {
                Box(Modifier.fillMaxSize().background(colors.background))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .background(colors.primary)
                        .align(Alignment.TopStart)
                )
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.secondary)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            theme.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
