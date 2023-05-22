package com.alexvt.home.viewui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.alexvt.home.viewmodels.MainViewModel
import com.alexvt.home.viewutils.LinePatternOverlayView
import com.alexvt.home.viewutils.MediaControlEvent
import com.alexvt.home.viewutils.MediaProgress
import com.alexvt.home.viewutils.MediaViewer
import kotlinx.coroutines.flow.Flow

@ExperimentalFoundationApi
@Composable
fun MediaView(
    mediaState: MainViewModel.MediaState,
    mediaControlEvents: Flow<MediaControlEvent>,
    onMediaProgress: (MediaProgress) -> Unit,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongOrRightClick()
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press) {
                        if (event.buttons.isSecondaryPressed) {
                            onLongOrRightClick()
                        }
                    }
                }
            }.background(Color(mediaState.ambientColor).copy(alpha = 0.3f))
    ) {
        // some media type implementations need to handle clicks directly
        MediaViewer(
            path = mediaState.currentMediaItem.path,
            mediaType = mediaState.currentMediaItem.type,
            mediaControlEvents,
            onMediaProgress,
            onClick,
            onLongOrRightClick,
        )
        if (mediaState.isUpdatingContent) {
            LinePatternOverlayView(
                lineColor = MaterialTheme.colors.background.copy(alpha = 0.6f),
                backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.1f),
                linePitchDp = 30.dp,
                lineThicknessDp = 1.dp,
                isAnimate = false,
            )
        }
        if (mediaState.isCounterVisible) {
            Text(
                with(mediaState) { "$naturalCounter of $totalCount" },
                fontSize = MaterialTheme.typography.subtitle2.fontSize,
                color = MaterialTheme.colors.onSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(14.dp)
                    .clip(shape = RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 6.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
