package com.skybox.bletest.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skybox.bletest.data.DeviceConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.nanoseconds
import kotlin.time.toDuration

class ScannerViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"

        internal const val NEW_DEVICE = -1
    }

    private lateinit var bluetoothLe: BluetoothLe
    var isScanning = mutableStateOf(false)

    val scanResults: Flow<List<ScanResult>>
        get() = _scanResults.map {
            it.filter { scanResult ->
                scanResult.isConnectable()
            }
        }

    private val _scanResults = MutableStateFlow<List<ScanResult>>(listOf())
    private val _scanResultsMap = mutableMapOf<String, ScanResult>()

    internal val deviceConnections: Set<DeviceConnection> get() = _deviceConnections
    private val _deviceConnections = mutableSetOf<DeviceConnection>()

    override fun onCleared() {
        super.onCleared()

        _deviceConnections.forEach { it.job?.cancel() }
    }

    fun setup(context: Context) {
        bluetoothLe = BluetoothLe(context)
    }

    private fun addScanResultIfNew(scanResult: ScanResult) {
        val deviceAddress = scanResult.deviceAddress.address

        _scanResultsMap[deviceAddress] = scanResult
        _scanResults.value = _scanResultsMap.values.toList()
    }

    fun addDeviceConnectionIfNew(bluetoothDevice: BluetoothDevice): Int {
        val deviceConnection = DeviceConnection(bluetoothDevice)

        val indexOf = _deviceConnections.map { it.bluetoothDevice }.indexOf(bluetoothDevice)
        if (indexOf != -1) {
            // Index 0 is Results page; Tabs for devices start from 1.
            return indexOf + 1
        }

        _deviceConnections.add(deviceConnection)
        return NEW_DEVICE
    }

    fun remove(bluetoothDevice: BluetoothDevice) {
        val deviceConnection = _deviceConnections.find { it.bluetoothDevice == bluetoothDevice }
        deviceConnection?.job?.cancel("MANUAL_DISCONNECT")
        deviceConnection?.job = null

        _deviceConnections.remove(deviceConnection)
    }

    private fun startPeriodicTask() {
   viewModelScope.launch(Dispatchers.Default) {
            while (isScanning.value) {
                Log.e(TAG, "startPeriodicTask: cleaning")
                removeUnavailableDevices()
                delay(10000) // 10 seconds
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(TAG, "startScan() called")
        viewModelScope.launch {
            Log.d(TAG, "bluetoothLe.scan() called")
            isScanning.value = true
            startPeriodicTask()
            try {
                bluetoothLe.scan()
                    .collect {
                        Log.d(
                            TAG,
                            "bluetoothLe.scan() collected: ScanResult = ${it.device.bondState}"
                        )
                        addScanResultIfNew(it)
                    }
            } catch (exception: Exception) {
                isScanning.value = false

                if (exception is CancellationException) {
                    Log.e(TAG, "bluetoothLe.scan() CancellationException", exception)
                }
            }
        }
    }

    fun removeUnavailableDevices() {
        val currentTime = System.currentTimeMillis()
        val devicesToKeep = mutableListOf<ScanResult>()

        for (scannedDevice in _scanResults.value) {
            if (currentTime - scannedDevice.timestampNanos < DEVICE_TIMEOUT * 1000000) {
                devicesToKeep.add(scannedDevice)
            }
        }

        _scanResults.value = devicesToKeep
    }

    fun deviceConnection(position: Int): DeviceConnection {
        // Index 0 is Results page; Tabs for devices start from 1.
        return deviceConnections.elementAt(position - 1)
    }
}

private const val DEVICE_TIMEOUT: Long = 10000