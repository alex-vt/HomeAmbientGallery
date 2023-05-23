package com.alexvt.home.viewui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Colors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexvt.home.AppDependencies
import com.alexvt.home.viewmodels.MainViewModel
import com.alexvt.home.viewutils.MediaControlEvent
import com.alexvt.home.viewutils.MediaProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.ui.viewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun MainView(
    dependencies: AppDependencies,
    backgroundDispatcher: CoroutineDispatcher,
) {
    val viewModel = viewModel(MainViewModel::class) {
        MainViewModel(dependencies.mainViewModelUseCases, backgroundDispatcher)
    }
    val uiState by viewModel.uiStateFlow.collectAsState()

    MaterialTheme(
        colors = with(uiState.theme.color) {
            Colors(
                primary = Color(background.bright),
                primaryVariant = Color(background.dim),
                secondary = Color(background.accentBright),
                secondaryVariant = Color(background.accent),
                background = Color(background.normal),
                surface = Color(background.dim),
                error = Color(background.normal),
                onPrimary = Color(text.accentBright),
                onSecondary = Color(text.bright),
                onBackground = Color(text.normal),
                onSurface = Color(text.dim),
                onError = Color(text.error),
                isLight = uiState.isThemeLight,
            )
        },
        typography = with(uiState.theme.font.size) {
            Typography(
                body1 = TextStyle(fontSize = big.sp),
                body2 = TextStyle(fontSize = normal.sp),
                button = TextStyle(fontSize = normal.sp),
                subtitle1 = TextStyle(fontSize = small.sp),
                subtitle2 = TextStyle(fontSize = small.sp),
            )
        }
    ) {
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
        )
        val coroutineScope = rememberCoroutineScope()
        var contentWidthDp by remember { mutableStateOf(0.dp) }
        val localDensity = LocalDensity.current
        val focusRequester = remember { FocusRequester() }
        val mediaControlEvents = remember {
            MutableSharedFlow<MediaControlEvent>(
                extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
        var mediaProgress by remember { mutableStateOf(MediaProgress()) }

        BottomSheetScaffold(
            sheetBackgroundColor = Color.Transparent, // the peeking part won't obstruct the image
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 60.dp,
            sheetElevation = 0.dp, // for fully invisible peeking part
            sheetContent = {
                with(viewModel) {
                    BottomSheetView(
                        bottomSheetState = uiState.bottomSheetState,
                        mediaProgress = mediaProgress,
                        isLoading = uiState.mediaState.isUpdatingContent,
                        isExpanded = bottomSheetScaffoldState.bottomSheetState.isExpanded,
                        onMediaControlClick = mediaControlEvents::tryEmit,
                        onSwitchMediaType = {
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                switchMediaType(it)
                            }
                        },
                        onSelectSorting = {
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                selectSorting(it)
                            }
                        },
                        onSwitchAlbum = {
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                switchAlbum(it)
                            }
                        },
                        onToggleTagsVisibility = ::toggleTagsVisibility,
                        onToggleTagsEditing = ::toggleTagsEditing,
                        onSwitchTag = { tagText, isToToggleExclusion ->
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                switchTag(tagText, isToToggleExclusion)
                            }
                        },
                        onIncludeAllTags = ::selectIncludeAllTags,
                        onExcludeAllTags = ::selectExcludeAllTags,
                        onClearAllTags = ::selectClearAllTags,
                        onSelectSlideshowInterval = {
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                selectSlideshowInterval(it)
                            }
                        },
                        onToggleAmbientColorSync = ::toggleAmbientColorSync,
                        onBluetoothColorSelection = {
                            bottomSheetScaffoldState.ifNoDrag(coroutineScope) {
                                selectBluetoothLightColor(it)
                            }
                        },
                        onToggleBottomSheetClick = {
                            bottomSheetScaffoldState.bottomSheetState.toggle(coroutineScope)
                        },
                        onSettingsClick = ::openSettings
                    )
                }
            },
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    contentWidthDp = with(localDensity) { coordinates.size.width.toDp() }
                }.onKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.DirectionUp -> bottomSheetScaffoldState.bottomSheetState
                                .pull(coroutineScope, isDown = false)

                            Key.DirectionDown -> bottomSheetScaffoldState.bottomSheetState
                                .pull(coroutineScope, isDown = true)

                            Key.DirectionLeft -> viewModel
                                .showNextMediaItem(isPreviousInstead = true)

                            Key.DirectionRight -> viewModel
                                .showNextMediaItem(isPreviousInstead = false)
                        }
                    }
                    false
                }.focusRequester(focusRequester).onFocusEvent {
                    if (!it.hasFocus) try {
                        focusRequester.requestFocus()
                    } catch (t: Throwable) {
                        // brute forced focus capture, todo improve, also when closing settings
                    }
                }
        ) {
            val density = LocalDensity.current.density
            Box(
                Modifier.fillMaxWidth()
                    .height(
                        (bottomSheetScaffoldState.bottomSheetState.requireOffset() / density + 60)
                            .dp
                    )
                    .background(MaterialTheme.colors.surface)
            ) {
                MediaView(
                    mediaState = uiState.mediaState,
                    mediaControlEvents = mediaControlEvents,
                    onMediaProgress = { mediaProgress = it },
                    onClick = { viewModel.showNextMediaItem(isPreviousInstead = false) },
                    onLongOrRightClick = { viewModel.showNextMediaItem(isPreviousInstead = true) },
                )
            }
        }

        if (uiState.isSettingsEditorShown) {
            SettingsView(
                editableSettings = uiState.editableSettings,
                onSaveClick = viewModel::saveSettings,
                onDiscardClick = viewModel::discardSettings,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun BottomSheetScaffoldState.ifNoDrag(coroutineScope: CoroutineScope, action: () -> Unit) {
    coroutineScope.launch {
        val initialBottomSheetOffset = bottomSheetState.requireOffset()
        val dragDetectionDelayMillis = 50L
        delay(dragDetectionDelayMillis)
        val currentBottomSheetOffset = bottomSheetState.requireOffset()
        val isDraggingBottomSheet = initialBottomSheetOffset != currentBottomSheetOffset
        if (!isDraggingBottomSheet) {
            action()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
private fun BottomSheetState.toggle(coroutineScope: CoroutineScope) {
    pull(coroutineScope, isDown = isExpanded)
}

@OptIn(ExperimentalMaterialApi::class)
private fun BottomSheetState.pull(coroutineScope: CoroutineScope, isDown: Boolean) {
    coroutineScope.launch {
        if (isDown) collapse() else expand()
    }
}