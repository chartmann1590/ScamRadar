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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.community.CommunityReportsRepository
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import kotlinx.coroutines.launch

@Composable
fun ReportAnonymouslyButton(scanResult: ScanResult) {
    if (scanResult.verdict != Verdict.LIKELY_SCAM) return

    val scope = rememberCoroutineScope()
    val repo = remember { CommunityReportsRepository() }
    var state by remember { mutableStateOf(ReportState.Ready) }

    OutlinedButton(
        onClick = {
            if (state == ReportState.Ready) {
                state = ReportState.Submitting
                scope.launch {
                    val outcome = repo.submitReport(scanResult)
                    state = when (outcome) {
                        is CommunityReportsRepository.ReportOutcome.Submitted -> ReportState.Submitted
                        is CommunityReportsRepository.ReportOutcome.NotEligible -> ReportState.NotEligible
                        is CommunityReportsRepository.ReportOutcome.Failed -> ReportState.Failed
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(14.dp),
        enabled = state == ReportState.Ready
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state == ReportState.Submitted) Icons.Default.Check else Icons.Default.Campaign,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (state) {
                    ReportState.Ready -> "Report this scam anonymously"
                    ReportState.Submitting -> "Submitting…"
                    ReportState.Submitted -> "Reported — thanks!"
                    ReportState.Failed -> "Try again"
                    ReportState.NotEligible -> "Not enough detail to report"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private enum class ReportState { Ready, Submitting, Submitted, Failed, NotEligible }
