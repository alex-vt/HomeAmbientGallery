package com.alexvt.home.viewutils

import androidx.compose.runtime.Composable
import com.alexvt.home.usecases.MediaType

@Composable
actual fun MediaViewer(
    path: String,
    mediaType: MediaType,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
) {
    // todo implement
}

actual fun tryGetAverageColor(path: String, mediaType: MediaType): Int? {
    throw NotImplementedError() // todo implement
}
