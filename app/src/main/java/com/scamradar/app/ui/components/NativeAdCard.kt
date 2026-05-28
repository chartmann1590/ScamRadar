package com.scamradar.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun NativeAdCard(ad: NativeAd) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(8.dp)
    ) {
        Text(
            text = ad.headline ?: "Ad",
            modifier = Modifier.padding(16.dp)
        )
    }
}
