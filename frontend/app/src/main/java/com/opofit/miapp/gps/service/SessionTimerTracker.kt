package com.opofit.miapp.gps.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estado compartido del cronómetro (simulacro, entrenamiento, etc.). */
object SessionTimerTracker {

    data class State(
        val active: Boolean = false,
        val paused: Boolean = false,
        val elapsedMs: Long = 0L,
        val label: String = "Sesión"
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(label: String, resumeFromMs: Long = 0L) {
        _state.value = State(active = true, paused = false, elapsedMs = resumeFromMs, label = label)
    }

    fun pause() {
        _state.update { if (it.active) it.copy(paused = true) else it }
    }

    fun resume() {
        _state.update { if (it.active) it.copy(paused = false) else it }
    }

    fun stop() {
        _state.value = State()
    }

    fun tick(deltaMs: Long) {
        _state.update { s ->
            if (s.active && !s.paused) s.copy(elapsedMs = s.elapsedMs + deltaMs) else s
        }
    }

    fun setElapsed(ms: Long) {
        _state.update { it.copy(elapsedMs = ms) }
    }
}
