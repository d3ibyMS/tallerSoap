package co.edu.udenar.trmcolombia.model

/** TRM de una fecha, ya convertida a un objeto propio de la app. */
data class TrmQuote(
    val queryDate: String,
    val value: Double,
    val unit: String,
    val validityFrom: String,
    val validityTo: String,
    val fromCache: Boolean = false
)

/** Lo que se envió y recibió: sirve para mostrar el mensaje SOAP real en la clase. */
data class SoapExchange(
    val request: String,
    val httpCode: Int,
    val response: String
)

/** Resultado de leer el XML de respuesta (capa Analizador XML). */
sealed class TrmParseResult {
    data class Success(
        val value: Double,
        val unit: String,
        val validityFrom: String,
        val validityTo: String
    ) : TrmParseResult()

    /** HTTP 200, pero el indicador success vino en false: no hay TRM para esa fecha. */
    object NoData : TrmParseResult()

    /** El cuerpo trae un elemento Fault de SOAP. */
    data class Fault(val message: String) : TrmParseResult()

    /** El XML no se pudo leer o no tiene la forma esperada. */
    data class Invalid(val reason: String) : TrmParseResult()
}

/** Resultado final que entrega el repositorio al ViewModel. */
sealed class TrmOutcome {
    data class Success(val quote: TrmQuote) : TrmOutcome()
    object NoData : TrmOutcome()
    data class Fault(val message: String) : TrmOutcome()
    object Timeout : TrmOutcome()
    object NoConnection : TrmOutcome()
    data class HttpError(val code: Int) : TrmOutcome()
    object InvalidResponse : TrmOutcome()
}

data class QueryResult(val outcome: TrmOutcome, val exchange: SoapExchange?)

object TrmConverter {
    fun usdToCop(usd: Double, trm: Double): Double = usd * trm

    /** Acepta "12.5" o "12,5". Devuelve null si no es un número válido y positivo. */
    fun parseAmount(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 && it.isFinite() }
}
