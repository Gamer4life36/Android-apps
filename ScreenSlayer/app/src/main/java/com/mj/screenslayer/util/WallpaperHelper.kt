package com.mj.screenslayer.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.net.Uri
import com.mj.screenslayer.model.ScaleMode
import com.mj.screenslayer.model.WallpaperSettings

object WallpaperHelper {

    /**
     * Picks the next wallpaper in the rotation sequence from the 15 fixed slots,
     * applies the current [WallpaperSettings] (scale, dim, grayscale) and pushes
     * it to the lock screen.
     */
    suspend fun applyNextWallpaper(context: Context) {
        val prefs = context.getSharedPreferences("screenslayer", Context.MODE_PRIVATE)

        val slotCount = prefs.getInt("slot_count", 15).coerceIn(1, 1000)
        val uris = (0 until slotCount).mapNotNull { i ->
            prefs.getString("slot_${i}_uri", null)?.let { Uri.parse(it) }
        }
        if (uris.isEmpty()) return

        val shuffle = prefs.getBoolean("shuffle_mode", true)
        val uri: Uri
        if (shuffle) {
            uri = uris.random()
        } else {
            val idx = prefs.getInt("rotation_index", 0) % uris.size
            uri = uris[idx]
            prefs.edit().putInt("rotation_index", (idx + 1) % uris.size).apply()
        }

        // Persist URI + MIME for the live wallpaper engine
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        prefs.edit()
            .putString("live_wallpaper_uri", uri.toString())
            .putString("live_wallpaper_mime", mime)
            .apply()

        // When ScreenSlayer's own live wallpaper is active, the engine handles rendering.
        // Calling WallpaperManager here would replace the live wallpaper component with a
        // flat static image, killing GIF animation and breaking the setup.
        if (isLiveWallpaperActive(context)) return

        ensureSamsungSingleImageMode(context)
        if (isGif(context, uri)) {
            applyGifWallpaper(context, uri)
        } else {
            val settings = loadSettings(prefs)
            applyWallpaper(context, uri, settings)
        }
    }

    /**
     * Applies a single [uri] directly (called from PreviewScreen's "Set Wallpaper" sheet).
     * GIFs are streamed raw so the system can render them animated (Samsung One UI supports this).
     * All other formats go through the bitmap pipeline (scale / dim / grayscale).
     */
    suspend fun setWallpaper(context: Context, uri: Uri, flag: Int): Boolean = runCatching {
        // Persist URI + MIME for the live wallpaper engine
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        context.getSharedPreferences("screenslayer", Context.MODE_PRIVATE).edit()
            .putString("live_wallpaper_uri", uri.toString())
            .putString("live_wallpaper_mime", mime)
            .apply()

        // Live wallpaper handles rendering — don't overwrite it via WallpaperManager
        if (isLiveWallpaperActive(context)) return@runCatching

        if (flag and WallpaperManager.FLAG_LOCK != 0) ensureSamsungSingleImageMode(context)
        if (isGif(context, uri)) {
            applyGifWallpaper(context, uri, flag)
        } else {
            val prefs    = context.getSharedPreferences("screenslayer", Context.MODE_PRIVATE)
            val settings = loadSettings(prefs)
            applyWallpaper(context, uri, settings, flag)
        }
    }.isSuccess

    // ── Live-wallpaper guard ──────────────────────────────────────────────────

    /**
     * Returns true when ScreenSlayer's own [ScreenSlayerLiveWallpaper] is the active
     * wallpaper component. In that case all rendering is handled by the engine via
     * [live_wallpaper_uri]; calling WallpaperManager would replace the component with
     * a flat static image and break GIF animation.
     */
    /**
     * Returns true when [ScreenSlayerLiveWallpaper] is the active engine.
     *
     * Samsung One UI reports the HOME-screen wallpaper from [WallpaperManager.getWallpaperInfo],
     * not the lock-screen one, so that check alone is unreliable. The engine writes
     * [live_wallpaper_running]=true to SharedPreferences while its surface is alive; we use
     * that as the primary signal and fall back to the WallpaperManager query.
     */
    private fun isLiveWallpaperActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("screenslayer", Context.MODE_PRIVATE)
        if (prefs.getBoolean("live_wallpaper_running", false)) return true
        return runCatching {
            WallpaperManager.getInstance(context)
                .wallpaperInfo?.component?.packageName == context.packageName
        }.getOrElse { false }
    }

    // ── Samsung One UI: disable slideshow mode before setting FLAG_LOCK ────────

    /**
     * On Samsung One UI, if [plugin_lock_wallpaper_type] == 1 the lock screen runs
     * its own slideshow engine and ignores [WallpaperManager.FLAG_LOCK] entirely.
     * Switching it to 0 (single-image mode) makes the system honour FLAG_LOCK.
     *
     * Requires WRITE_SECURE_SETTINGS, which must be granted once via ADB:
     *   adb shell pm grant com.mj.screenslayer android.permission.WRITE_SECURE_SETTINGS
     */
    private fun ensureSamsungSingleImageMode(context: Context) {
        runCatching {
            val cr = context.contentResolver
            val current = android.provider.Settings.Secure.getInt(
                cr, "plugin_lock_wallpaper_type", -1
            )
            if (current == 1) {
                android.provider.Settings.Secure.putInt(cr, "plugin_lock_wallpaper_type", 0)
            }
        }
    }

    // ── GIF helpers ───────────────────────────────────────────────────────────

    private fun isGif(context: Context, uri: Uri): Boolean =
        context.contentResolver.getType(uri) == "image/gif"

    /**
     * Streams the raw GIF bytes directly to [WallpaperManager].
     * The system wallpaper renderer (Samsung One UI in particular) will play
     * the animation; AOSP typically shows the first frame only.
     * Bypasses bitmap processing because re-encoding kills animation.
     */
    private fun applyGifWallpaper(
        context: Context,
        uri:     Uri,
        flag:    Int = WallpaperManager.FLAG_LOCK
    ) {
        val wm = WallpaperManager.getInstance(context)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            wm.setStream(stream, null, true, flag)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun loadSettings(prefs: android.content.SharedPreferences): WallpaperSettings =
        WallpaperSettings(
            scaleMode  = ScaleMode.valueOf(prefs.getString("wp_scale_mode", ScaleMode.FILL.name)!!),
            dimPercent = prefs.getInt("wp_dim_percent", 0),
            grayscale  = prefs.getBoolean("wp_grayscale", false)
        )

    private fun applyWallpaper(
        context:  Context,
        uri:      Uri,
        settings: WallpaperSettings,
        flag:     Int = WallpaperManager.FLAG_LOCK
    ) {
        val wm      = WallpaperManager.getInstance(context)
        val targetW = wm.desiredMinimumWidth.takeIf { it > 0 }
            ?: context.resources.displayMetrics.widthPixels
        val targetH = wm.desiredMinimumHeight.takeIf { it > 0 }
            ?: context.resources.displayMetrics.heightPixels

        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return

        val scaled = scaleBitmap(raw, targetW, targetH, settings.scaleMode)
        val dimmed = applyDim(scaled, settings.dimPercent)
        val final  = if (settings.grayscale) applyGrayscale(dimmed) else dimmed

        wm.setBitmap(final, null, true, flag)
    }

    // ── Bitmap processing ─────────────────────────────────────────────────────

    private fun scaleBitmap(src: Bitmap, w: Int, h: Int, mode: ScaleMode): Bitmap {
        if (src.width == 0 || src.height == 0) return src
        return when (mode) {
            ScaleMode.STRETCH -> Bitmap.createScaledBitmap(src, w, h, true)

            ScaleMode.FILL -> {
                // Scale up so both dimensions are ≥ target, then centre-crop
                val scale   = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
                val sw      = (src.width  * scale).toInt().coerceAtLeast(1)
                val sh      = (src.height * scale).toInt().coerceAtLeast(1)
                val tmp     = Bitmap.createScaledBitmap(src, sw, sh, true)
                val offX    = ((sw - w) / 2).coerceAtLeast(0)
                val offY    = ((sh - h) / 2).coerceAtLeast(0)
                Bitmap.createBitmap(tmp, offX, offY,
                    minOf(w, tmp.width), minOf(h, tmp.height))
            }

            ScaleMode.FIT -> {
                // Scale so the whole image fits; pad the rest with black
                val scale   = minOf(w.toFloat() / src.width, h.toFloat() / src.height)
                val sw      = (src.width  * scale).toInt().coerceAtLeast(1)
                val sh      = (src.height * scale).toInt().coerceAtLeast(1)
                val result  = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas  = Canvas(result)
                canvas.drawColor(Color.BLACK)
                val tmp     = Bitmap.createScaledBitmap(src, sw, sh, true)
                canvas.drawBitmap(tmp, (w - sw) / 2f, (h - sh) / 2f, null)
                result
            }
        }
    }

    private fun applyDim(src: Bitmap, dimPercent: Int): Bitmap {
        if (dimPercent <= 0) return src
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint  = Paint().apply {
            color = Color.argb((255 * dimPercent / 100), 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
        return result
    }

    private fun applyGrayscale(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint  = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().also { it.setSaturation(0f) }
            )
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }
}
