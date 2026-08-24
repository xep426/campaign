package io.github.xep426.campaign.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.xep426.campaign.ui.theme.AppColors

/**
 * A task field: text on a bare line, with no box around it.
 *
 * Bare because three outlined text fields stacked up look like a form to
 * fill in, and the end-of-day screen is asking a question. The only
 * chrome is the caret, which is ember — the single moving thing on the
 * screen, exactly where the user's attention should be.
 */
@Composable
fun SlotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()

    // Reported upward so the slot numeral beside it can light — the field
    // itself stays undecorated.
    androidx.compose.runtime.LaunchedEffect(focused) { onFocusChanged(focused) }

    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.merge(LocalTextStyle.current)
                .copy(color = AppColors.paper),
            singleLine = true,
            cursorBrush = SolidColor(AppColors.ember),
            interactionSource = interactions,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
            ),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.muted.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** The hairline under a field, ember while it has focus. */
@Composable
fun FieldUnderline(focused: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (focused) AppColors.ember.copy(alpha = 0.55f) else AppColors.line)
    )
}
