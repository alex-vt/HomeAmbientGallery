package com.alexvt.home

import com.alexvt.home.repositories.DefaultFoldersRepository
import com.alexvt.home.repositories.FileAccessRepository
import com.alexvt.home.repositories.StorageRepository
import com.alexvt.home.repositories.GenericBluetoothLightRepository
import com.alexvt.home.viewmodels.MainViewModelUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

@AppScope
@Component
abstract class AppDependencies {

    abstract val mainViewModelUseCases: MainViewModelUseCases

    @OptIn(DelicateCoroutinesApi::class)
    @AppScope
    @Provides
    protected fun appBackgroundCoroutineScope(): CoroutineScope =
        GlobalScope + Dispatchers.Default

    @AppScope
    @Provides
    protected fun storageRepository(): StorageRepository =
        StorageRepository()

    @AppScope
    @Provides
    protected fun defaultFoldersRepository(): DefaultFoldersRepository =
        DefaultFoldersRepository()

    @AppScope
    @Provides
    protected fun filesAccessRepository(): FileAccessRepository =
        FileAccessRepository()

    @AppScope
    @Provides
    protected fun genericBluetoothLightRepository(): GenericBluetoothLightRepository =
        GenericBluetoothLightRepository()

}
