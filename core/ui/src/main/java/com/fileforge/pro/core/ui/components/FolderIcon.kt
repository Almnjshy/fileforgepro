package com.fileforge.pro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Real folder icon (Master Spec §9) — Windows-style amber folder with shadow.
 * Used in Grid / Large Icon view modes.
 */
@Composable
fun FolderIcon(
    modifier: Modifier = Modifier,
    sizeDp: Int = 56,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFB300)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((sizeDp * 0.7f).dp),
        )
    }
}
