package com.opofit.miapp.utils

import com.opofit.miapp.BuildConfig

object MediaUrlUtil {
    fun resolveAvatar(url: String?): String? {
        val u = url?.trim().orEmpty()
        if (u.isBlank()) return null
        if (u.startsWith("http://") || u.startsWith("https://")) return u
        if (u.startsWith("/uploads/")) {
            return BuildConfig.BASE_URL.trimEnd('/') + u
        }
        return null
    }
}
