package com.mj.screenslayer.model

enum class ScaleMode { FILL, FIT, STRETCH }

data class WallpaperSettings(
    val scaleMode:   ScaleMode = ScaleMode.FILL,
    val dimPercent:  Int       = 0,      // 0 – 70
    val grayscale:   Boolean   = false
)
