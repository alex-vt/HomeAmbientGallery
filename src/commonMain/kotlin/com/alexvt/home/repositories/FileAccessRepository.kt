package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import me.tatarka.inject.annotations.Inject
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile

data class FileMetadata(val fullPath: String, val modificationTimestamp: Long, val size: Long)

@AppScope
@Inject
class FileAccessRepository() {

    fun listFiles(vararg absolutePathParts: String): List<String> {
        val folderPath = Paths.get("", *absolutePathParts)
        return Files.walk(folderPath)
            .filter { it.isRegularFile() }
            .map { it.toAbsolutePath().toString() }
            .toList()
    }

    fun isPresent(vararg absolutePathParts: String): Boolean =
        Files.exists(Paths.get("", *absolutePathParts))

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

    fun readFile(vararg absolutePathParts: String): ByteArray {
        return Files.readAllBytes(Paths.get("", *absolutePathParts))
    }

    fun writeFile(content: ByteArray, vararg absolutePathParts: String) {
        val fileAbsolutePath = Paths.get("", *absolutePathParts)
        Files.createDirectories(fileAbsolutePath.parent)
        Files.write(fileAbsolutePath, content)
        Result.success(Unit)
    }

    fun delete(vararg absolutePathParts: String) {
        Files.walk(Paths.get("", *absolutePathParts))
            .sorted(Comparator.reverseOrder())
            .forEach { path -> Files.delete(path) }
            .let { Result.success(Unit) }
    }

}
