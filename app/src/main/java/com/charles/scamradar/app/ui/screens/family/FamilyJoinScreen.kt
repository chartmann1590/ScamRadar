package com.charles.scamradar.app.ui.screens.family

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.family.FamilyRepository
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyJoinScreen(
    userPrefs: UserPrefs,
    initialCode: String = "",
    onBack: () -> Unit,
    onJoined: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { FamilyRepository(context) }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(initialCode) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents
        if (!text.isNullOrBlank()) {
            val cleaned = if (text.startsWith("scamradar://family/")) {
                text.removePrefix("scamradar://family/")
            } else text
            code = cleaned
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join a family") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan the QR code from your relative or type the family code they shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("Aim at the family QR code")
                        setBeepEnabled(false)
                        setOrientationLocked(true)
                    }
                    scanner.launch(options)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR code", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text("BLUE-FOX or BLUE-FOX-07") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        val outcome = repo.joinFamily(code)
                        busy = false
                        status = when (outcome) {
                            is FamilyRepository.JoinOutcome.Joined -> {
                                userPrefs.setFamilyCode(outcome.code)
                                userPrefs.setFamilyMemberLabel(outcome.memberLabel.orEmpty())
                                onJoined()
                                null
                            }
                            FamilyRepository.JoinOutcome.NotFound -> "That family code doesn't exist."
                            FamilyRepository.JoinOutcome.PodFull -> "This family is already at 8 members."
                            is FamilyRepository.JoinOutcome.Failed -> outcome.reason
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = code.isNotBlank() && !busy
            ) {
                Text(if (busy) "Joining…" else "Join")
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
