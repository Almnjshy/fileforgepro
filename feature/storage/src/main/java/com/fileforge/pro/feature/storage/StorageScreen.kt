package com.fileforge.pro.feature.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StorageScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Storage Sources", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}
