package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.viewutils.tryGetAverageColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class WatchAmbientColorForMediaUseCase(
    private val setBluetoothLightColorUseCase: SetBluetoothLightColorUseCase,
) {

    /**
     * Ambient color will be transparent for media items where it couldn't be calculated.
     * Calculated ambient colors are dispatched also to Bluetooth lights.
     */
    fun execute(mediaItem: MediaItem, coroutineScope: CoroutineScope): Flow<Long> =
        flowOf(mediaItem).map { mediaItemOrNull ->
            mediaItemOrNull.run {
                tryGetAverageColor(path, type)
            }?.toLong()?.also { setBluetoothLightColorUseCase.execute(it, coroutineScope) }
                ?: 0x00000000
        }

}
