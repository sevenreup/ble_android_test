package com.skybox.bletest.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanResult
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skybox.bletest.data.DeviceConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"

        internal const val NEW_DEVICE = -1
    }

    private lateinit var bluetoothLe: BluetoothLe
    var isScanning = mutableStateOf(false)

    val scanResults: MutableState<List<ScanResult>>
        get() = _scanResults
    private val _scanResults = mutableStateOf(listOf<ScanResult>())
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

    fun addScanResultIfNew(scanResult: ScanResult) {
        val deviceAddress = scanResult.deviceAddress.address

        if (_scanResultsMap.containsKey(deviceAddress).not()) {
            _scanResultsMap[deviceAddress] = scanResult
            _scanResults.value = _scanResultsMap.values.toList()
        }
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


    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(TAG, "startScan() called")
        viewModelScope.launch {
            Log.d(TAG, "bluetoothLe.scan() called")

            isScanning.value = true

            try {
                bluetoothLe.scan()
                    .collect {
                        Log.d(TAG, "bluetoothLe.scan() collected: ScanResult = $it")

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

    fun deviceConnection(position: Int): DeviceConnection {
        // Index 0 is Results page; Tabs for devices start from 1.
        return deviceConnections.elementAt(position - 1)
    }
}