package com.alexvt.home.viewui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alexvt.home.usecases.EditableSettings
import com.alexvt.home.viewutils.StandardButton

@Composable
fun SettingsView(
    editableSettings: EditableSettings,
    onSaveClick: (EditableSettings) -> Unit,
    onDiscardClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            // intercepted out-click
        }.background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.7f))
    ) {
        Column(
            Modifier.widthIn(max = 800.dp).align(Alignment.Center)
                .background(MaterialTheme.colors.background).padding(8.dp)
        ) {
            var unsavedEditableSettings by remember { mutableStateOf(editableSettings) }

            SettingTextField(
                label = "Album paths to view",
                hint = "Full paths, 1 per line",
                maxLines = 100,
                height = 240.dp,
                value = editableSettings.albumPaths,
            ) { newValue ->
                unsavedEditableSettings = unsavedEditableSettings.copy(albumPaths = newValue)
            }
            Spacer(Modifier.height(8.dp))

            SettingTextField(
                label = "Tags CSV file path",
                hint = "Full path, or empty if none",
                maxLines = 1,
                height = 60.dp,
                value = editableSettings.tagsCsvPath,
            ) { newValue ->
                unsavedEditableSettings = unsavedEditableSettings.copy(tagsCsvPath = newValue)
            }
            Spacer(Modifier.height(8.dp))

            SettingTextField(
                label = "Bluetooth lights MAC addresses",
                hint = "For generic type lights, 1 per line",
                maxLines = 5,
                height = 120.dp,
                value = editableSettings.bluetoothLightsMacAddresses,
            ) { newValue ->
                unsavedEditableSettings = unsavedEditableSettings.copy(
                    bluetoothLightsMacAddresses = newValue
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                StandardButton(text = "Discard", onClick = onDiscardClick)
                Spacer(Modifier.width(8.dp))
                StandardButton(text = "Save", isAccented = true) {
                    onSaveClick(unsavedEditableSettings)
                }
            }
        }
    }
}

@Composable
fun SettingTextField(
    label: String,
    hint: String,
    maxLines: Int,
    height: Dp,
    value: String,
    onNewValue: (String) -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = MaterialTheme.colors.secondary,
            backgroundColor = MaterialTheme.colors.secondaryVariant,
        )
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth().height(height),
            maxLines = maxLines,
            value = textFieldValue,
            label = {
                Text(
                    text = label,
                    fontSize = MaterialTheme.typography.subtitle2.fontSize,
                    color = MaterialTheme.colors.onSurface,
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                cursorColor = MaterialTheme.colors.onSecondary,
                focusedIndicatorColor = MaterialTheme.colors.secondaryVariant,
                backgroundColor = MaterialTheme.colors.primary,
                textColor = MaterialTheme.colors.onBackground,
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = MaterialTheme.typography.body2.fontSize,
            ),
            placeholder = {
                Text(
                    text = hint,
                    fontSize = MaterialTheme.typography.body2.fontSize,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            },
            onValueChange = { newValue ->
                textFieldValue = newValue
                onNewValue(newValue.text)
            }
        )
    }
}