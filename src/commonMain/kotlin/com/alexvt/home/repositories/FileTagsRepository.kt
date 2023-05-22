package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import de.siegmar.fastcsv.reader.CsvReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import java.io.File
import java.nio.charset.StandardCharsets

data class TagMatrix(
    val allFilenames: List<String>,
    val allTags: List<String>,
    val tagOccurrenceMatrix: List<List<Boolean>>
)

@AppScope
@Inject
class FileTagsRepository(
    private val settingsRepository: SettingsRepository,
) {

    private fun readTagMatrix(csvPath: String, hasHeader: Boolean = true): TagMatrix {
        val csv = try {
            CsvReader().read(File(csvPath), StandardCharsets.UTF_8)
        } catch (error: Throwable) { // todo error handling
            return TagMatrix(emptyList(), emptyList(), emptyList())
        }
        val tagsFromHeader: List<String> = csv.rows.takeIf { hasHeader }?.firstOrNull()
            ?.fields?.getOrNull(1)?.splitToTags() ?: emptyList()
        val filenamesToTags: List<Pair<String, List<String>>> = csv.rows
            .drop(if (hasHeader) 1 else 0)
            .filter { it.fieldCount >= 2 }
            .map { csvRow ->
                val filename = csvRow.getField(0)
                val tagsForFilename = csvRow.getField(1).splitToTags()
                filename to tagsForFilename
            }
        val allTags = filenamesToTags.fold(emptySet<String>()) { accumulatedTags, tagsForFilename ->
            accumulatedTags + tagsForFilename.second
        }
        val allTagsSortedLikeInHeader = tagsFromHeader + (allTags - tagsFromHeader)
        return TagMatrix(
            allFilenames = filenamesToTags.map { it.first },
            allTagsSortedLikeInHeader,
            tagOccurrenceMatrix = filenamesToTags.map { (filename, tagsForFilename) ->
                allTagsSortedLikeInHeader.map { tag ->
                    tag in tagsForFilename
                }
            }
        )
    }

    private fun String.splitToTags(separator: String = ","): List<String> =
        split(separator).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private val tagMatrixFlow: Flow<TagMatrix> =
        settingsRepository.watchSettings().map { settings ->
            settings.albumViewingSettings.tagsCsvPath
        }.distinctUntilChanged().map { csvPath ->
            readTagMatrix(csvPath)
        }

    fun readTagMatrix(): TagMatrix =
        settingsRepository.readSettings().albumViewingSettings.tagsCsvPath
            .run(::readTagMatrix)

    fun watchTagsFor(filenameWithoutExtension: String): Flow<Set<String>> =
        tagMatrixFlow.map { tagMatrix ->
            val tagIndex = tagMatrix.allFilenames.indexOf(filenameWithoutExtension)
            val tagRow = tagMatrix.tagOccurrenceMatrix.getOrElse(tagIndex) { emptyList() }
            tagMatrix.allTags.zip(tagRow) { tag, isSet ->
                tag to isSet
            }.filter { it.second }.map { it.first }.toSet()
        }

    fun watchAllTags(): Flow<Set<String>> =
        tagMatrixFlow.map { it.allTags.toSet() }

}
