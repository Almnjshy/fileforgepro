package com.fileforge.pro.feature.recent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecentScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recent Files", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Recent files will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
