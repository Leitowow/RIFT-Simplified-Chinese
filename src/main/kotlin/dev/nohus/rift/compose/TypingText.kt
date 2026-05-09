package dev.nohus.rift.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle

@Composable
fun TypingText(
    text: AnnotatedString,
    style: TextStyle,
    characterDuration: Int = 20,
    onFinishedTyping: () -> Unit = {},
    modifier: Modifier = Modifier.Companion,
) {
    var targetValue by remember(text) { mutableStateOf(0) }
    val animationSpec = remember(targetValue) {
        tween<Int>(durationMillis = targetValue * characterDuration, easing = LinearEasing)
    }
    val typedCharacters = key(text) { animateIntAsState(targetValue, animationSpec, finishedListener = { onFinishedTyping() }) }
    LaunchedEffect(text) {
        targetValue = text.length
    }
    Text(
        text = text.subSequence(0, typedCharacters.value),
        style = style,
        modifier = modifier,
    )
}
