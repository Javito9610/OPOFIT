package com.opofit.miapp.utils

object SegmentSlugUtil {
    fun slugFromLabel(label: String): String? = when {
        label.contains("50 m", ignoreCase = true) -> "50m"
        label.contains("100 m", ignoreCase = true) -> "100m"
        label.contains("1 km", ignoreCase = true) && !label.contains("10") -> "1km"
        label.contains("5K", ignoreCase = true) || label.contains("5 km", ignoreCase = true) -> "5km"
        label.contains("10K", ignoreCase = true) || label.contains("10 km", ignoreCase = true) -> "10km"
        label.contains("media", ignoreCase = true) -> "21km"
        label.contains("marat", ignoreCase = true) -> "42km"
        else -> null
    }
}
