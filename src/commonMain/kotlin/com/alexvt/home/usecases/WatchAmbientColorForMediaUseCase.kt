package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.viewutils.tryGetAverageColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute(mediaItemFlow: Flow<MediaItem?>): Flow<Long> =
        mediaItemFlow.distinctUntilChanged().conflate().mapLatest { mediaItemOrNull ->
            mediaItemOrNull?.run {
                tryGetAverageColor(path, type)
            }?.toLong()?.also(setBluetoothLightColorUseCase::execute) ?: 0x00000000
        }

}
