package com.opofit.miapp.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide events that cross screen boundaries without requiring
 * shared ViewModels or navigation plumbing.
 */
object AppEvents {
    private val _homeRefresh = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val homeRefresh: SharedFlow<Unit> = _homeRefresh.asSharedFlow()

    fun signalHomeRefresh() {
        _homeRefresh.tryEmit(Unit)
    }
}
