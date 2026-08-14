package com.fileforge.pro.feature.apk

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.domain.model.FFile

/**
 * APK Manager screen (Master Spec §50).
 *
 * Shows:
 *   - App icon + name + package
 *   - Version (and installed version if present)
 *   - Install / Update / Share / Delete buttons
 *   - Permissions list
 *   - Min/target SDK versions
 */
@Composable
fun ApkScreen(
    file: FFile,
    viewModel: ApkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        val info = state.info ?: return@Column

        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                info.icon?.let { drawable ->
                    androidx.compose.foundation.Image(
                        bitmap = drawableToBitmap(drawable).asImageBitmap(),
                        contentDescription = info.appName,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    )
                } ?: Icon(
                    Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = info.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    viewModel.buildInstallIntent()?.let { intent ->
                        runCatching { context.startActivity(intent) }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Android, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(
                    when {
                        state.canUpdate -> "Update"
                        state.isInstalled -> "Reinstall"
                        else -> "Install"
                    }
                )
            }
            OutlinedButton(onClick = {
                viewModel.buildShareIntent()?.let { intent ->
                    context.startActivity(Intent.createChooser(intent, "Share APK"))
                }
            }) {
                Icon(Icons.Outlined.IosShare, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Version info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow("Version", "${info.versionName} (${info.versionCode})")
                state.installedVersionCode?.let { installed ->
                    InfoRow("Installed", "$installed")
                }
                info.minSdkVersion?.let { InfoRow("Min SDK", sdkVersionName(it)) }
                info.targetSdkVersion?.let { InfoRow("Target SDK", sdkVersionName(it)) }
                InfoRow("Size", FormatUtils.formatBytes(file.size))
                info.isSystemApp.let { if (it) InfoRow("Type", "System app") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Permissions
        if (info.permissions.isNotEmpty()) {
            Text(
                "Permissions (${info.permissions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(info.permissions) { perm ->
                    Text(
                        text = perm,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

private fun sdkVersionName(sdk: Int): String = when (sdk) {
    26 -> "8.0 (Oreo)"
    27 -> "8.1 (Oreo)"
    28 -> "9 (Pie)"
    29 -> "10"
    30 -> "11"
    31 -> "12"
    32 -> "12L"
    33 -> "13"
    34 -> "14"
    else -> "API $sdk"
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
