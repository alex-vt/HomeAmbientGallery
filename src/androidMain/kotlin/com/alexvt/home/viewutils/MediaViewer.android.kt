package com.alexvt.home.viewutils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmapOrNull
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.alexvt.home.App.Companion.androidAppContext
import com.alexvt.home.usecases.MediaType
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
actual fun MediaViewer(
    path: String,
    mediaType: MediaType,
    version: Long,
    isVisible: Boolean,
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    when (mediaType) {
        MediaType.VIDEO -> VideoPlayer(
            path, version, isVisible, mediaControlEvents, onMediaProgress
        )

        MediaType.IMAGE, MediaType.GIF -> ImageViewer(path, version, isVisible)
        MediaType.LOADING -> LoadingPlaceholder()
        MediaType.NONE -> NoContentPlaceholder()
    }
}

actual suspend fun tryGetAverageColor(path: String, mediaType: MediaType): Int? {
    return when (mediaType) {
        MediaType.VIDEO -> {
            try {
                MediaMetadataRetriever().apply {
                    setDataSource(androidAppContext, Uri.parse(path))
                }.run {
                    getFrameAtTime(0)?.getSparseAverageColor()?.also {
                        release()
                    }
                }
            } catch (throwable: Throwable) {
                return null
            }
        }

        MediaType.IMAGE, MediaType.GIF -> {
            val imageLoader = ImageLoader.Builder(androidAppContext)
                .components {
                    add(ImageDecoderDecoder.Factory())
                }.build()
            val request = ImageRequest.Builder(androidAppContext)
                .data(data = path)
                .build()
            val drawable = imageLoader.execute(request).drawable
            drawable?.toBitmapOrNull(100, 100)
                ?.copy(Bitmap.Config.RGB_565, false)
                ?.getSparseAverageColor()
        }

        else -> null
    }
}

private fun Bitmap.getSparseAverageColor(dimensionSampleCount: Int = 10): Int =
    (0 until width step width / dimensionSampleCount).flatMap { x ->
        (0 until height step height / dimensionSampleCount).map { y ->
            getColor(x, y).toArgb()
        }
    }.average().roundToInt()

@Composable
fun VideoPlayer(
    path: String,
    version: Long,
    isVisible: Boolean,
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // Playback parameters need to update only on recompositions with new path.
    var currentPositionPersisting by rememberSaveable(path, version) { mutableStateOf(0L) }
    var isPlayingPersisting by rememberSaveable(path) { mutableStateOf(true) }

    LaunchedEffect(isVisible) {
        with(exoPlayer) {
            launch {
                if (!isVisible) {
                    pause()
                    return@launch
                }
                val playbackPollIntervalMillis = 8L // 120 FPS
                val pausePollIntervalMillis = 100L
                while (true) {
                    currentPositionPersisting = currentPosition
                    isPlayingPersisting = isPlaying
                    val normalizedProgress = getNormalizedProgress()
                    onMediaProgress(MediaProgress(normalizedProgress, isProgressing = isPlaying))
                    delay(if (isPlaying) playbackPollIntervalMillis else pausePollIntervalMillis)
                }
            }
        }
    }

    LaunchedEffect(path, version) {
        with(exoPlayer) {
            setMediaItem(com.google.android.exoplayer2.MediaItem.fromUri(path))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            seekTo(currentPositionPersisting)
            if (isPlayingPersisting) play()

            mediaControlEvents.collect {
                when (it) {
                    MediaControlEvent.RESUME -> play()
                    MediaControlEvent.PAUSE -> pause()
                    MediaControlEvent.LEAP_FORWARD -> seekForward()
                    MediaControlEvent.LEAP_BACK -> seekBack()
                }
            }
        }
    }

    AndroidView(factory = {
        StyledPlayerView(context).apply {
            useController = false
            player = exoPlayer
        }
    }, Modifier.fillMaxSize())

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }
}

private fun ExoPlayer.getNormalizedProgress(): Double =
    if (duration > 0) {
        (currentPosition.toDouble() / duration).coerceAtMost(1.0)
    } else {
        0.0
    }

@Composable
fun ImageViewer(path: String, version: Long, isVisible: Boolean) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (isVisible) {
                add(ImageDecoderDecoder.Factory()) // gif support
            }
        }.build()
    key(version) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = path).build(), imageLoader = imageLoader
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
