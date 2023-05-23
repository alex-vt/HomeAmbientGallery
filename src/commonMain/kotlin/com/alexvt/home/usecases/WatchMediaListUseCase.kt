package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.FileAccessRepository
import com.alexvt.home.repositories.FileTagsRepository
import com.alexvt.home.repositories.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import java.util.Random
import kotlin.coroutines.CoroutineContext

enum class MediaType {
    IMAGE, GIF, VIDEO, NONE, LOADING,
}

enum class SortingType {
    RANDOM,
    DATE_NEW_TO_OLD,
    DATE_OLD_TO_NEW,
    NAME_A_TO_Z,
    NAME_Z_TO_A,
    SIZE_SMALL_TO_BIG,
    SIZE_BIG_TO_SMALL,
}

data class MediaItem(
    val path: String,
    val type: MediaType,
)

data class MediaSelectionParams(
    val albumNames: List<String>,
    val mediaTypes: List<MediaType>,
    val sortingType: SortingType,
    val includedTags: Set<String>, // if none, consider all except excluded
    val excludedTags: Set<String>,
)

@AppScope
@Inject
class WatchMediaListUseCase(
    private val settingsRepository: SettingsRepository,
    private val fileAccessRepository: FileAccessRepository,
    private val tagsRepository: FileTagsRepository,
) {

    /**
     * Shown media list is defined by user selections, and what's in settings.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute(
        mediaSelectionParams: MediaSelectionParams,
        randomSeed: Long,
    ): Flow<List<MediaItem>> {
        return settingsRepository.watchSettings().map { settings ->
            mediaSelectionParams to settings
        }.conflate().transformLatest { (mediaSelectionParams, settings) ->
            coroutineScope {
                // this will be cancelled and re-launched on next pair of params and settings
                launch {
                    val selectedAlbumPaths =
                        settings.albumViewingSettings.folderPaths.filter { path ->
                            path.trimEnd('/')
                                .substringAfterLast('/') in mediaSelectionParams.albumNames
                        }

                    coroutineContext.ensureActive()

                    val filePathsFromSelectedAlbums = selectedAlbumPaths.map { albumPath ->
                        fileAccessRepository.listFiles(albumPath)
                    }.flatten()

                    coroutineContext.ensureActive()

                    val isHiddenAllowed = false
                    val selectedFullPaths = filePathsFromSelectedAlbums.filter { path ->
                        if (isHiddenAllowed) {
                            true
                        } else {
                            var pathInAlbum = path
                            selectedAlbumPaths.forEach {
                                pathInAlbum = pathInAlbum.removePrefix(it)
                            }
                            !pathInAlbum.contains("/.")
                        }
                    }.filter { path ->
                        getMediaTypeForExtension(path) in mediaSelectionParams.mediaTypes
                    }

                    val tagFilteredFullPaths = selectedFullPaths.filteredFullPathByTags(
                        mediaSelectionParams, coroutineContext,
                    )

                    val filteredMetadata = tagFilteredFullPaths.map { fullPath ->
                        fileAccessRepository.readMetadata(fullPath)
                    }

                    coroutineContext.ensureActive()

                    val sortedMetadata = with(filteredMetadata) {
                        when (mediaSelectionParams.sortingType) {
                            SortingType.RANDOM -> shuffled(Random(randomSeed))
                            SortingType.DATE_NEW_TO_OLD -> sortedByDescending {
                                it.modificationTimestamp
                            }

                            SortingType.DATE_OLD_TO_NEW -> sortedBy {
                                it.modificationTimestamp
                            }

                            SortingType.NAME_A_TO_Z -> sortedBy {
                                getFilenameWithoutExtension(it.fullPath)
                            }

                            SortingType.NAME_Z_TO_A -> sortedByDescending {
                                getFilenameWithoutExtension(it.fullPath)
                            }

                            SortingType.SIZE_SMALL_TO_BIG -> sortedBy { it.size }
                            SortingType.SIZE_BIG_TO_SMALL -> sortedByDescending { it.size }
                        }
                    }

                    val value = sortedMetadata.map {
                        MediaItem(
                            path = it.fullPath,
                            type = getMediaTypeForExtension(it.fullPath),
                        )
                    }.takeIf { it.isNotEmpty() } ?: listOf(
                        MediaItem(
                            path = "",
                            type = MediaType.NONE
                        )
                    )

                    coroutineContext.ensureActive()

                    emit(value)
                }
            }
        }
    }

    private fun List<String>.filteredFullPathByTags(
        mediaSelectionParams: MediaSelectionParams,
        coroutineContext: CoroutineContext,
    ): List<String> {
        val areTagsUsed = with(mediaSelectionParams) {
            excludedTags.isNotEmpty() || includedTags.isNotEmpty()
        }
        if (!areTagsUsed) return this

        coroutineContext.ensureActive()
        val tagMatrix = tagsRepository.getTagMatrix()

        val rowExclusionMask = tagMatrix.allTags.map { tag ->
            tag in mediaSelectionParams.excludedTags
        }
        val exclusionBoolMatrix =
            tagMatrix.tagOccurrenceMatrix.map { row ->
                (row zip rowExclusionMask).filter { (cellValue, maskValue) ->
                    maskValue
                }.map { (cellValue, maskValue) ->
                    cellValue
                }
            } // retained columns for only excluded tags
        coroutineContext.ensureActive()

        val areIncludedTagsUsed = mediaSelectionParams.includedTags.isNotEmpty()

        if (areIncludedTagsUsed) {
            val rowInclusionMask = tagMatrix.allTags.map { tag ->
                tag in mediaSelectionParams.includedTags
            }
            val inclusionBoolMatrix =
                tagMatrix.tagOccurrenceMatrix.map { row ->
                    (row zip rowInclusionMask).filter { (cellValue, maskValue) ->
                        maskValue
                    }.map { (cellValue, maskValue) ->
                        cellValue
                    }
                } // retained columns for only included tags
            coroutineContext.ensureActive()

            val allowedFilenamesByTags =
                (tagMatrix.allFilenames zip (exclusionBoolMatrix zip inclusionBoolMatrix))
                    .filter { (filename, exclusionAndInclusionRows) ->
                        val (exclusionRow, inclusionRow) = exclusionAndInclusionRows
                        exclusionRow.none { it } && inclusionRow.all { it }
                    }.map { (filename, exclusionAndInclusionRows) ->
                        filename
                    }.toSet()
            coroutineContext.ensureActive()

            return filter { fullPath ->
                getFilenameWithoutExtension(fullPath) in allowedFilenamesByTags
            }
        } else {
            val disallowedFilenamesByTags =
                (tagMatrix.allFilenames zip exclusionBoolMatrix)
                    .filter { (filename, exclusionRow) ->
                        exclusionRow.any { it }
                    }.map { (filename, exclusionAndInclusionRows) ->
                        filename
                    }.toSet()
            coroutineContext.ensureActive()

            return filterNot { fullPath ->
                getFilenameWithoutExtension(fullPath) in disallowedFilenamesByTags
            }
        }
    }

    private fun getFilenameWithoutExtension(path: String): String =
        path.trimEnd('/').substringAfterLast('/').substringBeforeLast('.')

    private fun getMediaTypeForExtension(path: String): MediaType =
        when (path.lowercase().substringAfterLast('.')) {
            "jpg", "jpeg", "png", "webp" -> MediaType.IMAGE
            "gif" -> MediaType.GIF
            "mp4", "avi", "mkv", "webm", "gifv" -> MediaType.VIDEO
            else -> MediaType.NONE
        }

}
