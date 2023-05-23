package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import java.text.SimpleDateFormat

data class TagMatrix(
    val allFilenames: List<String>,
    val allTags: List<String>,
    val tagOccurrenceMatrix: List<List<Boolean>>
)

@AppScope
@Inject
class FileTagsRepository(
    private val settingsRepository: SettingsRepository,
    private val csvRepository: CsvRepository,
    appBackgroundCoroutineScope: CoroutineScope,
) {
    // Combines file reads on settings change and the in memory replicas of writes.
    // Assuming no external writes.
    private val inMemoryTagMatrixFlow: MutableStateFlow<TagMatrix> =
        MutableStateFlow(readTagMatrix(settingsRepository.readSettings()))

    init {
        appBackgroundCoroutineScope.launch {
            settingsRepository.watchSettings().collect { settings ->
                inMemoryTagMatrixFlow.tryEmit(readTagMatrix(settings))
            }
        }
    }

    fun watchTagsFor(filenameWithoutExtension: String): Flow<Set<String>> =
        inMemoryTagMatrixFlow.map { tagMatrix ->
            val tagIndex = tagMatrix.allFilenames.indexOf(filenameWithoutExtension)
            val tagRow = tagMatrix.tagOccurrenceMatrix.getOrElse(tagIndex) { emptyList() }
            tagMatrix.allTags.zip(tagRow) { tag, isSet ->
                tag to isSet
            }.filter { it.second }.map { it.first }.toSet()
        }

    fun watchAllTags(): Flow<Set<String>> =
        inMemoryTagMatrixFlow.map {
            it.allTags.toSet()
        }

    fun getTagMatrix(): TagMatrix =
        inMemoryTagMatrixFlow.value

    fun writeTagHit(filenameWithoutExtension: String, tag: String, isHit: Boolean) {
        with(inMemoryTagMatrixFlow.value.withAddedFilenameIfAbsent(filenameWithoutExtension)) {
            val filenameIndex = allFilenames.indexOf(filenameWithoutExtension)
            val tagIndex = allTags.indexOf(tag)
            val updatedTagMatrix = copy(
                tagOccurrenceMatrix = tagOccurrenceMatrix.mapIndexed { rowIndex, row ->
                    row.mapIndexed { hitIndex, oldValue ->
                        if (rowIndex == filenameIndex && tagIndex == hitIndex) {
                            isHit
                        } else {
                            oldValue
                        }
                    }
                }
            ).withoutTaglessFiles()
            inMemoryTagMatrixFlow.tryEmit(updatedTagMatrix)
            writeTagMatrix(updatedTagMatrix)
        }
    }

    private fun writeTagMatrix(tagMatrix: TagMatrix) {
        csvRepository.submitWrite(
            csvFileFullPath = settingsRepository.readSettings().albumViewingSettings.tagsCsvPath
        ) {
            with(tagMatrix) {
                val headerTimestamp =
                    SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(System.currentTimeMillis())
                val headerOfFilenames = "<filenames_$headerTimestamp>"
                val headerOrTags = allTags.joinToString(separator = ",")
                val headerRow = listOf(headerOfFilenames, headerOrTags)
                val filenameToTagsRows =
                    (allFilenames zip tagOccurrenceMatrix).map { (filename, tagOccurrences) ->
                        val tagsForFilename = (tagOccurrences zip allTags)
                            .filter { (isHit, tagName) ->
                                isHit
                            }.map { (isHit, tagName) ->
                                tagName
                            }
                            .joinToString(separator = ",")
                        listOf(filename, tagsForFilename)
                    }
                listOf(headerRow) + filenameToTagsRows
            }
        }
    }

    private fun readTagMatrix(settings: Settings): TagMatrix =
        csvRepository.read(
            csvFileFullPath = settings.albumViewingSettings.tagsCsvPath
        ) { csvData ->
            val tagsFromHeader: List<String> = csvData[0][1].splitToTags()
            val filenamesToTags: List<Pair<String, List<String>>> = csvData.drop(1) // header
                .map { row ->
                    row[0] to row[1].splitToTags()
                }
                .sortedBy { it.first }
            val allTags: Set<String> =
                filenamesToTags.fold(emptySet()) { accumulatedTags, tagsForFilename ->
                    accumulatedTags + tagsForFilename.second
                }
            val allTagsSortedLikeInHeader = tagsFromHeader + (allTags - tagsFromHeader)
            TagMatrix(
                allFilenames = filenamesToTags.map { it.first },
                allTagsSortedLikeInHeader,
                tagOccurrenceMatrix = filenamesToTags.map { (filename, tagsForFilename) ->
                    allTagsSortedLikeInHeader.map { tag ->
                        tag in tagsForFilename
                    }
                }
            )
        }.getOrElse { TagMatrix(emptyList(), emptyList(), emptyList()) }

    private fun String.splitToTags(separator: String = ","): List<String> =
        split(separator).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun TagMatrix.withAddedFilenameIfAbsent(filename: String): TagMatrix {
        if (filename in allFilenames) return this
        // keeping sorted order of/by filenames
        val updatedFilenames = (allFilenames + filename).distinct().sorted()
        val newFilenameIndex = updatedFilenames.indexOf(filename)
        val newTagOccurrences = tagOccurrenceMatrix.toMutableList().apply {
            add(newFilenameIndex, allTags.map { false })
        }
        return copy(allFilenames = updatedFilenames, tagOccurrenceMatrix = newTagOccurrences)
    }

    private fun TagMatrix.withoutTaglessFiles(): TagMatrix {
        val (updatedFilenames, newTagOccurrences) =
            (allFilenames zip tagOccurrenceMatrix).filter { (filename, filenameTagOccurrences) ->
                filenameTagOccurrences.any { it }
            }.let { it.map { it.first } to it.map { it.second } }
        return copy(allFilenames = updatedFilenames, tagOccurrenceMatrix = newTagOccurrences)
    }

}
