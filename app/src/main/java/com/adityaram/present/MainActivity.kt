package com.adityaram.present

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.adityaram.present.ui.main.MainScreen
import com.adityaram.present.ui.navigation.Screen
import com.adityaram.present.ui.theme.PresentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as PresentApplication).container

        val content: android.view.View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (StartupState.isAppReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // Ensure we re-evaluate in the next frame so it doesn't freeze endlessly
                        content.postInvalidateOnAnimation()
                        false
                    }
                }
            }
        )

        setContent {
            val themeMode by appContainer.userPreferencesRepository.themeMode.collectAsState(initial = "dark")
            val onboardingCompleted by appContainer.userPreferencesRepository.onboardingCompleted.collectAsState(initial = null)
            
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "system" -> isSystemDark
                "light" -> false
                else -> true
            }
            if (onboardingCompleted != null) {
                StartupState.isAppReady = true
                val startDest = if (onboardingCompleted == true) Screen.Home.route else Screen.Welcome.route
                PresentTheme(darkTheme = useDarkTheme) {
                    MainScreen(startDestination = startDest)
                }
            }
        }
    }
}
