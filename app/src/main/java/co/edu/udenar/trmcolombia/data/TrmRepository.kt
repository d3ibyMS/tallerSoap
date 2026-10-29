package co.edu.udenar.trmcolombia.data

import co.edu.udenar.trmcolombia.model.QueryResult
import co.edu.udenar.trmcolombia.model.TrmOutcome
import co.edu.udenar.trmcolombia.model.TrmParseResult
import co.edu.udenar.trmcolombia.model.TrmQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate

/**
 * Capa "Repositorio": decide si usa el dato guardado o consulta el servicio
 * y convierte la respuesta en un objeto propio (TrmQuote).
 */
class TrmRepository(
    private val soapClient: SoapClient,
    private val cache: TrmCache
) {

    suspend fun getTrm(date: LocalDate): QueryResult = withContext(Dispatchers.IO) {
        val key = date.toString() // ISO: aaaa-mm-dd

        // 1) ¿Ya tenemos el valor de ese día?
        cache.get(key)?.let {
            return@withContext QueryResult(TrmOutcome.Success(it.copy(fromCache = true)), null)
        }

        // 2) Consultar el servicio SOAP
        val exchange = try {
            soapClient.queryTrm(key)
        } catch (e: SocketTimeoutException) {
            return@withContext QueryResult(TrmOutcome.Timeout, null)
        } catch (e: IOException) {
            return@withContext QueryResult(TrmOutcome.NoConnection, null)
        }

        // 3) Interpretar la respuesta (código HTTP Y contenido del XML)
        val outcome = when (val parsed = TrmXmlParser.parse(exchange.response)) {
            is TrmParseResult.Success -> {
                val quote = TrmQuote(
                    queryDate = key,
                    value = parsed.value,
                    unit = parsed.unit,
                    validityFrom = parsed.validityFrom,
                    validityTo = parsed.validityTo
                )
                cache.put(quote)
                TrmOutcome.Success(quote)
            }
            is TrmParseResult.NoData -> TrmOutcome.NoData
            is TrmParseResult.Fault -> TrmOutcome.Fault(parsed.message)
            is TrmParseResult.Invalid ->
                if (exchange.httpCode !in 200..299) TrmOutcome.HttpError(exchange.httpCode)
                else TrmOutcome.InvalidResponse
        }
        QueryResult(outcome, exchange)
    }

    fun history(): List<TrmQuote> = cache.history()
}
