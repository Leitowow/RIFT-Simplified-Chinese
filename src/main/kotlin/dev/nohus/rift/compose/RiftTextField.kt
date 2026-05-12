package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiftTextField(
    text: String,
    icon: DrawableResource? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    onTextChanged: (String) -> Unit,
    height: Dp = 32.dp,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start,
    selectAllOnFocus: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text))
    }
    if (textFieldValue.text != text) {
        textFieldValue = textFieldValue.copy(
            text = text,
            selection = TextRange(text.length),
        )
    }
    var wasFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onTextChanged(it.text)
        },
        textStyle = RiftTheme.typography.bodyPrimary.copy(textAlign = textAlign),
        cursorBrush = SolidColor(RiftTheme.colors.borderPrimaryLight),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = if (isPassword) passwordVisualTransformation else VisualTransformation.None,
        decorationBox = { innerTextField ->
            val contentAlignment = if (singleLine && textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart
            val centerBiasPadding = if (singleLine && textAlign == TextAlign.Center) {
                Modifier.padding(start = 1.dp)
            } else {
                Modifier
            }
            Row(
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                modifier = Modifier
                    .background(RiftTheme.colors.windowBackground.copy(alpha = 0.5f))
                    .border(1.dp, RiftTheme.colors.borderGrey)
                    .then(if (singleLine) Modifier.height(height) else Modifier.heightIn(min = height))
                    .padding(horizontal = 7.dp),
            ) {
                if (icon != null) {
                    val painter = painterResource(icon)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(RiftTheme.colors.textSecondary),
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .padding(end = 7.dp)
                            .size(16.dp),
                    )
                }
                Box(
                    contentAlignment = contentAlignment,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(centerBiasPadding),
                    ) {
                        innerTextField()
                    }
                    if (text.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            textAlign = textAlign,
                            style = RiftTheme.typography.bodySecondary.copy(textAlign = textAlign),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(centerBiasPadding),
                        )
                    }
                }
                if (onDeleteClick != null && text.isNotEmpty()) {
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = onDeleteClick,
                    )
                }
            }
        },
        modifier = modifier.onKeyEvent {
            when (it.key) {
                Key.Escape -> {
                    focusManager.clearFocus()
                    true
                }
                else -> false
            }
        }.onFocusChanged { focusState ->
            if (selectAllOnFocus && focusState.isFocused && !wasFocused) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, textFieldValue.text.length),
                )
            }
            wasFocused = focusState.isFocused
        },
    )
}

val passwordVisualTransformation = VisualTransformation {
    TransformedText(
        AnnotatedString("*".repeat(it.text.length)),
        OffsetMapping.Identity,
    )
}
