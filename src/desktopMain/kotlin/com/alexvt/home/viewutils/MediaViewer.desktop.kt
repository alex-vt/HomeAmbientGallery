package com.alexvt.home.viewutils

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.alexvt.home.usecases.MediaType
import kotlinx.coroutines.flow.Flow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.io.File
import kotlin.math.roundToInt

@Composable
actual fun MediaViewer(
    path: String,
    mediaType: MediaType,
    isVisible: Boolean,
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    when (mediaType) {
        MediaType.VIDEO -> VideoPlayer(path, isVisible, onClick, onLongOrRightClick)
        MediaType.GIF -> GifAnimation(path, isVisible)
        MediaType.IMAGE -> ImageViewer(path)
        MediaType.LOADING -> LoadingPlaceholder()
        MediaType.NONE -> NoContentPlaceholder()
    }
}

actual suspend fun tryGetAverageColor(path: String, mediaType: MediaType): Int? {
    return when (mediaType) {
        MediaType.VIDEO -> null // todo implement
        MediaType.GIF -> {
            val bytes = File(path).readBytes()
            val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
            val bitmap = Bitmap().apply {
                allocPixels(codec.imageInfo)
                try {
                    codec.readPixels(this, 0)
                } catch (t: Throwable) {
                    return null // todo handle Incomplete input: A partial image was generated.
                }
            }
            bitmap.getSparseAverageColor()
        }

        MediaType.IMAGE -> {
            val bitmap = File(path).inputStream().buffered().use(::loadImageBitmap).asSkiaBitmap()
            bitmap.getSparseAverageColor()
        }

        else -> null
    }
}

private fun Bitmap.getSparseAverageColor(dimensionSampleCount: Int = 10): Int =
    (0 until width step width / dimensionSampleCount).flatMap { x ->
        (0 until height step height / dimensionSampleCount).map { y ->
            getColor(x, y)
        }
    }.average().roundToInt()

// Video renders on a native overlay surface that has to handle clicks directly.
// todo software rendering into Compose bitmap
@Composable
fun VideoPlayer(
    path: String,
    isVisible: Boolean,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    val state = rememberVideoPlayerState()
    state.isResumed = isVisible
    val progress by VideoPlayer(
        url = path,
        state = state,
        onClick = onClick,
        onLongOrRightClick = onLongOrRightClick,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun GifAnimation(path: String, isVisible: Boolean) {
    if (!isVisible) return
    val codec = remember(path) {
        val bytes = File(path).readBytes()
        Codec.makeFromData(Data.makeFromBytes(bytes))
    }
    val transition = rememberInfiniteTransition()
    val frameIndex by transition.animateValue(
        initialValue = 0,
        targetValue = codec.frameCount - 1,
        Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                for ((index, frame) in codec.framesInfo.withIndex()) {
                    index at durationMillis
                    // preventing division by zero in zero duration frame math
                    val defaultDurationMillis = 100
                    val minDurationMillis = 10
                    val frameDuration =
                        if (frame.duration >= minDurationMillis) {
                            frame.duration
                        } else {
                            defaultDurationMillis
                        }
                    durationMillis += frameDuration
                }
            }
        )
    )

    val bitmap = Bitmap().apply {
        allocPixels(codec.imageInfo)
        try {
            codec.readPixels(this, frameIndex)
        } catch (t: Throwable) {
            // todo handle Incomplete input: A partial image was generated.
        }
    }

    Image(
        bitmap = bitmap.asComposeImageBitmap(),
        contentDescription = "",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ImageViewer(path: String) {
    Image(
        bitmap = File(path).inputStream().buffered().use(::loadImageBitmap),
        contentDescription = "",
        modifier = Modifier.fillMaxSize()
    )
}
