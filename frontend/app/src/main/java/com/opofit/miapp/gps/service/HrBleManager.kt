package com.opofit.miapp.gps.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Conecta con cualquier dispositivo BLE que exponga el estándar Heart Rate Service (0x180D):
 * Polar H10/H9, Garmin watches en modo broadcast HR, Wahoo TICKR, Mi Band / Amazfit con
 * "Broadcast HR" activado, etc.
 *
 * Funciona como utilidad independiente: no requiere lifecycle, expone Flows.
 */
@SuppressLint("MissingPermission")
class HrBleManager(private val context: Context) {

    data class FoundDevice(val address: String, val name: String?, val rssi: Int)

    sealed class State {
        data object Idle : State()
        data object Scanning : State()
        data class Connecting(val device: FoundDevice) : State()
        data class Connected(val device: FoundDevice) : State()
        data class Error(val message: String) : State()
    }

    private val manager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = manager?.adapter

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _found = MutableStateFlow<List<FoundDevice>>(emptyList())
    val found: StateFlow<List<FoundDevice>> = _found.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    /**
     * Dispositivos ya emparejados al sistema (Bluetooth Classic + BLE bonded).
     * Incluye Amazfit/Mi Band/Garmin emparejados a Zepp aunque no estén anunciándose
     * activamente. Útil para mostrar al usuario que el reloj sí está conocido por el
     * sistema, y permite intentar conectar directamente sin escanear.
     */
    fun pairedDevices(): List<FoundDevice> {
        if (!hasPermissions()) return emptyList()
        val bonded = adapter?.bondedDevices ?: return emptyList()
        return bonded.map { d ->
            FoundDevice(
                address = d.address,
                name = try { d.name } catch (_: SecurityException) { null },
                rssi = 0
            )
        }
    }

    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var onHrSample: ((Int) -> Unit)? = null
    private var onConnectionChanged: ((deviceName: String?, connected: Boolean) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectTimeout: Runnable? = null
    private var pendingConnectedDevice: FoundDevice? = null

    @Deprecated(
        "Causa que múltiples llamadas se sobrescriban porque HrBleManager es singleton. " +
        "Observa los Flows `heartRate` y `state` desde donde necesites las muestras o eventos.",
        level = DeprecationLevel.WARNING
    )
    fun setListeners(
        onHr: (Int) -> Unit,
        onConnection: (String?, Boolean) -> Unit
    ) {
        this.onHrSample = onHr
        this.onConnectionChanged = onConnection
    }

    fun hasBluetooth(): Boolean = adapter != null && adapter.isEnabled

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val connect = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scan && connect
        }
        // Pre-Android 12 escanear BLE requiere permiso de localización fina.
        val loc = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return loc
    }

    /** Servicios de localización del sistema activos.
     *  En Android <12 BLE scan exige tenerla encendida. En 12+ con BLUETOOTH_SCAN+neverForLocation
     *  (lo que hacemos) ya no hace falta, así que solo lo chequeamos en API <31 para no bloquear
     *  a usuarios con Android 12+ que tengan el GPS apagado pero quieran usar el pulso. */
    fun hasLocationServices(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    /** Emulador AVD estricto: solo bloquea si es CLARAMENTE un emulador, no por "generic" suelto.
     *  Combinación de hardware goldfish/ranchu + product sdk_* + fingerprint generic es la huella
     *  del emulador. Los móviles reales nunca cumplen todas a la vez. */
    fun isProbablyEmulator(): Boolean {
        val hw = Build.HARDWARE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val fingerprintBlatant = Build.FINGERPRINT.lowercase().let {
            it.startsWith("generic/") || it.contains("/sdk_") || it.contains("emulator")
        }
        val hwBlatant = hw == "goldfish" || hw == "ranchu" || hw.contains("ttvm")
        val productBlatant = product.startsWith("sdk_") || product.startsWith("vbox86p") || product == "google_sdk"
        // Requiere al menos 2 señales fuertes a la vez → cero falsos positivos en móviles reales.
        return listOf(fingerprintBlatant, hwBlatant, productBlatant).count { it } >= 2
    }

    fun startScan(broad: Boolean = false) {
        if (!hasBluetooth()) {
            _state.value = State.Error("Activa el Bluetooth en el sistema")
            return
        }
        if (!hasPermissions()) {
            _state.value = State.Error("Faltan permisos de Bluetooth")
            return
        }
        // Solo exigir Location services en Android <12 (en 12+ con neverForLocation ya no aplica).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !hasLocationServices()) {
            _state.value = State.Error(
                "Activa la ubicación del sistema. Android exige tenerla encendida para escanear Bluetooth."
            )
            return
        }
        // El warning de emulador NO bloquea, solo avisa por log y deja seguir escaneando
        // (algunos emuladores con sensor BLE virtual sí funcionan, y los falsos positivos en móviles
        // reales serían fatales — la bloqueaban entera).
        if (isProbablyEmulator()) {
            android.util.Log.w("HrBle", "Posible emulador: el escaneo BLE puede no funcionar")
        }
        stopScan()
        _found.value = emptyList()
        _state.value = State.Scanning

        val scanner = adapter?.bluetoothLeScanner ?: run {
            _state.value = State.Error("BLE scanner no disponible")
            return
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = device.name
                if (!broad && !advertisesHr(result)) return
                val item = FoundDevice(device.address, name, result.rssi)
                _found.update {
                    if (it.any { d -> d.address == item.address }) it else it + item
                }
            }

            override fun onScanFailed(errorCode: Int) {
                cancelScanTimeout()
                _state.value = State.Error("Escaneo BLE falló ($errorCode)")
            }
        }
        scanCallback = callback
        if (broad) {
            scanner.startScan(emptyList(), settings, callback)
        } else {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(HR_SERVICE))
                .build()
            scanner.startScan(listOf(filter), settings, callback)
        }
        // Auto-stop a los 20s para no quedarnos buscando indefinidamente: si no
        // encontró nada damos feedback claro al usuario en vez de la rueda infinita.
        scheduleScanTimeout(broad)
    }

    private var scanTimeoutRunnable: Runnable? = null
    private fun scheduleScanTimeout(broad: Boolean) {
        cancelScanTimeout()
        val r = Runnable {
            if (_state.value is State.Scanning) {
                stopScan()
                if (_found.value.isEmpty()) {
                    val msg = if (broad) {
                        "No apareció ningún dispositivo Bluetooth nuevo. " +
                            "Importante: si tu reloj ya está conectado a Zepp/Mi Fitness/Garmin Connect, " +
                            "Android lo OCULTA del escaneo de otras apps. No es un fallo. " +
                            "Pulsa abajo «Conectar con Health Connect» para enlazarlo de la forma correcta."
                    } else {
                        "Ninguna banda/reloj emite pulso por BLE estándar ahora mismo.\n\n" +
                            "Tu Amazfit/Mi Band/Garmin no aparecerá aquí aunque esté conectado a su app " +
                            "(Zepp, etc.). Para usar su pulso en OpoFit:\n\n" +
                            "• Bandas de pecho (Polar/Wahoo): aparecen solas al encenderlas.\n" +
                            "• Amazfit/Mi Band: activa Broadcast HR en Zepp o usa Health Connect (abajo)."
                    }
                    _state.value = State.Error(msg)
                } else {
                    _state.value = State.Idle
                }
            }
        }
        scanTimeoutRunnable = r
        // 45s: Amazfit/Mi Band pueden anunciar cada 30-40s para ahorrar batería.
        // Antes 20s cortaba antes de que el reloj llegara a emitir su advertisement.
        mainHandler.postDelayed(r, 45_000L)
    }
    private fun cancelScanTimeout() {
        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        scanTimeoutRunnable = null
    }

    private fun advertisesHr(result: ScanResult): Boolean {
        val record = result.scanRecord ?: return false
        val uuids = record.serviceUuids ?: return false
        return uuids.any { it.uuid == HR_SERVICE }
    }

    fun stopScan() {
        cancelScanTimeout()
        scanCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        if (_state.value is State.Scanning) _state.value = State.Idle
    }

    fun connect(device: FoundDevice) {
        if (!hasPermissions()) {
            _state.value = State.Error("Faltan permisos de Bluetooth")
            return
        }
        if (!hasBluetooth()) {
            _state.value = State.Error("Activa el Bluetooth en el sistema")
            return
        }
        stopScan()
        closeGatt()
        val remote = adapter?.getRemoteDevice(device.address) ?: run {
            _state.value = State.Error("Dispositivo no encontrado")
            return
        }
        _state.value = State.Connecting(device)
        scheduleConnectTimeout()
        gatt = openGatt(remote)
        if (gatt == null) {
            cancelConnectTimeout()
            _state.value = State.Error("No se pudo iniciar la conexión BLE")
        }
    }

    fun disconnect() {
        cancelConnectTimeout()
        closeGatt()
        _heartRate.value = null
        onConnectionChanged?.invoke(null, false)
        _state.value = State.Idle
    }

    private fun closeGatt() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private fun openGatt(remote: BluetoothDevice): BluetoothGatt? {
        // autoConnect=false: conexión inmediata al pulsar (autoConnect=true suele no conectar).
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remote.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            remote.connectGatt(context, false, gattCallback)
        }
    }

    private fun scheduleConnectTimeout() {
        cancelConnectTimeout()
        val timeout = Runnable {
            if (_state.value is State.Connecting) {
                closeGatt()
                _state.value = State.Error(
                    "Tiempo de conexión agotado. Acerca el reloj, actívalo y vuelve a pulsar Conectar."
                )
                onConnectionChanged?.invoke(null, false)
            }
        }
        connectTimeout = timeout
        mainHandler.postDelayed(timeout, 20_000L)
    }

    private fun cancelConnectTimeout() {
        connectTimeout?.let { mainHandler.removeCallbacks(it) }
        connectTimeout = null
    }

    private fun failConnection(g: BluetoothGatt, message: String) {
        cancelConnectTimeout()
        closeGatt()
        _state.value = State.Error(message)
        onConnectionChanged?.invoke(null, false)
        try {
            g.close()
        } catch (_: Exception) { }
    }

    private fun connectionErrorMessage(status: Int): String = when (status) {
        8 -> "Conexión cancelada. Vuelve a intentarlo."
        19 -> "El dispositivo se desconectó. Activa «Broadcast HR» en Zepp/Amazfit."
        22 -> "Empareja el reloj en Ajustes → Bluetooth y vuelve a intentarlo."
        133 -> "Error BLE (133). Apaga y enciende el Bluetooth o acerca el reloj."
        else -> "No se pudo conectar (código $status). Prueba emparejarlo antes en Ajustes."
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            mainHandler.post {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                            g.discoverServices()
                        } else {
                            failConnection(g, connectionErrorMessage(status))
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        val wasConnecting = _state.value is State.Connecting
                        val wasConnected = _state.value is State.Connected
                        if (gatt == g) closeGatt()
                        when {
                            status != BluetoothGatt.GATT_SUCCESS && wasConnecting ->
                                _state.value = State.Error(connectionErrorMessage(status))
                            wasConnected && status != BluetoothGatt.GATT_SUCCESS ->
                                _state.value = State.Error("Se perdió la conexión con el reloj")
                            _state.value !is State.Error ->
                                _state.value = State.Idle
                        }
                        onConnectionChanged?.invoke(null, false)
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            mainHandler.post {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failConnection(g, "No se pudieron leer los servicios del reloj")
                    return@post
                }
                val service = g.getService(HR_SERVICE) ?: run {
                    failConnection(
                        g,
                        "Este dispositivo no expone pulso BLE. En Amazfit activa «Broadcast HR» en Zepp."
                    )
                    return@post
                }
                val ch = service.getCharacteristic(HR_MEASUREMENT) ?: run {
                    failConnection(g, "El reloj no envía medidas de pulso por BLE")
                    return@post
                }
                g.setCharacteristicNotification(ch, true)
                val descriptor = ch.getDescriptor(CCC_DESCRIPTOR) ?: run {
                    failConnection(g, "No se pudo activar las notificaciones de pulso")
                    return@post
                }
                val deviceName = try { g.device.name } catch (_: SecurityException) { null }
                val current = _state.value
                val foundDev = if (current is State.Connecting) current.device
                else FoundDevice(g.device.address, deviceName, 0)
                pendingConnectedDevice = foundDev.copy(name = deviceName ?: foundDev.name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(descriptor)
                    // En API antiguas onDescriptorWrite no siempre se invoca.
                    mainHandler.postDelayed({ confirmConnectedIfPending() }, 1200L)
                }
            }
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            mainHandler.post {
                if (descriptor.uuid != CCC_DESCRIPTOR) return@post
                val device = pendingConnectedDevice ?: return@post
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    pendingConnectedDevice = null
                    failConnection(g, "No se pudieron activar las notificaciones de pulso")
                    return@post
                }
                pendingConnectedDevice = null
                cancelConnectTimeout()
                _state.value = State.Connected(device)
                saveLastDevice(device.address)
                onConnectionChanged?.invoke(device.name, true)
            }
        }

        @Deprecated("Compat with older API")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            mainHandler.post { handlePayload(characteristic.uuid, characteristic.value) }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            mainHandler.post { handlePayload(characteristic.uuid, value) }
        }
    }

    private fun confirmConnectedIfPending() {
        val device = pendingConnectedDevice ?: return
        if (_state.value !is State.Connecting) return
        pendingConnectedDevice = null
        cancelConnectTimeout()
        _state.value = State.Connected(device)
        saveLastDevice(device.address)
        onConnectionChanged?.invoke(device.name, true)
    }

    private fun handlePayload(uuid: UUID, value: ByteArray?) {
        if (uuid != HR_MEASUREMENT || value == null || value.isEmpty()) return
        val flag = value[0].toInt()
        val hr = if (flag and 0x01 == 0) {
            value.getOrNull(1)?.toInt()?.and(0xFF) ?: return
        } else {
            val lo = value.getOrNull(1)?.toInt()?.and(0xFF) ?: return
            val hi = value.getOrNull(2)?.toInt()?.and(0xFF) ?: return
            lo or (hi shl 8)
        }
        if (hr in 25..240) {
            _heartRate.value = hr
            onHrSample?.invoke(hr)
        }
    }

    private fun saveLastDevice(address: String) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_ADDRESS, address).apply()
    }

    fun clearLastDevice() {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_ADDRESS).apply()
    }

    fun savedDeviceAddress(): String? =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ADDRESS, null)

    /**
     * Reconnects to the last paired device without scanning.
     * Call this from GpsTrackingService.onCreate() to restore connection automatically.
     */
    fun autoConnectSavedDevice() {
        val address = savedDeviceAddress() ?: return
        if (_state.value is State.Connected || _state.value is State.Connecting) return
        if (!hasPermissions() || !hasBluetooth()) return
        val device = FoundDevice(address, null, 0)
        connect(device)
    }

    companion object {
        val HR_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val HR_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        val CCC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private const val PREFS_FILE = "hr_ble_prefs"
        private const val KEY_LAST_ADDRESS = "last_device_address"

        @Volatile
        private var INSTANCE: HrBleManager? = null

        fun get(context: Context): HrBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HrBleManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private fun <T> MutableStateFlow<List<T>>.update(transform: (List<T>) -> List<T>) {
    value = transform(value)
}
