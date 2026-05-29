package com.charles.scamradar.app.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.charles.scamradar.app.ads.AdUnits

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp

    val adView = remember {
        AdView(context).apply {
            adUnitId = AdUnits.banner
            val activity = context as? Activity
            val size = if (activity != null) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
            } else {
                AdSize.BANNER
            }
            setAdSize(size)
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { adView })
    }
}
