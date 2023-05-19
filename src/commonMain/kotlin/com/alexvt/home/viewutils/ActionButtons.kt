package com.alexvt.home.viewutils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

data class ActionButton(
    val text: String,
    val icon: ImageVector,
    val isAlwaysShown: Boolean,
    val isSelected: Boolean = false,
)

@Composable
fun ActionButtons(
    actionButtons: List<ActionButton>,
    onClick: (String) -> Unit,
) {
    var availableWidth by remember { mutableStateOf(0) }
    Box(
        Modifier.onGloballyPositioned { layoutCoordinates ->
            availableWidth = layoutCoordinates.parentCoordinates?.size?.width ?: 0
        }
    ) {
        // fitting preferably all buttons and labels, gracefully degrading to less (if no space)
        val alwaysShownButtons = actionButtons.filter { actionButton ->
            actionButton.isAlwaysShown
        }
        PlaceFirstThatFits(
            fittingCondition = { placeable: Placeable ->
                placeable.width <= availableWidth
            },
            {
                ActionButtons(
                    actionButtons,
                    isLabeledButtons = true,
                    onClick,
                )
            },
            {
                ActionButtons(
                    alwaysShownButtons,
                    isLabeledButtons = true,
                    onClick
                )
            },
            {
                ActionButtons(
                    actionButtons,
                    isLabeledButtons = false,
                    onClick
                )
            },
            {
                ActionButtons(
                    alwaysShownButtons,
                    isLabeledButtons = false,
                    onClick
                )
            },
        )
    }
}

/**
 * Passed views are measured. The first that fits, is placed.
 * If no views fit, the last one is placed.
 */
@Composable
private fun PlaceFirstThatFits(
    fittingCondition: (Placeable) -> Boolean,
    vararg candidateViews: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        with(candidateViews) {
            dropWhile { candidateView ->
                val placeable = subcompose("$candidateView", candidateView)[0]
                    .measure(Constraints()) // unconstrained
                !fittingCondition(placeable)
            }.firstOrNull() ?: lastOrNull()
        }?.let { lastView ->
            val contentPlaceable = subcompose("placing_$lastView") { lastView() }[0]
                .measure(constraints)
            contentPlaceable.run {
                layout(width, height) {
                    place(0, 0)
                }
            }
        } ?: layout(0, 0) {}
    }
}

@Composable
private fun ActionButtons(
    actionButtons: List<ActionButton>,
    isLabeledButtons: Boolean,
    onClick: (String) -> Unit,
) {
    Row {
        actionButtons.forEachIndexed { index, item ->
            SingleActionButton(
                text = if (isLabeledButtons) item.text else null,
                icon = item.icon,
                isSelected = item.isSelected,
                offsetX = (-1 * index).dp,
                isFirst = index == 0,
                isLast = index == actionButtons.size - 1,
                onClick = { onClick(item.text) },
            )
        }
    }
}

@Composable
fun SingleActionButton(
    text: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colors.onSurface,
    isSelected: Boolean,
    isNegativelySelected: Boolean = false,
    isAccentuated: Boolean = false,
    startPadding: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onClick: () -> Unit,
    onLongOrRightClick: (() -> Unit)? = null,
) {
    val cornerRadius = 16.dp
    OutlinedButton(
        modifier = Modifier
            .offset(offsetX, 0.dp)
            .padding(start = startPadding)
            .widthIn(min = 50.dp)
            .height(32.dp)
            .zIndex(if (isSelected) 1f else 0f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (onLongOrRightClick != null) {
                            onLongOrRightClick()
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                        if (event.buttons.isSecondaryPressed) {
                            if (onLongOrRightClick != null) {
                                onLongOrRightClick()
                            }
                        }
                    }
                }
            },
        onClick = onClick,
        shape = RoundedCornerShape(
            topStart = if (isFirst) cornerRadius else 0.dp,
            topEnd = if (isLast) cornerRadius else 0.dp,
            bottomStart = if (isFirst) cornerRadius else 0.dp,
            bottomEnd = if (isLast) cornerRadius else 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = with(MaterialTheme.colors) {
                when {
                    isNegativelySelected -> onError
                    isAccentuated && isSelected -> secondary
                    isSelected -> onSurface
                    else -> primary
                }
            },
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = with(MaterialTheme.colors) {
                if (isSelected) primary else primaryVariant
            },
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                tint = if (isSelected) {
                    iconColor
                } else {
                    iconColor.copy(alpha = 0.6f)
                },
                contentDescription = text ?: "",
                modifier = Modifier
                    .padding(
                        // outer buttons outer rounded edges need bigger padding
                        start = if (isFirst) 12.dp else 10.dp,
                        end = when {
                            text != null -> 4.dp
                            isLast -> 12.dp
                            else -> 10.dp
                        },
                        top = 4.dp, bottom = 4.dp
                    )
                    .size(20.dp)
            )
        }
        if (text != null) {
            Text(
                text = text,
                color = with(MaterialTheme.colors) {
                    when {
                        isNegativelySelected -> onError
                        isAccentuated && isSelected -> onPrimary
                        isAccentuated -> secondary
                        isSelected -> onBackground
                        else -> onSurface
                    }
                },
                fontSize = MaterialTheme.typography.body2.fontSize,
                modifier = Modifier.padding(
                    start = if (icon != null) 0.dp else 12.dp,
                    end = 12.dp,
                    top = 2.dp,
                    bottom = 2.dp
                )
            )
        }
    }
}