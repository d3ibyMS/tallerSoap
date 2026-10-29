package co.edu.udenar.trmcolombia.data

import android.content.Context
import android.content.SharedPreferences
import co.edu.udenar.trmcolombia.model.TrmQuote
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Guarda localmente las TRM consultadas (SharedPreferences) para:
 *  1) no repetir llamadas: la TRM de una fecha no cambia, y
 *  2) poder ver las últimas consultas sin conexión.
 */
class TrmCache(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("trm_cache", Context.MODE_PRIVATE)

    fun get(date: String): TrmQuote? {
        val raw = prefs.getString(KEY_QUOTE + date, null) ?: return null
        return try {
            val o = JSONObject(raw)
            TrmQuote(
                queryDate = o.getString("date"),
                value = o.getDouble("value"),
                unit = o.getString("unit"),
                validityFrom = o.getString("from"),
                validityTo = o.getString("to")
            )
        } catch (e: JSONException) {
            null
        }
    }

    fun put(quote: TrmQuote) {
        val json = JSONObject()
            .put("date", quote.queryDate)
            .put("value", quote.value)
            .put("unit", quote.unit)
            .put("from", quote.validityFrom)
            .put("to", quote.validityTo)
        val dates = (listOf(quote.queryDate) + historyDates().filter { it != quote.queryDate })
            .take(MAX_HISTORY)
        prefs.edit()
            .putString(KEY_QUOTE + quote.queryDate, json.toString())
            .putString(KEY_HISTORY, JSONArray(dates).toString())
            .apply()
    }

    fun history(): List<TrmQuote> =
        historyDates().mapNotNull { get(it) }.map { it.copy(fromCache = true) }

    private fun historyDates(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private companion object {
        const val KEY_QUOTE = "q_"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY = 10
    }
}
