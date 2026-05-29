package com.charles.scamradar.app.ui.navigation

import android.util.Base64

object NavArgCodec {

    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    fun encode(text: String): String {
        return Base64.encodeToString(text.toByteArray(Charsets.UTF_8), FLAGS)
    }

    fun decode(text: String): String {
        return runCatching { String(Base64.decode(text, FLAGS), Charsets.UTF_8) }
            .getOrDefault(text)
    }
}
