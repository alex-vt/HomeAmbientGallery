package com.alexvt.home.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class GenericBluetoothLightRepository {

    actual suspend fun setColor(macAddress: String, red: UByte, green: UByte, blue: UByte) {
        val characteristic = "0x0009"
        val data = "56${red.toHexString()}${green.toHexString()}${blue.toHexString()}00f0aa"
        val command = "gatttool -b $macAddress --char-write-req -a $characteristic -n $data"
        withContext(Dispatchers.IO) {
            Runtime.getRuntime().exec(command)
        }
    }

    private fun UByte.toHexString(): String =
        toString(16).padStart(2, '0')

}