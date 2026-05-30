package com.charles.scamradar.app.shield

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ClipboardWatcher {

    private val INTERESTING = Regex(
        pattern = "https?://|www\\.|@\\w+\\.|\\+?\\d[\\d\\s().-]{7,}|(zelle|venmo|cashapp|paypal|gift card|wire|crypto|bitcoin|ethereum|usdt)",
        option = RegexOption.IGNORE_CASE,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun register(context: Context) {
        if (context !is Application) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            scope.launch {
                val prefs = UserPrefs(context)
                if (!prefs.clipboardChipEnabled.first()) return@launch
                val text = runCatching {
                    cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                }.getOrDefault("")
                if (text.length < 8) return@launch
                if (!INTERESTING.containsMatchIn(text)) return@launch
                ClipboardHeadsUp.post(context, text)
            }
        }

        val owner = ProcessLifecycleOwner.get()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> runCatching { cm.addPrimaryClipChangedListener(listener) }
                Lifecycle.Event.ON_STOP -> runCatching { cm.removePrimaryClipChangedListener(listener) }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
    }
}
