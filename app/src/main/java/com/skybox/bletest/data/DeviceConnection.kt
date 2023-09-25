package com.skybox.bletest.data

import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattService
import java.util.UUID
import kotlinx.coroutines.Job

class DeviceConnection(
    val bluetoothDevice: BluetoothDevice
) {
    var job: Job? = null
    var onClickReadCharacteristic: OnClickCharacteristic? = null
    var onClickWriteCharacteristic: OnClickCharacteristic? = null
    var status = Status.DISCONNECTED
    var services = emptyList<GattService>()

    private val values = mutableMapOf<UUID, ByteArray?>()

    fun storeValueFor(characteristic: GattCharacteristic, value: ByteArray?) {
        values[characteristic.uuid] = value
    }

    fun valueFor(characteristic: GattCharacteristic): ByteArray? {
        return values[characteristic.uuid]
    }

}


interface OnClickCharacteristic {
    fun onClick(deviceConnection: DeviceConnection, characteristic: GattCharacteristic)
}

enum class Status {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}