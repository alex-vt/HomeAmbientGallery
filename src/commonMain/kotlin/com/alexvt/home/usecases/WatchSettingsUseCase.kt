package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.Settings
import com.alexvt.home.repositories.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class WatchSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(): StateFlow<Settings> {
        return settingsRepository.watchSettings()
    }

}
