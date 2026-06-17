package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.onboarding.OnboardingScreen
import com.alturya.fluenta.ui.theme.FluentaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun login() {
        compose.setContent { FluentaTheme { LoginScreen(onSuccess = {}) } }
        compose.onRoot().captureRoboImage("build/screens/login.png")
    }

    @Test fun onboarding() {
        compose.setContent { FluentaTheme { OnboardingScreen(onPicked = { _, _ -> }) } }
        compose.onRoot().captureRoboImage("build/screens/onboarding.png")
    }
}
