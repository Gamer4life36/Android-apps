package com.mj.screenslayer.service

import android.graphics.*
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import com.mj.screenslayer.model.ScaleMode
import kotlinx.coroutines.*

/**
 * Live wallpaper — the only reliable way to show custom images on Samsung One UI's
 * lock screen when FoldInteractive is the system live wallpaper.
 *
 * GIF animation
 * ─────────────
 * Detection : ImageDecoder.decodeDrawable() cast as? AnimatedImageDrawable
 *             No MIME type needed — if the drawable is animated, it IS a GIF.
 *
 * Rendering : AnimatedImageDrawable + proper Drawable.Callback
 *             The callback's scheduleDrawable / invalidateDrawable are implemented
 *             via a main-thread Handler. This is the correct design: the drawable
 *             drives its own frame timing and notifies us when to redraw.
 *             The 16 ms polling approach was kept as a belt-and-suspenders backup
 *             (frameRunnable) to guarantee animation on surfaces that don't invoke
 *             the callback chain.
 *
 * android.graphics.Movie was tried but produced static output on Samsung's
 * WallpaperService surfaces — it appears Movie.draw() does not advance frames on
 * Samsung's non-standard canvas implementation. AnimatedImageDrawable is the
 * correct modern API and is confirmed to work on the Z Fold 5.
 */
class ScreenSlayerLiveWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = ScreenSlayerEngine()

    inner class ScreenSlayerEngine : Engine() {

        private val scope       = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val mainHandler = Handler(Looper.getMainLooper())
        private var renderJob: Job? = null

        @Volatile private var surfaceW = 0
        @Volatile private var surfaceH = 0

        // ── Animated drawable state (main thread) ─────────────────────────────

        private var currentDrawable: Drawable? = null
        private var currentDimPaint: Paint? = null
        private var currentScaleMode = ScaleMode.FILL

        /** Belt-and-suspenders 16 ms loop — fires in addition to Drawable.Callback
         *  to guarantee frames advance on surfaces where callbacks aren't invoked. */
        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!isVisible || currentDrawable == null) return
                drawAnimatedFrame()
                mainHandler.postDelayed(this, 16L)
            }
        }

        private fun drawAnimatedFrame() {
            val drawable = currentDrawable ?: return
            val canvas   = surfaceHolder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                if (currentScaleMode == ScaleMode.FILL) {
                    canvas.save()
                    canvas.clipRect(0, 0, surfaceW, surfaceH)
                }
                drawable.draw(canvas)
                if (currentScaleMode == ScaleMode.FILL) canvas.restore()
                currentDimPaint?.let {
                    canvas.drawRect(0f, 0f, surfaceW.toFloat(), surfaceH.toFloat(), it)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun startAnimatedDrawable(
            drawable: AnimatedImageDrawable,
            prefs:    android.content.SharedPreferences
        ) {
            stopGifAnimation()

            val (mode, dim, gray) = loadWallpaperSettings(prefs)
            currentScaleMode = mode
            currentDimPaint  = if (dim > 0) Paint().apply {
                color = Color.argb(255 * dim / 100, 0, 0, 0)
            } else null

            drawable.colorFilter = if (gray)
                ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
            else null

            val bounds = gifBounds(drawable, mode)
            drawable.setBounds(bounds[0], bounds[1], bounds[2], bounds[3])

            // Drawable.Callback — drives frame scheduling inside the drawable itself
            drawable.callback = object : Drawable.Callback {
                override fun invalidateDrawable(who: Drawable) = drawAnimatedFrame()
                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                    mainHandler.postAtTime(what, who, `when`)
                }
                override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                    mainHandler.removeCallbacks(what)
                }
            }

            currentDrawable = drawable
            drawable.start()

            // Draw first frame immediately, then start the backup loop
            drawAnimatedFrame()
            mainHandler.post(frameRunnable)
        }

        private fun stopGifAnimation() {
            mainHandler.removeCallbacks(frameRunnable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (currentDrawable as? AnimatedImageDrawable)?.let {
                    it.callback = null
                    it.stop()
                }
            }
            currentDrawable = null
            currentDimPaint = null
        }

        // ── Surface / visibility lifecycle ────────────────────────────────────

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            getSharedPreferences("screenslayer", MODE_PRIVATE)
                .edit().putBoolean("live_wallpaper_running", true).apply()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            surfaceW = w; surfaceH = h
            startRender()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopGifAnimation(); stopRender()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) startRender() else { stopGifAnimation(); stopRender() }
        }

        override fun onDestroy() {
            stopGifAnimation()
            scope.cancel()
            getSharedPreferences("screenslayer", MODE_PRIVATE)
                .edit().putBoolean("live_wallpaper_running", false).apply()
        }

        // ── Render dispatch ───────────────────────────────────────────────────

        private fun startRender() {
            stopGifAnimation(); stopRender()
            if (surfaceW == 0 || surfaceH == 0) return
            renderJob = scope.launch { render() }
        }

        private fun stopRender() { renderJob?.cancel(); renderJob = null }

        private suspend fun render() {
            val prefs = getSharedPreferences("screenslayer", MODE_PRIVATE)
            val uri   = resolveUri(prefs) ?: run { renderStatic(null, prefs); return }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Try to decode as AnimatedImageDrawable — no MIME type needed.
                // Non-GIF files decode as BitmapDrawable; the cast returns null → static path.
                val animated: AnimatedImageDrawable? = runCatching {
                    ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(contentResolver, uri)
                    ) as? AnimatedImageDrawable
                }.getOrNull()

                if (animated != null) {
                    withContext(Dispatchers.Main) { startAnimatedDrawable(animated, prefs) }
                    return
                }
            }

            renderStatic(uri, prefs)
        }

        // ── Static render ─────────────────────────────────────────────────────

        private fun renderStatic(uri: Uri?, prefs: android.content.SharedPreferences) {
            val bitmap = uri?.let { loadBitmap(it, prefs) }
            val canvas = surfaceHolder.lockCanvas() ?: return
            try {
                if (bitmap != null) {
                    canvas.drawColor(Color.BLACK)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                } else drawPlaceholder(canvas)
            } finally { surfaceHolder.unlockCanvasAndPost(canvas) }
        }

        private fun loadBitmap(uri: Uri, prefs: android.content.SharedPreferences): Bitmap? {
            val (mode, dim, gray) = loadWallpaperSettings(prefs)
            val raw = runCatching {
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: return null
            val scaled = scaleBitmap(raw, surfaceW, surfaceH, mode)
            val dimmed = applyDim(scaled, dim)
            return if (gray) applyGrayscale(dimmed) else dimmed
        }

        // ── URI resolution ────────────────────────────────────────────────────

        private fun resolveUri(prefs: android.content.SharedPreferences): Uri? {
            prefs.getString("live_wallpaper_uri", null)?.let { return Uri.parse(it) }
            val count = prefs.getInt("slot_count", 15).coerceIn(1, 1000)
            for (i in 0 until count) {
                val s = prefs.getString("slot_${i}_uri", null) ?: continue
                prefs.edit().putString("live_wallpaper_uri", s).apply()
                return Uri.parse(s)
            }
            return null
        }

        // ── GIF bounds ────────────────────────────────────────────────────────

        @RequiresApi(Build.VERSION_CODES.P)
        private fun gifBounds(d: AnimatedImageDrawable, mode: ScaleMode): IntArray {
            val iw = d.intrinsicWidth.takeIf  { it > 0 } ?: surfaceW
            val ih = d.intrinsicHeight.takeIf { it > 0 } ?: surfaceH
            return when (mode) {
                ScaleMode.STRETCH -> intArrayOf(0, 0, surfaceW, surfaceH)
                ScaleMode.FILL -> {
                    val s  = maxOf(surfaceW.toFloat() / iw, surfaceH.toFloat() / ih)
                    val dw = (iw * s).toInt(); val dh = (ih * s).toInt()
                    val l  = (surfaceW - dw) / 2;  val t = (surfaceH - dh) / 2
                    intArrayOf(l, t, l + dw, t + dh)
                }
                ScaleMode.FIT -> {
                    val s  = minOf(surfaceW.toFloat() / iw, surfaceH.toFloat() / ih)
                    val dw = (iw * s).toInt(); val dh = (ih * s).toInt()
                    val l  = (surfaceW - dw) / 2;  val t = (surfaceH - dh) / 2
                    intArrayOf(l, t, l + dw, t + dh)
                }
            }
        }

        // ── Placeholder ───────────────────────────────────────────────────────

        private fun drawPlaceholder(canvas: Canvas) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, 0f, canvas.height.toFloat(),
                        intArrayOf(0xFF1A1A2E.toInt(), 0xFF16213E.toInt(), 0xFF0F3460.toInt()),
                        null, Shader.TileMode.CLAMP) })
            canvas.drawText("Add images in ScreenSlayer",
                canvas.width / 2f, canvas.height / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xCCFFFFFF.toInt(); textSize = 48f; textAlign = Paint.Align.CENTER })
        }

        // ── Settings ──────────────────────────────────────────────────────────

        private fun loadWallpaperSettings(prefs: android.content.SharedPreferences)
            : Triple<ScaleMode, Int, Boolean> = Triple(
            ScaleMode.valueOf(prefs.getString("wp_scale_mode", ScaleMode.FILL.name)!!),
            prefs.getInt("wp_dim_percent", 0), prefs.getBoolean("wp_grayscale", false))

        // ── Bitmap ops ────────────────────────────────────────────────────────

        private fun scaleBitmap(src: Bitmap, w: Int, h: Int, mode: ScaleMode): Bitmap =
            when (mode) {
                ScaleMode.STRETCH -> Bitmap.createScaledBitmap(src, w, h, true)
                ScaleMode.FILL -> {
                    val s = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
                    val sw = (src.width * s).toInt().coerceAtLeast(1)
                    val sh = (src.height * s).toInt().coerceAtLeast(1)
                    val t = Bitmap.createScaledBitmap(src, sw, sh, true)
                    Bitmap.createBitmap(t, ((sw - w) / 2).coerceAtLeast(0),
                        ((sh - h) / 2).coerceAtLeast(0), minOf(w, t.width), minOf(h, t.height))
                }
                ScaleMode.FIT -> {
                    val s = minOf(w.toFloat() / src.width, h.toFloat() / src.height)
                    val sw = (src.width * s).toInt().coerceAtLeast(1)
                    val sh = (src.height * s).toInt().coerceAtLeast(1)
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(out).apply { drawColor(Color.BLACK)
                        drawBitmap(Bitmap.createScaledBitmap(src, sw, sh, true),
                            (w - sw) / 2f, (h - sh) / 2f, null) }
                    out
                }
            }

        private fun applyDim(src: Bitmap, pct: Int): Bitmap {
            if (pct <= 0) return src
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(),
                Paint().apply { color = Color.argb(255 * pct / 100, 0, 0, 0) })
            return out
        }

        private fun applyGrayscale(src: Bitmap): Bitmap {
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) }) })
            return out
        }
    }
}
