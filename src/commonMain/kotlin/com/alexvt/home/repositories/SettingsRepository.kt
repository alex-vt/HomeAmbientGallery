package com.alexvt.home.repositories

import com.alexvt.home.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@kotlinx.serialization.Serializable
data class TextColors(
    val normal: Long,
    val dim: Long,
    val bright: Long,
    val accentBright: Long,
    val error: Long,
)

@kotlinx.serialization.Serializable
data class BackgroundColors(
    val normal: Long,
    val bright: Long,
    val dim: Long,
    val accent: Long,
    val accentBright: Long,
)

@kotlinx.serialization.Serializable
data class ThemeColors(
    val background: BackgroundColors,
    val text: TextColors,
)

@kotlinx.serialization.Serializable
data class FontSizes(
    val small: Int,
    val normal: Int,
    val big: Int,
)

@kotlinx.serialization.Serializable
data class ThemeFonts(
    val size: FontSizes,
)

@kotlinx.serialization.Serializable
data class Theme(
    val color: ThemeColors,
    val font: ThemeFonts,
)

@kotlinx.serialization.Serializable
data class BluetoothLightsSettings(
    val bluetoothMacAddresses: List<String>,
    val colorPresets: List<Long>,
)

@kotlinx.serialization.Serializable
data class AlbumViewingSettings(
    val folderPaths: List<String>,
    val tagsCsvPath: String,
    val autoplayDelayPresetsSeconds: List<Int>, // 0 - no autoplay
)

@kotlinx.serialization.Serializable
data class Settings(
    val theme: Theme,
    val bluetoothLightsSettings: BluetoothLightsSettings,
    val albumViewingSettings: AlbumViewingSettings,
)

@AppScope
@Inject
class SettingsRepository(
    private val storageRepository: StorageRepository,
    private val defaultFoldersRepository: DefaultFoldersRepository,
) {
    private val defaultTheme = Theme(
        color = ThemeColors(
            background = BackgroundColors(
                normal = 0xFF2B2F2D,
                bright = 0xFF3C403D,
                dim = 0xFF202221,
                accent = 0xFF008866,
                accentBright = 0xFF11CCAA,
            ),
            text = TextColors(
                normal = 0xFFEEEEEE,
                dim = 0xFFBBBBBB,
                bright = 0xFFFFFFFF,
                accentBright = 0xFF55FFDD,
                error = 0xFFFF7777,
            ),
        ),
        font = ThemeFonts(
            size = FontSizes(
                small = 13,
                normal = 14,
                big = 16,
            )
        )
    )

    private val defaultSettings = Settings(
        bluetoothLightsSettings = BluetoothLightsSettings(
            bluetoothMacAddresses = listOf(),
            colorPresets = listOf(
                0xFFFFFF00,
                0xFF00FF00,
                0xFF00FFFF,
                0xFF0000FF,
                0xFF000000,
                0xFFFF0000,
                0xFFFF00FF,
                0xFFFFFFFF,
            )
        ),
        albumViewingSettings = AlbumViewingSettings(
            folderPaths = defaultFoldersRepository.get(),
            tagsCsvPath = "",
            autoplayDelayPresetsSeconds = listOf(0, 5, 10, 20, 30),
        ),
        theme = defaultTheme,
    )
    private val storageKey = "settings"
    private val json = Json { prettyPrint = true }

    private val settingsMutableFlow: MutableStateFlow<Settings> =
        MutableStateFlow(
            storageRepository.readEntry(
                key = storageKey,
                defaultValue = json.encodeToString(defaultSettings)
            ).let { jsonString ->
                try {
                    json.decodeFromString(jsonString)
                } catch (t: SerializationException) {
                    // settings migration strategy on schema change: reset to default
                    defaultSettings
                }
            }
        )

    fun watchSettings(): StateFlow<Settings> =
        settingsMutableFlow.asStateFlow()

    fun readSettings(): Settings =
        settingsMutableFlow.value

    suspend fun updateSettings(newSettings: Settings) {
        settingsMutableFlow.emit(newSettings).also {
            storageRepository.writeEntry(key = storageKey, value = json.encodeToString(newSettings))
        }
    }

}
