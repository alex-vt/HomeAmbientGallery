package com.alexvt.home.repositories

import com.alexvt.home.App.Companion.androidAppContext

actual class DefaultFoldersRepository {

    private val storageFolderPath: String =
        androidAppContext.dataDir.absolutePath.trimEnd('/')

    actual fun get(): List<String> =
        listOf(
            "$storageFolderPath/Download",
        )

}