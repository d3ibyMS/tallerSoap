package co.edu.udenar.trmcolombia.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import co.edu.udenar.trmcolombia.R
import co.edu.udenar.trmcolombia.databinding.ActivityMainBinding
import co.edu.udenar.trmcolombia.model.TrmConverter
import co.edu.udenar.trmcolombia.model.TrmQuote
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * Capa "Interfaz de usuario": muestra la fecha, el resultado y los mensajes de error.
 * No conoce nada de SOAP: solo observa el estado del ViewModel.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TrmViewModel by viewModels()

    private val copFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"))
        .apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

    /** Última TRM mostrada; la usa el conversor. */
    private var currentTrm: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnQuery.setOnClickListener { viewModel.query() }
        binding.etUsd.doAfterTextChanged { updateConversion() }
        binding.btnToggleSoap.setOnClickListener { toggleSoapLog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.selectedDate.collect { binding.etDate.setText(it.toString()) } }
                launch { viewModel.history.collect { renderHistory(it) } }
                launch {
                    viewModel.soapLog.collect {
                        if (it.isNotEmpty()) binding.tvSoapLog.text = it
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val selected = viewModel.selectedDate.value
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // no permite fechas futuras
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.date_picker_title)
            .setSelection(selected)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            viewModel.onDateSelected(date)
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun render(state: UiState) {
        binding.progress.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
        binding.btnQuery.isEnabled = state !is UiState.Loading

        when (state) {
            is UiState.Idle -> {
                binding.cardResult.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
            is UiState.Loading -> {
                binding.cardResult.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
            is UiState.Success -> {
                val q = state.quote
                currentTrm = q.value
                binding.tvError.visibility = View.GONE
                binding.cardResult.visibility = View.VISIBLE
                binding.tvTrmValue.text = "$ ${copFormat.format(q.value)} ${q.unit}"
                binding.tvTrmDetail.text = getString(
                    R.string.result_detail,
                    q.validityFrom.take(10),
                    q.validityTo.take(10),
                    q.queryDate,
                    getString(if (q.fromCache) R.string.source_cache else R.string.source_service)
                )
                updateConversion()
            }
            is UiState.Error -> {
                binding.cardResult.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = buildString {
                    append(getString(state.messageRes))
                    state.detail?.let { append("\n").append(it) }
                }
            }
        }
    }

    private fun updateConversion() {
        val trm = currentTrm
        val usd = TrmConverter.parseAmount(binding.etUsd.text?.toString().orEmpty())
        binding.tvConversion.text = when {
            trm == null -> getString(R.string.conversion_need_trm)
            usd == null -> ""
            else -> getString(
                R.string.conversion_result,
                copFormat.format(usd),
                copFormat.format(TrmConverter.usdToCop(usd, trm))
            )
        }
    }

    private fun renderHistory(items: List<TrmQuote>) {
        val container = binding.historyContainer
        container.removeAllViews()
        if (items.isEmpty()) {
            container.addView(historyRow(getString(R.string.history_empty)))
            return
        }
        items.forEach { q ->
            container.addView(historyRow("${q.queryDate}   →   $ ${copFormat.format(q.value)}"))
        }
    }

    private fun historyRow(text: String) = TextView(this).apply {
        this.text = text
        setPadding(0, 8, 0, 8)
    }

    private fun toggleSoapLog() {
        val show = binding.tvSoapLog.visibility != View.VISIBLE
        binding.tvSoapLog.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnToggleSoap.setText(if (show) R.string.soap_hide else R.string.soap_show)
    }
}
