package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import me.tatarka.inject.annotations.Inject
import java.text.SimpleDateFormat

data class TagMatrix(
    val allFilenames: List<String>,
    val allTags: List<String>,
    val tagOccurrenceMatrix: List<List<Boolean>>,
    val timestamp: Long?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@AppScope
@Inject
class FileTagsRepository(
    private val settingsRepository: SettingsRepository,
    private val fileAccessRepository: FileAccessRepository,
    private val csvRepository: CsvRepository,
    appBackgroundCoroutineScope: CoroutineScope,
) {
    private val defaultEmptyTagMatrix = TagMatrix(emptyList(), emptyList(), emptyList(), null)

    // Combines file reads on settings change and the in memory replicas of writes.
    private val inMemoryTagMatrixFlow: MutableStateFlow<TagMatrix> =
        MutableStateFlow(defaultEmptyTagMatrix)

    // When there are subscribers, watch tags CSV file for updates
    init {
        appBackgroundCoroutineScope.launch {
            inMemoryTagMatrixFlow.subscriptionCount
                .flatMapLatest { subscriptionCount ->
                    if (subscriptionCount > 0) {
                        settingsRepository.watchSettings()
                            .map { settings ->
                                settings.albumViewingSettings.tagsCsvPath
                            }.flatMapLatest { csvPath ->
                                fileAccessRepository.watchOccurrenceOfChanges(csvPath)
                                    .map { csvPath }
                            }
                    } else {
                        flowOf()
                    }
                }
                .conflate()
                .flatMapLatest { csvPath ->
                    readTagMatrix(csvPath)
                }
                .distinctUntilChanged()
                .collect { tagMatrix ->
                    val isNotRewritingWithOlder = with(inMemoryTagMatrixFlow.value.timestamp) {
                        this == null || tagMatrix.timestamp == null || tagMatrix.timestamp > this
                    }
                    if (isNotRewritingWithOlder) {
                        inMemoryTagMatrixFlow.tryEmit(tagMatrix)
                    }
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

    fun watchTagMatrix(): StateFlow<TagMatrix> =
        inMemoryTagMatrixFlow.asStateFlow()

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
                val headerOfFilenames = csvHeaderTimestampFormat.format(System.currentTimeMillis())
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

    private val csvHeaderTimestampFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")

    private suspend fun readTagMatrix(
        csvFileFullPath: String,
        maxAttempts: Int = 3,
        attemptDelayMillis: Long = 100L,
    ): Flow<TagMatrix> =
        flow {
            val tagMatrix = (1..maxAttempts)
                .asFlow()
                .map { readTagMatrixFromFile(csvFileFullPath) }
                .onEach { if (it.isFailure) delay(attemptDelayMillis) }
                .filter { it.isSuccess }
                .firstOrNull()?.getOrNull() ?: defaultEmptyTagMatrix
            yield()
            emit(tagMatrix)
        }

    private fun readTagMatrixFromFile(csvFileFullPath: String): Result<TagMatrix> =
        csvRepository.read(csvFileFullPath) { csvData ->
            val timestampFromHeader: Long? =
                try {
                    csvHeaderTimestampFormat.parse(csvData[0][0]).time
                } catch (throwable: Throwable) {
                    null // timestamp is optional
                }
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
                },
                timestamp = timestampFromHeader,
            )
        }

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
