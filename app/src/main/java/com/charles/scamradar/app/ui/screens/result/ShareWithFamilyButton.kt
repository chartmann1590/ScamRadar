package com.charles.scamradar.app.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.family.FamilyRepository
import kotlinx.coroutines.launch

@Composable
fun ShareWithFamilyButton(scanResult: ScanResult, userPrefs: UserPrefs) {
    val context = LocalContext.current
    val familyCode by userPrefs.familyCode.collectAsState(initial = "")
    if (familyCode.isBlank()) return

    val repo = remember { FamilyRepository(context) }
    val scope = rememberCoroutineScope()
    var sharedOk by remember(scanResult.timestamp) { mutableStateOf(false) }
    var busy by remember(scanResult.timestamp) { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            if (!sharedOk && !busy) {
                busy = true
                scope.launch {
                    val ok = repo.shareWithFamily(familyCode, scanResult)
                    sharedOk = ok
                    busy = false
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(14.dp),
        enabled = !busy
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (sharedOk) Icons.Default.Check else Icons.Default.Diversity1,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    sharedOk -> "Shared with family"
                    busy -> "Sharing…"
                    else -> "Share with family"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
