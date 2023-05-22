package com.alexvt.home.repositories

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alexvt.home.App.Companion.androidAppContext
import com.alexvt.home.repositories.GenericBluetoothLightRepository.BluetoothPermissionHandlingActivity.Companion.permissionOutcomeOrNull
import com.polidea.rxandroidble2.RxBleClient
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class GenericBluetoothLightRepository {

    private var currentCommandDisposable: Disposable? = null

    actual suspend fun setColor(macAddress: String, red: UByte, green: UByte, blue: UByte) {
        if (!checkOrGetBluetoothPermission()) return

        val characteristicUuidString = "0000ffd9-0000-1000-8000-00805f9b34fb"
        val data = byteArrayOf(
            0x56.toByte(),
            red.toByte(),
            green.toByte(),
            blue.toByte(),
            0x00.toByte(), 0xF0.toByte(), 0xAA.toByte()
        )

        val bluetoothOperationTimeoutMillis = 2000L
        suspendCoroutine { continuation ->
            currentCommandDisposable?.dispose()
            currentCommandDisposable = RxBleClient.create(androidAppContext)
                .getBleDevice(macAddress)
                .establishConnection(false)
                .flatMapSingle { connection ->
                    connection.writeCharacteristic(
                        UUID.fromString(characteristicUuidString),
                        data
                    )
                }
                .take(1)
                .timeout(bluetoothOperationTimeoutMillis, TimeUnit.MILLISECONDS)
                .subscribe({
                    continuation.resume(true)
                }, { throwable ->
                    Log.e("HomeAmbientGallery", "Failed to set color", throwable)
                    continuation.resume(false)
                })
        }
    }

    private suspend fun checkOrGetBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true // see https://github.com/dariuszseweryn/RxAndroidBle#connecting
        }
        // an already granted permission won't trigger re-requesting
        if (
            ContextCompat.checkSelfPermission(
                androidAppContext, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        // at this point permission is not granted, an overlay activity requests it
        androidAppContext.startActivity(
            Intent(androidAppContext, BluetoothPermissionHandlingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        while (true) {
            delay(100) // not granted or denied yet...
            permissionOutcomeOrNull?.let { return it }
        }
    }

    class BluetoothPermissionHandlingActivity : Activity() {

        companion object {
            var permissionOutcomeOrNull: Boolean? = null
            private const val REQUEST_CODE = 1
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            permissionOutcomeOrNull = null

            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE
            )
        }

        override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray
        ) {
            if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                permissionOutcomeOrNull = true
            } else {
                permissionOutcomeOrNull = false
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
            finish()
        }
    }

}