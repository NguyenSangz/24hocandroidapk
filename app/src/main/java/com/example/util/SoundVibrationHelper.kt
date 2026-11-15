package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundVibrationHelper {

    fun playCorrectSound(enabled: Boolean = true) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (_: Exception) {
            // Ignore if tone generation not supported
        }
    }

    fun playIncorrectSound(enabled: Boolean = true) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tg.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun vibrateCorrect(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, longArrayOf(0, 40, 60, 50))
    }

    fun vibrateIncorrect(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, longArrayOf(0, 100, 80, 120))
    }

    fun vibrateClick(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, longArrayOf(0, 25))
    }

    private fun vibrate(context: Context, timings: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibrator unavailable
        }
    }
}
