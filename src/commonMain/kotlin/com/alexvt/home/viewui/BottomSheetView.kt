package com.alexvt.home.viewui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SnippetFolder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alexvt.home.viewmodels.MainViewModel
import com.alexvt.home.viewutils.ActionButton
import com.alexvt.home.viewutils.ActionButtons
import com.alexvt.home.viewutils.LinePatternOverlayView
import com.alexvt.home.viewutils.MediaControlEvent
import com.alexvt.home.viewutils.MediaProgress
import com.alexvt.home.viewutils.SingleActionButton
import com.alexvt.home.viewutils.StandardButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomSheetView(
    bottomSheetState: MainViewModel.BottomSheetState,
    mediaProgress: MediaProgress,
    isExpanded: Boolean,
    isLoading: Boolean,
    onMediaControlClick: (MediaControlEvent) -> Unit,
    onSwitchMediaType: (String) -> Unit,
    onSelectSorting: (String) -> Unit,
    onSwitchAlbum: (String) -> Unit,
    onToggleTagsVisibility: () -> Unit,
    onToggleTagsEditing: () -> Unit,
    onSwitchTag: (String, Boolean) -> Unit,
    onIncludeAllTags: () -> Unit,
    onExcludeAllTags: () -> Unit,
    onClearAllTags: () -> Unit,
    onSelectSlideshowInterval: (String) -> Unit,
    onToggleAmbientColorSync: () -> Unit,
    onBluetoothColorSelection: (Long) -> Unit,
    onToggleBottomSheetClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // Peeking part
        Box(
            Modifier.fillMaxWidth().height(60.dp)
        ) {
            // Progress bar indicator & controls
            if (bottomSheetState.isMediaProgressShown) {
                Box(
                    Modifier.fillMaxWidth().height(10.dp)
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.3f))
                )
                Box(
                    Modifier.fillMaxWidth(mediaProgress.normalizedProgress.toFloat()).height(10.dp)
                        .align(Alignment.BottomStart).padding(3.dp)
                        .background(MaterialTheme.colors.onSecondary.copy(alpha = 0.4f))
                )
                Row(Modifier.align(Alignment.Center)) {
                    RoundTranslucentButton(
                        icon = Icons.Default.FastRewind,
                        contentDescription = "Leap back",
                        isExtraTranslucent = !isExpanded,
                        onClick = { onMediaControlClick(MediaControlEvent.LEAP_BACK) },
                    )
                    RoundTranslucentButton(
                        icon = if (mediaProgress.isProgressing) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Pause/Resume",
                        isExtraTranslucent = !isExpanded,
                        onClick = {
                            onMediaControlClick(
                                if (mediaProgress.isProgressing) {
                                    MediaControlEvent.PAUSE
                                } else {
                                    MediaControlEvent.RESUME
                                }
                            )
                        },
                    )
                    RoundTranslucentButton(
                        icon = Icons.Default.FastForward,
                        contentDescription = "Leap forward",
                        isExtraTranslucent = !isExpanded,
                        onClick = { onMediaControlClick(MediaControlEvent.LEAP_FORWARD) },
                    )
                }
            }
            // Loading bar
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(10.dp).align(Alignment.BottomCenter)) {
                    LinePatternOverlayView(
                        lineColor = MaterialTheme.colors.secondaryVariant,
                        linePitchDp = 20.dp,
                        lineThicknessDp = 10.dp,
                        isAnimate = true,
                    )
                }
            }
            // Settings
            if (isExpanded) {
                RoundTranslucentButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    isExtraTranslucent = false,
                    onClick = onSettingsClick,
                )
            }
            // Bottom sheet toggle
            Box(Modifier.align(Alignment.CenterEnd)) {
                RoundTranslucentButton(
                    icon = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Select what to view",
                    isExtraTranslucent = !isExpanded,
                    onClick = onToggleBottomSheetClick,
                )
            }
        }

        // visible when expanded

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colors.background)
                .padding(horizontal = 8.dp)
        ) {
            if (!bottomSheetState.isEditingTags) {

                Spacer(Modifier.height(8.dp))
                TextLabel("Show from albums:")
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 80.dp).verticalScroll(rememberScrollState())) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bottomSheetState.albumSelections.forEach { selection ->
                            Box(Modifier.padding(bottom = 8.dp)) {
                                SingleActionButton(
                                    text = selection.name,
                                    isSelected = selection.isSelected,
                                    onClick = { onSwitchAlbum(selection.name) },
                                )
                            }
                        }
                    }
                }

                TextLabel("Media forms:")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bottomSheetState.mediaTypeSelections.run {
                        val actionIconMap = mapOf(
                            "Image" to ActionButton(
                                text = "Image",
                                icon = Icons.Outlined.Image,
                                isAlwaysShown = true,
                            ),
                            "Gif" to ActionButton(
                                text = "Gif",
                                icon = Icons.Default.Gif,
                                isAlwaysShown = true,
                            ),
                            "Video" to ActionButton(
                                text = "Video",
                                icon = Icons.Default.Movie,
                                isAlwaysShown = true,
                            ),
                        )
                        map { selection ->
                            actionIconMap.getValue(selection.name)
                                .copy(isSelected = selection.isSelected)
                        }.forEach { actionButton ->
                            SingleActionButton(
                                text = actionButton.text,
                                icon = actionButton.icon,
                                isSelected = actionButton.isSelected,
                                onClick = { onSwitchMediaType(actionButton.text) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextLabel("Sort:")
                Spacer(Modifier.height(4.dp))
                ActionButtons(
                    actionButtons = bottomSheetState.sortingSelections.run {
                        val actionIconMap = mapOf(
                            "Random" to ActionButton(
                                text = "Random",
                                icon = Icons.Default.Shuffle,
                                isAlwaysShown = true,
                            ),
                            "Date new to old" to ActionButton(
                                text = "Date new to old",
                                icon = Icons.Default.AutoAwesome,
                                isAlwaysShown = true,
                            ),
                            "Date old to new" to ActionButton(
                                text = "Date old to new",
                                icon = Icons.Default.SnippetFolder,
                                isAlwaysShown = true,
                            ),
                            "Name a to z" to ActionButton(
                                text = "Name a to z",
                                icon = Icons.Default.FormatSize,
                                isAlwaysShown = true,
                            ),
                            "Name z to a" to ActionButton(
                                text = "Name z to a",
                                icon = Icons.Default.TextFields,
                                isAlwaysShown = false,
                            ),
                            "Size small to big" to ActionButton(
                                text = "Size small to big",
                                icon = Icons.Default.TrendingUp,
                                isAlwaysShown = true,
                            ),
                            "Size big to small" to ActionButton(
                                text = "Size big to small",
                                icon = Icons.Default.TrendingDown,
                                isAlwaysShown = true,
                            ),
                        )
                        map { selection ->
                            actionIconMap.getValue(selection.name)
                                .copy(isSelected = selection.isSelected)
                        }
                    },
                ) { selection ->
                    onSelectSorting(selection)
                }

                Spacer(Modifier.height(8.dp))
                TextLabel("Slideshow, slide time:")
                Spacer(Modifier.height(4.dp))
                ActionButtons(
                    actionButtons = bottomSheetState.slideshowIntervalSelections.run {
                        val minVisibleButtons = 3
                        mapIndexed { index, selection ->
                            ActionButton(
                                text = selection.name,
                                icon = if (selection.name == "Off") {
                                    Icons.Default.PlayDisabled
                                } else {
                                    Icons.Default.Slideshow
                                },
                                isSelected = selection.isSelected,
                                isAlwaysShown = index < minVisibleButtons,
                            )
                        }
                    },
                ) { selection ->
                    onSelectSlideshowInterval(selection)
                }
            }

            if (bottomSheetState.tagSelections.isNotEmpty()) {
                Row {
                    SettingSwitch(
                        label = "Show tags",
                        value = bottomSheetState.areTagsShowing,
                    ) {
                        onToggleTagsVisibility()
                    }
                    if (bottomSheetState.areTagsShowing) {
                        Spacer(Modifier.width(8.dp))
                        SettingSwitch(
                            label = "Edit",
                            value = bottomSheetState.isEditingTags,
                        ) {
                            onToggleTagsEditing()
                        }
                    }
                }
                if (bottomSheetState.areTagsShowing) {
                    Column(
                        Modifier // more space to edited tags
                            .heightIn(max = if (bottomSheetState.isEditingTags) 320.dp else 120.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            bottomSheetState.tagSelections.forEach { selection ->
                                Box(Modifier.padding(bottom = 8.dp)) {
                                    SingleActionButton(
                                        text = selection.name,
                                        isSelected = selection.isIncluded || selection.isExcluded,
                                        isNegativelySelected = selection.isExcluded,
                                        isAccentuated = selection.isHit,
                                        onClick = {
                                            onSwitchTag(
                                                selection.name,
                                                false
                                            )
                                        }, // inclusion
                                        onLongOrRightClick = {
                                            onSwitchTag(
                                                selection.name,
                                                true
                                            )
                                        }, // exclusion
                                    )
                                }
                            }
                        }
                    }
                    if (!bottomSheetState.isEditingTags) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StandardButton(text = "Include all", onClick = onIncludeAllTags)
                            StandardButton(text = "Exclude all", onClick = onExcludeAllTags)
                            StandardButton(text = "Clear all", onClick = onClearAllTags)
                        }
                    }
                }
            }
            if (bottomSheetState.areBluetoothOptionsShowing && !bottomSheetState.isEditingTags) {
                SettingSwitch(
                    label = "Match background & Bluetooth lights",
                    value = bottomSheetState.isAmbientColorSyncEnabled,
                ) {
                    onToggleAmbientColorSync()
                }
                TextLabel("Or set Bluetooth lights color:")
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 40.dp).verticalScroll(rememberScrollState())) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bottomSheetState.bluetoothManualColorOptions.forEach { color ->
                            Box(Modifier.padding(bottom = 8.dp)) {
                                SingleActionButton(
                                    icon = Icons.Filled.Lightbulb,
                                    iconColor = Color(color),
                                    isSelected = false,
                                    onClick = { onBluetoothColorSelection(color) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        fontSize = MaterialTheme.typography.body2.fontSize,
        color = MaterialTheme.colors.onSurface,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    onNewValue: (Boolean) -> Unit,
) {
    var checkedState by remember(value) { mutableStateOf(value) }
    Row(horizontalArrangement = Arrangement.Start) {
        Text(
            text = label,
            maxLines = 1,
            fontSize = MaterialTheme.typography.body2.fontSize,
            color = MaterialTheme.colors.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterVertically).weight(1f, fill = false)
        )
        Switch(
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colors.primary,
                checkedThumbColor = MaterialTheme.colors.secondary,
            ),
            checked = checkedState,
            onCheckedChange = { newValue ->
                checkedState = newValue
                onNewValue(newValue)
            }
        )
    }
}

@Composable
fun RoundTranslucentButton(
    icon: ImageVector,
    contentDescription: String,
    isExtraTranslucent: Boolean,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        modifier = Modifier.padding(14.dp).size(32.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
        ),
        backgroundColor = MaterialTheme.colors.primaryVariant
            .copy(alpha = if (isExtraTranslucent) 0.3f else 0.5f),
        contentColor = MaterialTheme.colors.onSecondary
            .copy(alpha = if (isExtraTranslucent) 0.5f else 0.7f),
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}
