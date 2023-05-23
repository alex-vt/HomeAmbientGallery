package com.alexvt.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.alexvt.home.viewui.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import moe.tlaster.precompose.PreComposeWindow
import java.awt.Dimension

@ExperimentalFoundationApi
@FlowPreview
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
fun main() = application {
    val dependencies: AppDependencies = remember { AppDependencies::class.create() }
    val windowState = rememberWindowState(width = 1024.dp, height = 800.dp)
    val windowVisibilityFlow = MutableStateFlow(true)
    PreComposeWindow(
        onCloseRequest = ::exitApplication,
        title = "Home Ambient Gallery",
        state = windowState,
        icon = BitmapPainter(useResource("ic_launcher.png", ::loadImageBitmap)),
    ) {
        window.minimumSize = Dimension(320, 700)
        window.addWindowStateListener {
            windowVisibilityFlow.tryEmit(!windowState.isMinimized)
        }

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
            val isShown by windowVisibilityFlow.collectAsState()
            MainView(isShown, dependencies, Dispatchers.Default)
        }
    }
}