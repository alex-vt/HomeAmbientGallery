package com.alexvt.home.viewutils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StandardButton(
    text: String,
    isAccented: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            backgroundColor = with(MaterialTheme.colors) {
                if (isAccented) secondaryVariant else primary
            }
        ),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Text(
            text,
            color = with(MaterialTheme.colors) {
                if (isAccented) onSecondary else onBackground
            },
        )
    }
}