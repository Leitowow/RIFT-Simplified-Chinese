package dev.nohus.rift.infodialog

import dev.nohus.rift.compose.text.FormattedText

data class InfoDialogInputModel(
    val title: String,
    val text: FormattedText,
    val isWarning: Boolean,
)
