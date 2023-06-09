package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.FileTagsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

data class TagHit(val name: String, val isHit: Boolean)

@AppScope
@Inject
class WatchTagsUseCase(
    private val fileTagsRepository: FileTagsRepository,
) {

    fun execute(path: String): Flow<List<TagHit>> {
        val allTagsFlow = fileTagsRepository.watchAllTags()
        val fileTagsFlow = fileTagsRepository.watchTagsFor(getFilenameWithoutExtension(path))
        return allTagsFlow.combine(fileTagsFlow) { allTags, tagsForFile ->
            allTags.map { everyTag ->
                TagHit(name = everyTag, isHit = everyTag in tagsForFile)
            }
        }
    }

    private fun getFilenameWithoutExtension(path: String): String =
        path.trimEnd('/').substringAfterLast('/').substringBeforeLast('.')

}
