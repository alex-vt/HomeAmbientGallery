package com.alexvt.home.repositories

expect class GenericBluetoothLightRepository() {

    fun setColor(macAddress: String, red: UByte, green: UByte, blue: UByte)

}