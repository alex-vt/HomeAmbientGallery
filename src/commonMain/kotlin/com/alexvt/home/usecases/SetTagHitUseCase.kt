package com.alexvt.home.usecases

import com.alexvt.home.AppScope
import com.alexvt.home.repositories.FileTagsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class SetTagHitUseCase(
    private val fileTagsRepository: FileTagsRepository,
) {

    fun execute(path: String, tag: String, isHit: Boolean) {
        fileTagsRepository.writeTagHit(
            filenameWithoutExtension = getFilenameWithoutExtension(path), tag, isHit,
        )
    }

    private fun getFilenameWithoutExtension(path: String): String =
        path.trimEnd('/').substringAfterLast('/').substringBeforeLast('.')

}
