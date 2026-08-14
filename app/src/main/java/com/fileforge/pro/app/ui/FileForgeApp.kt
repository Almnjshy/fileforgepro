package com.fileforge.pro.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.app.navigation.FileForgeNavHost

/**
 * Top-level composable for the FileForge app.
 *
 * Shows onboarding (permission request) on first launch until storage
 * permission is granted, then proceeds to the main navigation graph.
 */
@Composable
fun FileForgeApp() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    var onboardingCompleted by remember { mutableStateOf(false) }

    if (onboardingState.isChecking) {
        // Show nothing while checking permissions
        return
    }

    if (onboardingState.hasStorageAccess || onboardingCompleted) {
        FileForgeNavHost()
    } else {
        OnboardingScreen(
            onPermissionsGranted = { onboardingCompleted = true },
            viewModel = onboardingViewModel,
        )
    }
}
