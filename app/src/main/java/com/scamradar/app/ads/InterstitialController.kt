package com.scamradar.app.ads

import android.app.Activity
import android.content.Context
import com.scamradar.app.data.model.Verdict

object InterstitialController {

    fun preload(context: Context) {
    }

    fun maybeShow(activity: Activity, verdict: Verdict, onDismiss: () -> Unit) {
        onDismiss()
    }
}
