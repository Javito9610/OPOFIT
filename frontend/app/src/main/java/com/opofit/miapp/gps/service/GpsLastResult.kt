package com.opofit.miapp.gps.service

import com.opofit.miapp.gps.model.ActivitySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-screen bridge for the most recent saved GPS activity.
 *
 * Lets callers like the training screen detect that the user just finished
 * a GPS recording and prefill the exercise distance accordingly.
 */
object GpsLastResult {
    private val _value = MutableStateFlow<ActivitySummary?>(null)
    val value: StateFlow<ActivitySummary?> = _value.asStateFlow()

    fun set(summary: ActivitySummary?) {
        _value.value = summary
    }

    fun consume(): ActivitySummary? {
        val v = _value.value
        _value.value = null
        return v
    }
}
