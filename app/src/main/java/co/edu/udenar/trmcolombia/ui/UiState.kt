package co.edu.udenar.trmcolombia.ui

import androidx.annotation.StringRes
import co.edu.udenar.trmcolombia.model.TrmQuote

/** Estado que expone el ViewModel: cargando, éxito o error. */
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val quote: TrmQuote) : UiState()
    data class Error(@StringRes val messageRes: Int, val detail: String? = null) : UiState()
}
