package com.opofit.miapp

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.opofit.miapp.gps.service.GpsTracker
import com.opofit.miapp.gps.service.HrBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OpoFitApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // ImageLoader con soporte de GIFs animados: necesario para mostrar
        // las animaciones de ejercicios (musclewiki, wger, etc.) en el sheet.
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

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
