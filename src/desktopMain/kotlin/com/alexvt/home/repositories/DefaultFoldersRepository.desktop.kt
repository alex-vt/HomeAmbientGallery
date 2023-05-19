package com.alexvt.home.repositories

actual class DefaultFoldersRepository {

    private val homeFolderPath: String =
        System.getProperty("user.home").trimEnd('/')

    actual fun get(): List<String> =
        listOf(
            "$homeFolderPath/Downloads"
        )

}