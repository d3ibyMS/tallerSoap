package co.edu.udenar.trmcolombia.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.edu.udenar.trmcolombia.R
import co.edu.udenar.trmcolombia.data.SoapClient
import co.edu.udenar.trmcolombia.data.TrmCache
import co.edu.udenar.trmcolombia.data.TrmRepository
import co.edu.udenar.trmcolombia.model.SoapExchange
import co.edu.udenar.trmcolombia.model.TrmOutcome
import co.edu.udenar.trmcolombia.model.TrmQuote
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Capa "ViewModel": recibe la fecha, lanza la consulta en una corrutina
 * y expone el estado (cargando, éxito o error). Sobrevive a rotaciones.
 */
class TrmViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TrmRepository(SoapClient(), TrmCache(app))

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _history = MutableStateFlow(repository.history())
    val history: StateFlow<List<TrmQuote>> = _history.asStateFlow()

    private val _soapLog = MutableStateFlow("")
    val soapLog: StateFlow<String> = _soapLog.asStateFlow()

    private var job: Job? = null

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun query() {
        val date = _selectedDate.value

        // Validar antes de armar ningún XML
        if (date.isAfter(LocalDate.now())) {
            _uiState.value = UiState.Error(R.string.error_future_date)
            return
        }

        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getTrm(date)

            result.exchange?.let { _soapLog.value = formatLog(it) }

            _uiState.value = when (val o = result.outcome) {
                is TrmOutcome.Success -> UiState.Success(o.quote)
                is TrmOutcome.NoData -> UiState.Error(R.string.error_no_data)
                is TrmOutcome.Fault -> UiState.Error(R.string.error_fault, o.message)
                is TrmOutcome.Timeout -> UiState.Error(R.string.error_timeout)
                is TrmOutcome.NoConnection -> UiState.Error(R.string.error_no_connection)
                is TrmOutcome.HttpError -> UiState.Error(R.string.error_http, "HTTP ${o.code}")
                is TrmOutcome.InvalidResponse -> UiState.Error(R.string.error_invalid)
            }
            _history.value = repository.history()
        }
    }

    private fun formatLog(e: SoapExchange): String = buildString {
        appendLine("=== SOLICITUD ===")
        appendLine("POST ${SoapClient.ENDPOINT}")
        appendLine("Content-Type: ${SoapClient.CONTENT_TYPE}")
        appendLine("SOAPAction: \"${SoapClient.SOAP_ACTION}\"")
        appendLine()
        appendLine(e.request)
        appendLine()
        appendLine("=== RESPUESTA (HTTP ${e.httpCode}) ===")
        append(e.response.take(3000))
    }
}
