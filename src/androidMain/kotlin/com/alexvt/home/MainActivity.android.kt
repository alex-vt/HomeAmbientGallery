package com.alexvt.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.alexvt.home.viewui.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.tlaster.precompose.lifecycle.Lifecycle
import moe.tlaster.precompose.lifecycle.LifecycleObserver
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent


@ExperimentalFoundationApi
class MainActivity : PreComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isShown by getIsShownStateFlow().collectAsState()
            MainView(isShown, App.dependencies, Dispatchers.Default)
        }

        if (!Environment.isExternalStorageManager()) { // todo request more gracefully
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }

    }

    private fun getIsShownStateFlow(): StateFlow<Boolean> =
        MutableStateFlow(false).apply {
            lifecycle.addObserver(object : LifecycleObserver {
                override fun onStateChanged(state: Lifecycle.State) {
                    tryEmit(state == Lifecycle.State.Active)
                }
            })
        }.asStateFlow()

}