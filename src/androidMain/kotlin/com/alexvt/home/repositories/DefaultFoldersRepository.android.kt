package com.alexvt.home.repositories

import android.os.Environment

actual class DefaultFoldersRepository {

    private val storageFolderPath: String =
        Environment.getExternalStorageDirectory().path.trimEnd('/')

    actual fun get(): List<String> =
        listOf(
            "$storageFolderPath/Download",
        )

}