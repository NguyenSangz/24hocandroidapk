package com.example.util

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService

object DevicePerformanceUtils {
    /**
     * Xác định thiết bị có phải cấu hình thấp (Low-end device) hay không dựa trên:
     * - isLowRamDevice() của Android.
     * - Số lượng CPU cores <= 4.
     */
    fun isLowEndDevice(context: Context): Boolean {
        val activityManager = context.getSystemService<ActivityManager>()
        val isLowRam = activityManager?.isLowRamDevice() ?: false
        val runtimeCores = Runtime.getRuntime().availableProcessors()
        return isLowRam || runtimeCores <= 4
    }
}
