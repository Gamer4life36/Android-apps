package com.mj.screenslayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mj.screenslayer.util.WallpaperHelper

class WallpaperRotationWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        WallpaperHelper.applyNextWallpaper(applicationContext)
        Result.success()
    }.getOrElse { Result.failure() }
}
