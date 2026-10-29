package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.ui.components.HudHapticFeedback
import com.example.ui.viewmodel.AiProcessingStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ROBBY AI", appName)
  }

  @Test
  fun `user profile defaults include haptics enabled`() {
    val profile = UserProfile(userId = "test_user")
    assertTrue(profile.hapticsEnabled)
    assertEquals("cyan_arc", profile.hudTheme)
  }

  @Test
  fun `hud haptic feedback executes safely across all processing stages`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hapticFeedback = HudHapticFeedback(context)

    // Verify all stage transitions run without exception
    AiProcessingStage.values().forEach { stage ->
      hapticFeedback.onStageTransition(stage, isEnabled = true)
      hapticFeedback.onStageTransition(stage, isEnabled = false)
    }

    // Verify neural ticks, completion, and abort
    hapticFeedback.onNeuralPulseTick(isEnabled = true)
    hapticFeedback.onNeuralPulseTick(isEnabled = false)
    hapticFeedback.onDirectiveComplete(isEnabled = true)
    hapticFeedback.onDirectiveComplete(isEnabled = false)
    hapticFeedback.onAbort(isEnabled = true)
    hapticFeedback.onAbort(isEnabled = false)
  }
}
