package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

data class FileMetadata(val fullPath: String, val modificationTimestamp: Long, val size: Long)

@AppScope
@Inject
class FileAccessRepository() {

    suspend fun listFiles(vararg absolutePathParts: String): List<String> {
        val folderPath = Paths.get("", *absolutePathParts)
        return withContext(Dispatchers.IO) {
            Files.walk(folderPath)
        }.filter { it.isRegularFile() }.toList().map { it.toAbsolutePath().toString() }
    }

    // start of observation is considered a change
    fun watchOccurrenceOfChanges(vararg absolutePathParts: String): Flow<Unit> =
        flow {
            emit(Unit)
            val path = Paths.get("", *absolutePathParts)
            if (!path.exists()) {
                return@flow
            }
            val pathToWatch = if (path.isRegularFile()) path.parent else path
            val watchService = FileSystems.getDefault().newWatchService()
            val pathKey = pathToWatch.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE,
            )
            while (true) {
                for (event in pathKey.pollEvents()) {
                    val isRelevant =
                        !path.isRegularFile() || (event.context() as Path).fileName == path.fileName
                    if (isRelevant) {
                        emit(Unit)
                    }
                }
                val eventPollIntervalMillis = 100L
                try {
                    delay(eventPollIntervalMillis)
                } catch (exception: CancellationException) {
                    pathKey.cancel()
                    watchService.close()
                    break
                }
                if (!pathKey.reset()) {
                    break
                }
            }
            watchService.close()
        }.conflate().flowOn(Dispatchers.IO)

    fun readMetadata(vararg absolutePathParts: String): FileMetadata {
        return Paths.get("", *absolutePathParts).run {
            FileMetadata(
                fullPath = toString(),
                modificationTimestamp = try {
                    getLastModifiedTime().toMillis()
                } catch (t: Throwable) {
                    0L // consider quite old by default
                },
                size = try {
                    fileSize()
                } catch (t: Throwable) {
                    0L // consider quite small by default
                },
            )
        }
    }
}

