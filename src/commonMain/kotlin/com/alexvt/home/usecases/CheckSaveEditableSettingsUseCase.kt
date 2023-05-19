package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

data class EditableSettings(
    val albumPaths: String,
    val tagsCsvPath: String,
    val bluetoothLightsMacAddresses: String,
)

@AppScope
@Inject
class CheckSaveEditableSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(editableSettings: EditableSettings, coroutineScope: CoroutineScope): Boolean {
        // todo error handling
        coroutineScope.launch {
            with(settingsRepository.readSettings()) {
                settingsRepository.updateSettings(
                    copy(
                        bluetoothLightsSettings = bluetoothLightsSettings.copy(
                            bluetoothMacAddresses = editableSettings.bluetoothLightsMacAddresses
                                .lines(),
                        ),
                        albumViewingSettings = albumViewingSettings.copy(
                            folderPaths = editableSettings.albumPaths.lines(),
                            tagsCsvPath = editableSettings.tagsCsvPath,
                        )
                    )
                )
            }
        }
        return true
    }

}
