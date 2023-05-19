package com.alexvt.home.repositories

expect class DefaultFoldersRepository() {

    fun get(): List<String>

}