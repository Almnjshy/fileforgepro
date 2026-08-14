package com.fileforge.pro.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings screen (Master Spec §61 — Themes, §54 — Developer Mode).
 */
@Composable
fun SettingsScreen() {
    var themeMode by remember { mutableStateOf("system") }
    var showHidden by remember { mutableStateOf(false) }
    var developerMode by remember { mutableStateOf(false) }
    var dynamicColor by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }

        item {
            SettingsSection("Appearance") {
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = "Light · Dark · System · AMOLED",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeChip("light", "Light", themeMode) { themeMode = it }
                        ThemeChip("dark", "Dark", themeMode) { themeMode = it }
                        ThemeChip("system", "System", themeMode) { themeMode = it }
                        ThemeChip("amoled", "AMOLED", themeMode) { themeMode = it }
                    }
                }
                SettingsRow(
                    icon = Icons.Outlined.Image,
                    title = "Dynamic color",
                    subtitle = "Material You (Android 12+)",
                ) {
                    Switch(checked = dynamicColor, onCheckedChange = { dynamicColor = it })
                }
            }
        }

        item {
            SettingsSection("Files") {
                SettingsRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Show hidden files",
                    subtitle = "Files and folders starting with .",
                ) {
                    Switch(checked = showHidden, onCheckedChange = { showHidden = it })
                }
            }
        }

        item {
            SettingsSection("Developer") {
                SettingsRow(
                    icon = Icons.Outlined.Code,
                    title = "Developer mode",
                    subtitle = "Show file details, MIME, permissions, terminal",
                ) {
                    Switch(checked = developerMode, onCheckedChange = { developerMode = it })
                }
            }
        }

        item {
            SettingsSection("About") {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "FileForge Pro",
                    subtitle = "Version 1.0.0 · Build 1",
                ) { }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun ThemeChip(value: String, label: String, current: String, onSelect: (String) -> Unit) {
    val isSelected = value == current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onSelect(value) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
