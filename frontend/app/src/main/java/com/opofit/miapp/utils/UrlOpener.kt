package com.opofit.miapp.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object UrlOpener {
    fun open(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return

        
        runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
        }.onFailure {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                
            }
        }
    }
}

