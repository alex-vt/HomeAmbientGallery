package com.alexvt.home.viewutils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlin.math.roundToInt


@Composable
actual fun MediaViewer(
    path: String,
    mediaType: MediaType,
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    when (mediaType) {
        MediaType.VIDEO -> VideoPlayer(path, mediaControlEvents, onMediaProgress)
        MediaType.IMAGE, MediaType.GIF -> ImageViewer(path)
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
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(Unit) {
        val progressPollIntervalMillis = 8L // 120 FPS
        while (true) {
            with(exoPlayer) {
                val normalizedProgress =
                    if (duration > 0) {
                        (currentPosition.toDouble() / duration).coerceAtMost(1.0)
                    } else {
                        0.0
                    }
                onMediaProgress(MediaProgress(normalizedProgress, isProgressing = isPlaying))
            }
            delay(progressPollIntervalMillis)
        }
    }

    LaunchedEffect(path) {
        exoPlayer.setMediaItem(com.google.android.exoplayer2.MediaItem.fromUri(path))
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare()
        exoPlayer.play()
        mediaControlEvents.collect {
            Log.d("HomeAmbientGallery", "" + it)
            when (it) {
                MediaControlEvent.RESUME -> exoPlayer.play()
                MediaControlEvent.PAUSE -> exoPlayer.pause()
                MediaControlEvent.LEAP_FORWARD -> exoPlayer.seekForward()
                MediaControlEvent.LEAP_BACK -> exoPlayer.seekBack()
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

@Composable
fun ImageViewer(path: String) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory())
        }.build()
    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context).data(data = path).build(), imageLoader = imageLoader
        ),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
}
