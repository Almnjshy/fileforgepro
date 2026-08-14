package com.fileforge.pro.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.core.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val hasStorageAccess: Boolean = false,
    val hasNotificationsAccess: Boolean = false,
    val isChecking: Boolean = true,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val dispatchers: AppDispatchers,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    init { checkPermissions() }
    fun checkPermissions() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = OnboardingUiState(
                hasStorageAccess = permissionManager.hasManageExternalStorage() ||
                        permissionManager.hasReadMediaImages() ||
                        permissionManager.hasReadMediaVideo() ||
                        permissionManager.hasReadMediaAudio(),
                hasNotificationsAccess = permissionManager.hasPostNotifications(),
                isChecking = false,
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onPermissionsGranted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ -> viewModel.checkPermissions() }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> viewModel.checkPermissions() }

    LaunchedEffect(state.hasStorageAccess) {
        if (state.hasStorageAccess) onPermissionsGranted()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        if (state.isChecking) { CircularProgressIndicator(); return@Box }
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Text("FileForge Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Professional file manager for Android", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (state.hasStorageAccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (state.hasStorageAccess) Icons.Outlined.CheckCircle else Icons.Outlined.Storage, contentDescription = null,
                            tint = if (state.hasStorageAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (state.hasStorageAccess) "Storage access granted" else "Storage access required",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text("FileForge Pro needs access to your files so it can list, copy, move, and manage them like a desktop file explorer.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!state.hasStorageAccess) {
                        Button(onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                manageStorageLauncher.launch(intent)
                            } catch (e: Exception) {
                                notificationLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Grant storage access") }
                    }
                }
            }

            if (!state.hasNotificationsAccess) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Notifications (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Show progress of long-running copy, move, and delete operations.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.hasStorageAccess) {
                Text("✓ Ready — opening FileForge…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
