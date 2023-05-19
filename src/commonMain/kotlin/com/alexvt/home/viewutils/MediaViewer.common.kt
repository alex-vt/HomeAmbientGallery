package com.alexvt.home.viewutils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alexvt.home.usecases.MediaType

@Composable
expect fun MediaViewer(
    path: String,
    mediaType: MediaType,
    onClick: () -> Unit,
    onLongOrRightClick: () -> Unit,
)

expect fun tryGetAverageColor(path: String, mediaType: MediaType): Int?

@Composable
fun LoadingPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        Icon(
            Icons.Default.HourglassTop,
            tint = LocalContentColor.current.copy(alpha = 0.7f),
            contentDescription = "Loading",
            modifier = Modifier.size(24.dp).align(Alignment.Center)
        )
    }
}

@Composable
fun NoContentPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = "Nothing to show",
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

