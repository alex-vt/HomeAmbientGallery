package com.alexvt.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.alexvt.home.viewui.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import moe.tlaster.precompose.PreComposeWindow
import java.awt.Dimension

@ExperimentalFoundationApi
@FlowPreview
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
fun main() = application {
    val dependencies: AppDependencies = remember { AppDependencies::class.create() }
    PreComposeWindow(
        onCloseRequest = ::exitApplication,
        title = "Home Ambient Gallery",
        state = WindowState(width = 1024.dp, height = 800.dp),
        icon = BitmapPainter(useResource("ic_launcher.png", ::loadImageBitmap)),
    ) {
        window.minimumSize = Dimension(320, 700)
        Box(Modifier.onKeyEvent {
            if (it.type == KeyEventType.KeyDown) {
                when (it.key) {
                    Key.Escape -> window.placement = WindowPlacement.Floating
                    Key.F, Key.F11 -> window.placement = when (window.placement) {
                        WindowPlacement.Fullscreen -> WindowPlacement.Floating
                        else -> WindowPlacement.Fullscreen
                    }
                }
            }
            false
        }) {
            MainView(dependencies, Dispatchers.Default)
        }
    }
}