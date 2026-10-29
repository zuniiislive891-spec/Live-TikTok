package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.viewmodel.AiProcessingStage

/**
 * Advanced Haptic Feedback Controller for ROBBY AI Futuristic HUD.
 * Coordinates system vibrators and Compose haptics to provide tactile confirmation
 * of neural processing stages, radar sweeps, and directive dispatches.
 */
class HudHapticFeedback(
    private val context: Context,
    private val composeHaptic: HapticFeedback? = null
) {
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w("HudHaptic", "Could not get vibrator: ${e.message}")
            null
        }
    }

    /**
     * Tactile pulse triggered on AI state transition.
     */
    fun onStageTransition(stage: AiProcessingStage, isEnabled: Boolean = true) {
        if (!isEnabled) return

        try {
            when (stage) {
                AiProcessingStage.LISTENING -> {
                    // Quick sharp tick indicating mic active
                    playClick(durationMs = 25, amplitude = 120)
                    composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                AiProcessingStage.ANALYZING -> {
                    // Double pulse simulating neural sync / lock-on
                    playDoublePulse(firstMs = 35, pauseMs = 40, secondMs = 45)
                    composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                AiProcessingStage.EXECUTING -> {
                    // Solid confirmation pulse
                    playClick(durationMs = 60, amplitude = 200)
                    composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                AiProcessingStage.SPEAKING -> {
                    // Gentle humming burst
                    playClick(durationMs = 30, amplitude = 80)
                }
                AiProcessingStage.IDLE -> {
                    // Light release
                    playClick(durationMs = 15, amplitude = 60)
                }
            }
        } catch (e: Exception) {
            Log.w("HudHaptic", "Stage haptic failed: ${e.message}")
        }
    }

    /**
     * Subtle rhythmic tick for continuous neural scanning.
     */
    fun onNeuralPulseTick(isEnabled: Boolean = true) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (e: Exception) {
            // Ignore in testing or unsupported environments
        }
    }

    /**
     * Triumphant multi-pulse confirmation when command finishes.
     */
    fun onDirectiveComplete(isEnabled: Boolean = true) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 50, 40, 50, 60)
                val amplitudes = intArrayOf(0, 100, 0, 160, 0, 240)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 30, 50, 40, 50, 60), -1)
            }
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Fallback
        }
    }

    /**
     * Abort / warning haptic cue.
     */
    fun onAbort(isEnabled: Boolean = true) {
        if (!isEnabled) return
        try {
            playDoublePulse(firstMs = 70, pauseMs = 60, secondMs = 90)
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun playClick(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val clampedAmp = amplitude.coerceIn(1, 255)
            vib.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    private fun playDoublePulse(firstMs: Long, pauseMs: Long, secondMs: Long) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, firstMs, pauseMs, secondMs)
            val amplitudes = intArrayOf(0, 180, 0, 220)
            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, firstMs, pauseMs, secondMs), -1)
        }
    }
}
