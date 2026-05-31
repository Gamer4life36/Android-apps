package com.mj.screenslayer.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mj.screenslayer.model.ScaleMode
import com.mj.screenslayer.model.WallpaperSettings
import com.mj.screenslayer.util.WallpaperHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeMode { System, Light, Dark }
enum class WallpaperTarget { Lock, Home, Both }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("screenslayer", Context.MODE_PRIVATE)

    // ── Theme ────────────────────────────────────────────────────────────────

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(
            prefs.getString("theme_mode", ThemeMode.System.name) ?: ThemeMode.System.name
        )
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun cycleTheme() {
        val next = when (_themeMode.value) {
            ThemeMode.System -> ThemeMode.Light
            ThemeMode.Light  -> ThemeMode.Dark
            ThemeMode.Dark   -> ThemeMode.System
        }
        _themeMode.value = next
        prefs.edit().putString("theme_mode", next.name).apply()
    }

    // ── Slot count (user-configurable, 1–50) ─────────────────────────────────

    private val _slotCount = MutableStateFlow(
        prefs.getInt("slot_count", DEFAULT_SLOT_COUNT).coerceIn(MIN_SLOTS, MAX_SLOTS)
    )
    val slotCount: StateFlow<Int> = _slotCount.asStateFlow()

    fun setSlotCount(count: Int) {
        val newCount = count.coerceIn(MIN_SLOTS, MAX_SLOTS)
        val current  = _slots.value.toMutableList()

        // Trim URIs beyond the new count and clear their prefs keys
        if (newCount < current.size) {
            val editor = prefs.edit()
            for (i in newCount until current.size) editor.remove("slot_${i}_uri")
            editor.apply()
            current.subList(newCount, current.size).clear()
        }
        // Pad with nulls if growing
        while (current.size < newCount) current.add(null)

        _slotCount.value = newCount
        _slots.value     = current
        prefs.edit().putInt("slot_count", newCount).apply()
    }

    // ── Wallpaper slots ───────────────────────────────────────────────────────

    private val _slots = MutableStateFlow(loadSlots())
    val slots: StateFlow<List<Uri?>> = _slots.asStateFlow()

    private fun loadSlots(): List<Uri?> = List(_slotCount.value) { i ->
        prefs.getString("slot_${i}_uri", null)?.let { Uri.parse(it) }
    }

    private fun persistSlots(slots: List<Uri?>) {
        prefs.edit().apply {
            slots.forEachIndexed { i, uri ->
                if (uri != null) putString("slot_${i}_uri", uri.toString())
                else remove("slot_${i}_uri")
            }
        }.apply()
    }

    fun setSlot(index: Int, uri: Uri) {
        val context = getApplication<Application>()
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        val updated = _slots.value.toMutableList().apply { set(index, uri) }
        _slots.value = updated
        persistSlots(updated)
    }

    /**
     * Fills slots starting at [startIndex] with [uris].
     * Auto-expands [slotCount] up to [MAX_SLOTS] if more images are supplied than current slots.
     */
    fun setSlots(startIndex: Int, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        val current = _slots.value.toMutableList()

        val needed = (startIndex + uris.size).coerceAtMost(MAX_SLOTS)
        if (needed > current.size) {
            while (current.size < needed) current.add(null)
            _slotCount.value = needed
            prefs.edit().putInt("slot_count", needed).apply()
        }

        uris.forEachIndexed { offset, uri ->
            val idx = startIndex + offset
            if (idx >= MAX_SLOTS) return@forEachIndexed
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (idx < current.size) current[idx] = uri
        }

        _slots.value = current
        persistSlots(current)
    }

    /**
     * Enumerates all images one level deep under [treeUri] and fills empty slots
     * starting from the first available position.
     * Calls [onResult] on the main thread with the number of images actually imported.
     */
    fun importFolder(treeUri: Uri, onResult: (Int) -> Unit) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            )

            val imageUris = mutableListOf<Uri>()
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idCol   = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val mime = cursor.getString(mimeCol) ?: continue
                    if (!mime.startsWith("image/")) continue
                    val docId = cursor.getString(idCol)
                    imageUris.add(
                        android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    )
                }
            }

            launch(Dispatchers.Main) {
                val firstEmpty = _slots.value.indexOfFirst { it == null }
                    .takeIf { it >= 0 } ?: _slots.value.size
                val importable = imageUris.take((MAX_SLOTS - firstEmpty).coerceAtLeast(0))
                if (importable.isNotEmpty()) setSlots(firstEmpty, importable)
                onResult(importable.size)
            }
        }
    }

    fun clearSlot(index: Int) {
        val updated = _slots.value.toMutableList()
        updated.removeAt(index)   // remove the slot
        updated.add(null)         // pad the end so the list stays the same length
        _slots.value = updated
        persistSlots(updated)
    }

    fun clearAll() {
        val empty = List<Uri?>(_slotCount.value) { null }
        _slots.value = empty
        persistSlots(empty)
    }

    // ── Wallpaper appearance ─────────────────────────────────────────────────

    private val _wallpaperSettings = MutableStateFlow(
        WallpaperSettings(
            scaleMode  = ScaleMode.valueOf(
                prefs.getString("wp_scale_mode", ScaleMode.FILL.name) ?: ScaleMode.FILL.name
            ),
            dimPercent = prefs.getInt("wp_dim_percent", 0),
            grayscale  = prefs.getBoolean("wp_grayscale", false)
        )
    )
    val wallpaperSettings: StateFlow<WallpaperSettings> = _wallpaperSettings.asStateFlow()

    fun saveWallpaperSettings(settings: WallpaperSettings) {
        _wallpaperSettings.value = settings
        prefs.edit()
            .putString("wp_scale_mode", settings.scaleMode.name)
            .putInt("wp_dim_percent", settings.dimPercent)
            .putBoolean("wp_grayscale", settings.grayscale)
            .apply()
    }

    // ── Rotation settings ────────────────────────────────────────────────────

    private val _screenTriggerEnabled = MutableStateFlow(
        prefs.getBoolean("screen_trigger_enabled", false)
    )
    val screenTriggerEnabled: StateFlow<Boolean> = _screenTriggerEnabled.asStateFlow()

    private val _shakeEnabled = MutableStateFlow(
        prefs.getBoolean("shake_enabled", false)
    )
    val shakeEnabled: StateFlow<Boolean> = _shakeEnabled.asStateFlow()

    /** Acceleration threshold in m/s² above which a shake is detected.
     *  Lower = more sensitive. Presets: Low=20, Medium=14, High=10. */
    private val _shakeSensitivity = MutableStateFlow(
        prefs.getFloat("shake_sensitivity", 14f)
    )
    val shakeSensitivity: StateFlow<Float> = _shakeSensitivity.asStateFlow()

    /** true = random, false = sequential */
    private val _shuffleMode = MutableStateFlow(
        prefs.getBoolean("shuffle_mode", true)
    )
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    fun setScreenTriggerEnabled(enabled: Boolean) {
        _screenTriggerEnabled.value = enabled
        prefs.edit().putBoolean("screen_trigger_enabled", enabled).apply()
        syncTriggerService()
    }

    fun setShakeEnabled(enabled: Boolean) {
        _shakeEnabled.value = enabled
        prefs.edit().putBoolean("shake_enabled", enabled).apply()
        syncTriggerService()
    }

    fun setShakeSensitivity(threshold: Float) {
        _shakeSensitivity.value = threshold
        prefs.edit().putFloat("shake_sensitivity", threshold).apply()
    }

    fun setShuffleMode(shuffle: Boolean) {
        _shuffleMode.value = shuffle
        prefs.edit().putBoolean("shuffle_mode", shuffle).apply()
    }

    private fun syncTriggerService() {
        val ctx = getApplication<Application>()
        if (_screenTriggerEnabled.value || _shakeEnabled.value) {
            com.mj.screenslayer.service.WallpaperTriggerService.start(ctx)
        } else {
            com.mj.screenslayer.service.WallpaperTriggerService.stop(ctx)
        }
    }

    // ── Wallpaper ────────────────────────────────────────────────────────────

    fun setWallpaper(uri: Uri, target: WallpaperTarget, onResult: (Boolean) -> Unit) {
        val context = getApplication<Application>()
        val flag = when (target) {
            WallpaperTarget.Lock -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.Home -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.Both -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
        }
        viewModelScope.launch(Dispatchers.IO) {
            val success = WallpaperHelper.setWallpaper(context, uri, flag)
            launch(Dispatchers.Main) { onResult(success) }
        }
    }

    fun setRandomWallpaper(onResult: (Boolean) -> Unit) {
        val filled = _slots.value.filterNotNull()
        if (filled.isEmpty()) { onResult(false); return }
        setWallpaper(filled.random(), WallpaperTarget.Lock, onResult)
    }

    companion object {
        const val DEFAULT_SLOT_COUNT = 15
        const val MIN_SLOTS          = 1
        const val MAX_SLOTS          = 1000
    }
}
