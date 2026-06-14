package com.opofit.miapp

import android.app.Application
import com.opofit.miapp.gps.service.GpsTracker
import com.opofit.miapp.gps.service.HrBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OpoFitApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val hrBle = runCatching { HrBleManager.get(this) }.getOrNull() ?: return
        runCatching { hrBle.autoConnectSavedDevice() }

        appScope.launch {
            hrBle.heartRate.collect { bpm ->
                if (bpm != null) GpsTracker.onHrSample(bpm)
            }
        }
        appScope.launch {
            hrBle.state.collect { st ->
                when (st) {
                    is HrBleManager.State.Connected ->
                        GpsTracker.onHrDeviceChanged(st.device.name, true)
                    is HrBleManager.State.Idle,
                    is HrBleManager.State.Error ->
                        GpsTracker.onHrDeviceChanged(null, false)
                    else -> Unit
                }
            }
        }
    }
}
