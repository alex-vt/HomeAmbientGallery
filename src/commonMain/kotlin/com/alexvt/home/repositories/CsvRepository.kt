package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.writer.CsvWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

@AppScope
@Inject
class CsvRepository(
    appBackgroundCoroutineScope: CoroutineScope,
) {

    fun <T> read(
        csvFileFullPath: String, reader: (List<List<String>>) -> T,
    ): Result<T> {
        val csv = try {
            CsvReader().read(File(csvFileFullPath), StandardCharsets.UTF_8)
        } catch (throwable: Throwable) {
            return Result.failure(Exception("Failed to read from file $csvFileFullPath", throwable))
        }
        val resultObject = try {
            reader(csv.rows.map { it.fields })
        } catch (throwable: Throwable) {
            return Result.failure(Exception("Failed to read from CSV rows", throwable))
        }
        return Result.success(resultObject)
    }

    /**
     * CSV data write command without backpressure.
     * If there's a write under way, a new one will start after the current one completes.
     * If another write is already waiting, it will be discarded in favor of the new one.
     */
    fun submitWrite(csvFileFullPath: String, writer: () -> List<List<String>>) {
        csvWriteCommandFlow.tryEmit(CsvWriteCommand(csvFileFullPath, writer))
    }

    private data class CsvWriteCommand(
        val csvFileFullPath: String,
        val writer: () -> List<List<String>>,
    )

    private val csvWriteCommandFlow: MutableSharedFlow<CsvWriteCommand> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    init {
        appBackgroundCoroutineScope.launch {
            csvWriteCommandFlow.conflate().collect { (csvFileFullPath, writer) ->
                write(csvFileFullPath, writer).exceptionOrNull()?.run(csvWriteErrorFlow::tryEmit)
            }
        }
    }

    private fun write(
        csvFileFullPath: String, writer: () -> List<List<String>>,
    ): Result<Unit> {
        val csvData = try {
            writer().map { rowList ->
                rowList.toTypedArray()
            }
        } catch (throwable: Throwable) {
            return Result.failure(Exception("Failed to prepare CSV data", throwable))
        }
        try {
            val writeInProgressFileExtension = ".new.tmp"
            val csvPathForWriteInProgress = "$csvFileFullPath$writeInProgressFileExtension"
            CsvWriter().write(File(csvPathForWriteInProgress), StandardCharsets.UTF_8, csvData)
            Files.move(
                Path(csvPathForWriteInProgress),
                Path(csvFileFullPath),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (throwable: Throwable) {
            return Result.failure(Exception("Failed to write to file $csvFileFullPath", throwable))
        }
        return Result.success(Unit)
    }

    private val csvWriteErrorFlow: MutableSharedFlow<Throwable> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun watchWriteErrors(): SharedFlow<Throwable> =
        csvWriteErrorFlow.asSharedFlow()

}
