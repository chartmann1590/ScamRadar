package com.charles.scamradar.app.ui.senior

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SeniorScaffold(content: @Composable (Modifier) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold { inner ->
            Box(modifier = Modifier.fillMaxSize().padding(inner)) {
                content(Modifier.fillMaxSize())
            }
        }
    }
}
