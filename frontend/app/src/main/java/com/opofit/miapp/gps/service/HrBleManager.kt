package com.opofit.miapp.gps.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
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

    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var onHrSample: ((Int) -> Unit)? = null
    private var onConnectionChanged: ((deviceName: String?, connected: Boolean) -> Unit)? = null

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

    fun startScan() {
        if (!hasBluetooth()) {
            _state.value = State.Error("Activa el Bluetooth en el sistema")
            return
        }
        if (!hasPermissions()) {
            _state.value = State.Error("Faltan permisos de Bluetooth")
            return
        }
        stopScan()
        _found.value = emptyList()
        _state.value = State.Scanning

        val scanner = adapter?.bluetoothLeScanner ?: run {
            _state.value = State.Error("BLE scanner no disponible")
            return
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HR_SERVICE))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val item = FoundDevice(device.address, device.name, result.rssi)
                _found.update {
                    if (it.any { d -> d.address == item.address }) it else it + item
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = State.Error("Escaneo BLE fallo ($errorCode)")
            }
        }
        scanCallback = callback
        scanner.startScan(listOf(filter), settings, callback)
    }

    fun stopScan() {
        scanCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        if (_state.value is State.Scanning) _state.value = State.Idle
    }

    fun connect(device: FoundDevice) {
        if (!hasPermissions()) {
            _state.value = State.Error("Faltan permisos de Bluetooth")
            return
        }
        stopScan()
        disconnect()
        val remote = adapter?.getRemoteDevice(device.address) ?: run {
            _state.value = State.Error("Dispositivo no encontrado")
            return
        }
        _state.value = State.Connecting(device)
        gatt = remote.connectGatt(context, true, gattCallback)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _heartRate.value = null
        onConnectionChanged?.invoke(null, false)
        _state.value = State.Idle
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = State.Idle
                onConnectionChanged?.invoke(null, false)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(HR_SERVICE) ?: run {
                _state.value = State.Error("El dispositivo no expone Heart Rate")
                return
            }
            val ch = service.getCharacteristic(HR_MEASUREMENT) ?: return
            g.setCharacteristicNotification(ch, true)
            val descriptor = ch.getDescriptor(CCC_DESCRIPTOR) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
            val deviceName = try { g.device.name } catch (_: SecurityException) { null }
            val current = _state.value
            val foundDev = if (current is State.Connecting) current.device
            else FoundDevice(g.device.address, deviceName, 0)
            _state.value = State.Connected(foundDev.copy(name = deviceName ?: foundDev.name))
            onConnectionChanged?.invoke(deviceName ?: foundDev.name, true)
        }

        @Deprecated("Compat with older API")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handlePayload(characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePayload(characteristic.uuid, value)
        }
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

    companion object {
        val HR_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val HR_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        val CCC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

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
