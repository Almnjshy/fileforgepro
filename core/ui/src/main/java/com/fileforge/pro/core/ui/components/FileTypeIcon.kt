package com.fileforge.pro.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fileforge.pro.domain.model.FileType

/**
 * Vector icon for a [FileType]. Used in List / Details view modes.
 * (Master Spec §9 — folders must look like real folders.)
 */
@Composable
fun FileTypeIcon(
    fileType: FileType,
    modifier: Modifier = Modifier,
    size: Int = 24,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val (icon, color) = iconAndColor(fileType)
    Icon(
        imageVector = icon,
        contentDescription = fileType.name,
        tint = color,
        modifier = modifier.size(size.dp),
    )
}

private fun iconAndColor(fileType: FileType): Pair<ImageVector, Color> = when (fileType) {
    FileType.FOLDER -> Icons.Outlined.Folder to Color(0xFFFFB300)
    FileType.TEXT -> Icons.Outlined.TextSnippet to Color(0xFF42A5F5)
    FileType.IMAGE -> Icons.Outlined.Image to Color(0xFF66BB6A)
    FileType.VIDEO -> Icons.Outlined.Movie to Color(0xFFEF5350)
    FileType.AUDIO -> Icons.Outlined.MusicNote to Color(0xFFAB47BC)
    FileType.ARCHIVE -> Icons.Outlined.Archive to Color(0xFF8D6E63)
    FileType.APK -> Icons.Outlined.Android to Color(0xFF26A69A)
    FileType.PDF -> Icons.Outlined.PictureAsPdf to Color(0xFFEF5350)
    FileType.OTHER -> Icons.Outlined.Description to Color(0xFF607D8B)
}
