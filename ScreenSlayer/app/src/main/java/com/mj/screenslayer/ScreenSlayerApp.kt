package com.mj.screenslayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.mj.screenslayer.service.WallpaperTriggerService

class ScreenSlayerApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                WallpaperTriggerService.CHANNEL_ID,
                "Wallpaper Trigger",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while ScreenSlayer listens for screen wake / unlock events"
                setShowBadge(false)
            }
        )
    }
}
