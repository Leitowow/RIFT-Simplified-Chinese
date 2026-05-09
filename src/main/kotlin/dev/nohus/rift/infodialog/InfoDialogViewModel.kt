package dev.nohus.rift.infodialog

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.text.FormattedText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class InfoDialogViewModel(
    @InjectedParam private val inputModel: InfoDialogInputModel,
) : ViewModel() {

    data class UiState(
        val title: String,
        val text: FormattedText,
        val isWarning: Boolean,
    )

    private val _state = MutableStateFlow(
        UiState(
            title = inputModel.title,
            text = inputModel.text,
            isWarning = inputModel.isWarning,
        ),
    )
    val state = _state.asStateFlow()
}
