package com.alexvt.home.repositories

expect class GenericBluetoothLightRepository() {

    suspend fun setColor(macAddress: String, red: UByte, green: UByte, blue: UByte)

}