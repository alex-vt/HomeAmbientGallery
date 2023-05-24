package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.FileAccessRepository
import com.alexvt.home.repositories.FileTagsRepository
import com.alexvt.home.repositories.SettingsRepository
import com.alexvt.home.repositories.TagMatrix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.yield
import me.tatarka.inject.annotations.Inject
import java.util.Random

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
    val version: Long,
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
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun execute(
        mediaSelectionParams: MediaSelectionParams,
        randomSeed: Long,
    ): Flow<List<MediaItem>> {
        return settingsRepository.watchSettings().map { settings ->
            mediaSelectionParams to settings
        }.flatMapLatest { (mediaSelectionParams, settings) ->
            val selectedAlbumPaths =
                settings.albumViewingSettings.folderPaths.filter { path ->
                    path.trimEnd('/')
                        .substringAfterLast('/') in mediaSelectionParams.albumNames
                }

            tagsRepository.watchTagMatrix().flatMapLatest { tagMatrix ->
                selectedAlbumPaths.asFlow().flatMapConcat { albumPath ->
                    fileAccessRepository.watchOccurrenceOfChanges(albumPath)
                }.map {
                    selectedAlbumPaths to tagMatrix
                }
            }

        }.transformLatest { (selectedAlbumPaths, tagMatrix) ->
            val filePathsFromSelectedAlbums = selectedAlbumPaths.map { albumPath ->
                fileAccessRepository.listFiles(albumPath)
            }.flatten()

            yield()

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

            val tagFilteredFullPaths =
                selectedFullPaths.filteredFullPathByTags(mediaSelectionParams, tagMatrix)

            val filteredMetadata = tagFilteredFullPaths.map { fullPath ->
                fileAccessRepository.readMetadata(fullPath)
            }

            yield()

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
                    version = it.modificationTimestamp,
                )
            }.takeIf { it.isNotEmpty() } ?: listOf(
                MediaItem(
                    path = "",
                    type = MediaType.NONE,
                    version = 0,
                )
            )

            yield()

            emit(value)
        }
    }

    private suspend fun List<String>.filteredFullPathByTags(
        mediaSelectionParams: MediaSelectionParams, tagMatrix: TagMatrix,
    ): List<String> {
        val areTagsUsed = with(mediaSelectionParams) {
            excludedTags.isNotEmpty() || includedTags.isNotEmpty()
        }
        if (!areTagsUsed) return this

        yield()

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
        yield()

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
            yield()

            val allowedFilenamesByTags =
                (tagMatrix.allFilenames zip (exclusionBoolMatrix zip inclusionBoolMatrix))
                    .filter { (filename, exclusionAndInclusionRows) ->
                        val (exclusionRow, inclusionRow) = exclusionAndInclusionRows
                        exclusionRow.none { it } && inclusionRow.all { it }
                    }.map { (filename, exclusionAndInclusionRows) ->
                        filename
                    }.toSet()
            yield()

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
            yield()

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
