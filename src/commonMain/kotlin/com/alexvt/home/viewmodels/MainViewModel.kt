package com.alexvt.home.viewmodels

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.Settings
import com.alexvt.home.repositories.Theme
import com.alexvt.home.usecases.CheckSaveEditableSettingsUseCase
import com.alexvt.home.usecases.EditableSettings
import com.alexvt.home.usecases.MediaItem
import com.alexvt.home.usecases.MediaSelectionParams
import com.alexvt.home.usecases.MediaType
import com.alexvt.home.usecases.SetBluetoothLightColorUseCase
import com.alexvt.home.usecases.SortingType
import com.alexvt.home.usecases.TagHit
import com.alexvt.home.usecases.WatchAmbientColorForMediaUseCase
import com.alexvt.home.usecases.WatchMediaListUseCase
import com.alexvt.home.usecases.WatchSettingsUseCase
import com.alexvt.home.usecases.WatchTagsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@AppScope
@Inject
class MainViewModelUseCases(
    val watchTagsUseCase: WatchTagsUseCase,
    val watchMediaListUseCase: WatchMediaListUseCase,
    val watchSettingsUseCase: WatchSettingsUseCase,
    val setBluetoothLightColorUseCase: SetBluetoothLightColorUseCase,
    val watchAmbientColorForMediaUseCase: WatchAmbientColorForMediaUseCase,
    val checkSaveEditableSettingsUseCase: CheckSaveEditableSettingsUseCase,
)

class MainViewModel(
    private val useCases: MainViewModelUseCases,
    backgroundDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val backgroundCoroutineScope = viewModelScope + backgroundDispatcher

    data class MediaTypeSelection(
        val name: String,
        val isSelected: Boolean,
    )

    fun switchMediaType(selectionText: String) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                mediaTypeSelections = uiState.bottomSheetState
                    .mediaTypeSelections.map {
                        if (it.name == selectionText) {
                            it.copy(isSelected = !it.isSelected)
                        } else {
                            it
                        }
                    }
            )
        )
    }

    data class MediaState(
        val viewableMediaItems: List<MediaItem>,
        val currentMediaItem: MediaItem,
        val ambientColor: Long,
        val isCounterVisible: Boolean,
        val isUpdatingContent: Boolean,
        val naturalCounter: Int,
        val totalCount: Int,
    )

    data class SortingSelection(
        val name: String,
        val isSelected: Boolean,
    )

    fun selectSorting(selectionText: String) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                sortingSelections = uiState.bottomSheetState
                    .sortingSelections.map {
                        it.copy(isSelected = it.name == selectionText)
                    }
            )
        )
    }

    data class AlbumSelection(
        val name: String,
        val isSelected: Boolean,
    )

    fun switchAlbum(selectionText: String) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                albumSelections = uiState.bottomSheetState
                    .albumSelections.map {
                        if (it.name == selectionText) {
                            it.copy(isSelected = !it.isSelected)
                        } else {
                            it
                        }
                    }
            )
        )
    }

    data class TagSelection(
        val name: String,
        val isHit: Boolean, // whether item's filename marked with this tag
        val isIncluded: Boolean, // all shown items must have this tag
        val isExcluded: Boolean, // none of shown items must have this tag
    )

    fun switchTag(tagText: String, isToToggleExclusion: Boolean) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                tagSelections = uiState.bottomSheetState
                    .tagSelections.map {
                        when {
                            it.name != tagText -> it
                            it.isExcluded -> it.copy(isIncluded = false, isExcluded = false)
                            isToToggleExclusion -> it.copy(isIncluded = false, isExcluded = true)
                            else -> it.copy(isIncluded = !it.isIncluded)
                        }
                    }
            )
        )
    }

    fun selectIncludeAllTags() {
        selectToggleAllTags(isIncluded = true, isExcluded = false)
    }

    fun selectExcludeAllTags() {
        selectToggleAllTags(isIncluded = false, isExcluded = true)
    }

    fun selectClearAllTags() {
        selectToggleAllTags(isIncluded = false, isExcluded = false)
    }

    private fun selectToggleAllTags(isIncluded: Boolean, isExcluded: Boolean) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                tagSelections = uiState.bottomSheetState.tagSelections.map {
                    it.copy(isIncluded = isIncluded, isExcluded = isExcluded)
                }
            )
        )
    }

    data class SlideshowIntervalSelection(
        val name: String,
        val delayMillis: Long?,
        val isSelected: Boolean,
    )

    fun selectSlideshowInterval(selectionText: String) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                slideshowIntervalSelections = uiState.bottomSheetState
                    .slideshowIntervalSelections.map {
                        it.copy(isSelected = it.name == selectionText)
                    }
            )
        )
    }

    data class BottomSheetState(
        val albumSelections: List<AlbumSelection>,
        val mediaTypeSelections: List<MediaTypeSelection>,
        val sortingSelections: List<SortingSelection>,
        val areTagsShowing: Boolean,
        val tagSelections: List<TagSelection>,
        val slideshowIntervalSelections: List<SlideshowIntervalSelection>,
        val areBluetoothOptionsShowing: Boolean, // when there are Bluetooth lights in settings
        val isAmbientColorSyncEnabled: Boolean,
        val bluetoothManualColorOptions: List<Long>,
    )

    fun toggleTagsVisibility() {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                areTagsShowing = !uiState.bottomSheetState.areTagsShowing,
                tagSelections = if (uiState.bottomSheetState.areTagsShowing) {
                    uiState.bottomSheetState.tagSelections.map { // clear tags on hide
                        it.copy(isIncluded = false, isExcluded = false)
                    }
                } else {
                    uiState.bottomSheetState.tagSelections
                }
            )
        )
    }

    fun toggleAmbientColorSync() {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(
                isAmbientColorSyncEnabled = !uiState.bottomSheetState.isAmbientColorSyncEnabled
            )
        )
    }

    fun selectBluetoothLightColor(color: Long) {
        uiState = uiState.copy(
            bottomSheetState = uiState.bottomSheetState.copy(isAmbientColorSyncEnabled = false)
        )
        useCases.setBluetoothLightColorUseCase.execute(color)
    }

    fun showNextMediaItem(isPreviousInstead: Boolean) {
        val newlyViewedMediaItemIndex = with(uiState.mediaState) {
            val currentMediaItemIndex = viewableMediaItems.indexOf(currentMediaItem)
            val indexIncrement = if (isPreviousInstead) -1 else 1
            (currentMediaItemIndex + indexIncrement)
                .coerceAtMost(viewableMediaItems.size - 1)
                .coerceAtLeast(0)
        }
        uiState = uiState.copy(
            mediaState = uiState.mediaState.copy(
                currentMediaItem = uiState.mediaState.viewableMediaItems[newlyViewedMediaItemIndex],
                naturalCounter = newlyViewedMediaItemIndex + 1,
            )
        )
    }

    fun openSettings() {
        switchSettingsVisibility(isToShow = true)
    }

    fun discardSettings() {
        switchSettingsVisibility(isToShow = false)
    }

    fun saveSettings(newSettings: EditableSettings) {
        useCases.checkSaveEditableSettingsUseCase.execute(
            editableSettings = newSettings,
            coroutineScope = backgroundCoroutineScope,
        ).let { isSaved ->
            switchSettingsVisibility(isToShow = !isSaved)
        }
    }

    private fun switchSettingsVisibility(isToShow: Boolean) {
        uiState = uiState.copy(
            isSettingsEditorShown = isToShow,
        )
    }

    data class UiState(
        val mediaState: MediaState,
        val bottomSheetState: BottomSheetState,
        val isSettingsEditorShown: Boolean,
        val editableSettings: EditableSettings,
        val theme: Theme,
        val isThemeLight: Boolean,
    )

    private fun UiState.updatedWithItems(
        newMediaItems: List<MediaItem> = mediaState.viewableMediaItems,
        ambientColor: Long? = null,
        isUpdatingContent: Boolean = false,
        newTagHits: List<TagHit>? = null,
        newSettings: Settings? = null,
    ): UiState {
        val isMediaListChanged =
            newMediaItems != mediaState.viewableMediaItems
        val currentMediaItem =
            if (isMediaListChanged) {
                newMediaItems.firstOrNull()
                    ?: MediaItem(path = "", type = MediaType.NONE) // placeholder
            } else {
                mediaState.currentMediaItem
            }
        val naturalCounter =
            if (isMediaListChanged) {
                1
            } else {
                mediaState.naturalCounter
            }
        return copy(
            mediaState = MediaState(
                newMediaItems,
                currentMediaItem,
                isCounterVisible = currentMediaItem.type !in listOf(
                    MediaType.NONE, MediaType.LOADING,
                ),
                ambientColor = ambientColor ?: mediaState.ambientColor,
                isUpdatingContent = isUpdatingContent,
                naturalCounter = naturalCounter,
                totalCount = newMediaItems.size,
            ),
            bottomSheetState = bottomSheetState.copy(
                // hits update, inclusions / exclusions remain
                tagSelections = newTagHits?.map { tagHit ->
                    TagSelection(
                        name = tagHit.name,
                        isHit = tagHit.isHit,
                        isIncluded = bottomSheetState.tagSelections.firstOrNull { existingTag ->
                            existingTag.name == tagHit.name
                        }?.isIncluded ?: false,
                        isExcluded = bottomSheetState.tagSelections.firstOrNull { existingTag ->
                            existingTag.name == tagHit.name
                        }?.isExcluded ?: false,
                    )
                } ?: bottomSheetState.tagSelections,
            ).updatedWithSettings(newSettings),
            editableSettings = editableSettings.updatedWithSettings(newSettings),
            theme = newSettings?.theme ?: theme,
            isThemeLight = (newSettings?.theme
                ?: theme).color.run { background.normal > text.normal },
        )
    }

    private fun EditableSettings.updatedWithSettings(
        newSettings: Settings?,
    ): EditableSettings = copy(
        albumPaths = newSettings?.albumViewingSettings?.folderPaths?.joinToString(separator = "\n")
            ?: albumPaths,
        tagsCsvPath = newSettings?.albumViewingSettings?.tagsCsvPath ?: tagsCsvPath,
        bluetoothLightsMacAddresses = newSettings?.bluetoothLightsSettings?.bluetoothMacAddresses
            ?.joinToString(separator = "\n") ?: bluetoothLightsMacAddresses,
    )

    private fun BottomSheetState.updatedWithSettings(newSettings: Settings?): BottomSheetState =
        copy(
            albumSelections = newSettings?.albumViewingSettings?.folderPaths
                ?.mapIndexed { index, path ->
                    AlbumSelection(
                        name = path.trimEnd('/').substringAfterLast('/'),
                        isSelected = index == 0,
                    )
                } ?: albumSelections,
            slideshowIntervalSelections = newSettings?.albumViewingSettings
                ?.autoplayDelayPresetsSeconds?.map { seconds ->
                    SlideshowIntervalSelection(
                        name = if (seconds == 0) "Off" else "$seconds s",
                        delayMillis = if (seconds > 0) seconds * 1000L else null,
                        isSelected = seconds == 0,
                    )
                } ?: slideshowIntervalSelections,
            areBluetoothOptionsShowing = newSettings?.bluetoothLightsSettings
                ?.bluetoothMacAddresses?.isNotEmpty()
                ?: areBluetoothOptionsShowing,
            bluetoothManualColorOptions = newSettings?.bluetoothLightsSettings?.colorPresets
                ?: bluetoothManualColorOptions,
        )

    private val loadingPlaceholderMediaItem: MediaItem =
        MediaItem(path = "", type = MediaType.LOADING)

    private fun getDefaultLoadingState(settings: Settings): UiState {
        return UiState(
            mediaState = MediaState(
                viewableMediaItems = emptyList(),
                currentMediaItem = loadingPlaceholderMediaItem,
                ambientColor = 0x00000000,
                isCounterVisible = false,
                isUpdatingContent = true,
                naturalCounter = 1,
                totalCount = 0,
            ),
            bottomSheetState = BottomSheetState(
                albumSelections = emptyList(), // will update from settings
                sortingSelections = SortingType.values().mapIndexed { index, sortingType ->
                    SortingSelection(
                        name = sortingType.name.toCapitalizedPhrase(),
                        isSelected = index == 0,
                    )
                },
                mediaTypeSelections = listOf(MediaType.IMAGE, MediaType.GIF, MediaType.VIDEO)
                    .mapIndexed { index, mediaType ->
                        MediaTypeSelection(
                            name = mediaType.name.toCapitalizedPhrase(),
                            isSelected = index == 0,
                        )
                    },
                areTagsShowing = false,
                tagSelections = emptyList(), // wil be loaded
                slideshowIntervalSelections = emptyList(), // will update from settings
                areBluetoothOptionsShowing = false, // will update from settings
                isAmbientColorSyncEnabled = false,
                bluetoothManualColorOptions = emptyList(),  // will update from settings
            ).updatedWithSettings(settings),
            isSettingsEditorShown = false,
            editableSettings = EditableSettings("", "", ""), // will update from settings
            theme = settings.theme,
            isThemeLight = settings.theme.color.run { background.normal > text.normal },
        )
    }

    private fun String.toCapitalizedPhrase(): String =
        lowercase().replaceFirstChar { it.uppercaseChar() }.replace('_', ' ')

    val uiStateFlow: StateFlow<UiState> =
        MutableStateFlow(
            value = getDefaultLoadingState(
                settings = useCases.watchSettingsUseCase.execute().value
            )
        )

    init {
        backgroundCoroutineScope.launch {
            useCases.watchMediaListUseCase.execute(
                mediaSelectionParamsFlow = uiStateFlow.map { it.toMediaSelectionParams() }
                    .distinctUntilChanged().onEach {
                        uiState = uiState.updatedWithItems(isUpdatingContent = true)
                    },
            ).collectLatest { mediaItems ->
                uiState = uiState.updatedWithItems(newMediaItems = mediaItems)
            }
        }
        backgroundCoroutineScope.launch {
            useCases.watchTagsUseCase.execute(
                filePathFlow = uiStateFlow.map { it.mediaState.currentMediaItem.path }
                    .distinctUntilChanged(),
            ).collectLatest { tagHits ->
                uiState = uiState.updatedWithItems(newTagHits = tagHits)
            }
        }
        backgroundCoroutineScope.launch {
            useCases.watchSettingsUseCase.execute().collectLatest { settings ->
                uiState = uiState.updatedWithItems(newSettings = settings)
            }
        }
        backgroundCoroutineScope.launch {
            useCases.watchAmbientColorForMediaUseCase.execute(
                mediaItemFlow = uiStateFlow.distinctUntilChanged { old, new ->
                    old.mediaState.currentMediaItem == new.mediaState.currentMediaItem
                            && old.isAmbientColorSyncEnabled() == new.isAmbientColorSyncEnabled()
                }.map { uiStateAfterAmbienceRelatedChange ->
                    uiStateAfterAmbienceRelatedChange.takeIf {
                        it.bottomSheetState.isAmbientColorSyncEnabled // otherwise default color
                    }?.mediaState?.currentMediaItem
                }
            ).collectLatest { ambientColor ->
                uiState = uiState.updatedWithItems(ambientColor = ambientColor)
            }
        }
        backgroundCoroutineScope.launch {
            uiStateFlow.distinctUntilChanged { old, new ->
                old.mediaState.currentMediaItem == new.mediaState.currentMediaItem
                        && old.slideshowIntervalOrNull() == new.slideshowIntervalOrNull()
            }.map { uiStateAfterSlideshowRelatedChange ->
                uiStateAfterSlideshowRelatedChange.slideshowIntervalOrNull()
            }.collectLatest { delayMillisOrNull ->
                delayMillisOrNull?.takeIf { it > 0 }?.let { delayMillis ->
                    delay(delayMillis)
                    showNextMediaItem(isPreviousInstead = false)
                }
            }
        }
    }

    private fun UiState.isAmbientColorSyncEnabled(): Boolean =
        bottomSheetState.isAmbientColorSyncEnabled

    private fun UiState.slideshowIntervalOrNull(): Long? =
        bottomSheetState.slideshowIntervalSelections.firstOrNull { it.isSelected }?.delayMillis

    private fun UiState.toMediaSelectionParams(): MediaSelectionParams =
        with(bottomSheetState) {
            MediaSelectionParams(
                albumNames = albumSelections.filter { it.isSelected }.map { it.name },
                mediaTypes = mediaTypeSelections.filter { it.isSelected }
                    .map { MediaType.valueOf(it.name.uppercase().replace(' ', '_')) },
                sortingType = sortingSelections.firstOrNull { it.isSelected }?.name?.uppercase()
                    ?.replace(' ', '_')?.run(SortingType::valueOf)
                    ?: SortingType.RANDOM,
                includedTags = tagSelections.filter { it.isIncluded }.map { it.name }.toSet(),
                excludedTags = tagSelections.filter { it.isExcluded }.map { it.name }.toSet(),
            )
        }

    private var uiState: UiState = (uiStateFlow as MutableStateFlow).value
        set(newUiState) {
            field = newUiState.apply((uiStateFlow as MutableStateFlow)::tryEmit)
        }

}
