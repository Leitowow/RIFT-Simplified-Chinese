package dev.nohus.rift

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import dev.nohus.rift.crash.handleFatalException
import dev.nohus.rift.di.koin
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.util.UUID

open class ViewModel {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> handleFatalException(throwable) }
    protected val viewModelScope = CoroutineScope(Job() + exceptionHandler)

    fun <T> MutableSharedFlow<T>.scopedEmit(value: T) {
        viewModelScope.launch {
            emit(value)
        }
    }

    fun close() {
        viewModelScope.cancel()
        onClose()
    }

    open fun onClose() {}
}

@Composable
inline fun <reified VM : ViewModel> viewModel(): VM {
    val viewModel: VM = remember { koin.get() }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.close()
        }
    }
    return viewModel
}

@Composable
inline fun <reified VM : ViewModel> viewModel(windowUuid: UUID): VM {
    val viewModel: VM = remember(windowUuid) { koin.get { parametersOf(windowUuid) } }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.close()
        }
    }
    return viewModel
}

@Composable
inline fun <reified VM : ViewModel, I> viewModel(inputModel: I): VM {
    val viewModel: VM = remember(inputModel) { koin.get { parametersOf(inputModel) } }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.close()
        }
    }
    return viewModel
}
