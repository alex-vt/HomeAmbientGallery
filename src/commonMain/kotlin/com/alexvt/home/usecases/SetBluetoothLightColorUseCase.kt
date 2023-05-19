package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.GenericBluetoothLightRepository
import com.alexvt.home.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class SetBluetoothLightColorUseCase(
    private val genericBluetoothLightRepository: GenericBluetoothLightRepository,
    private val settingsRepository: SettingsRepository,
) {

    fun execute(color: Long) {
        val bluetoothLightsMacAddresses =
            settingsRepository.readSettings().bluetoothLightsSettings.bluetoothMacAddresses
        bluetoothLightsMacAddresses.forEach { macAddress ->
            val red = ((color shr 16) and 0xFF).toUByte()
            val green = ((color shr 8) and 0xFF).toUByte()
            val blue = (color and 0xFF).toUByte()
            genericBluetoothLightRepository.setColor(macAddress, red, green, blue)
        }

    }

}
