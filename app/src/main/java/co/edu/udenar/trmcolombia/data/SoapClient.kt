package co.edu.udenar.trmcolombia.data

import co.edu.udenar.trmcolombia.model.SoapExchange
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Capa "Cliente SOAP": arma el sobre XML, lo envía por POST con OkHttp
 * y devuelve el XML de respuesta SIN interpretarlo.
 *
 * Los valores de la sección CONSTANTES salen del WSDL del servicio
 * (…/TCRMServicesWebService?WSDL). Si el WSDL cambia, solo se ajusta aquí.
 */
class SoapClient(
    private val client: OkHttpClient = defaultClient()
) {

    /** Bloqueante: llamar siempre desde un hilo secundario (el repositorio usa Dispatchers.IO). */
    fun queryTrm(date: String): SoapExchange {
        val xml = buildEnvelope(date)
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("SOAPAction", SOAP_ACTION)      // SOAP 1.1
            .header("Accept", "text/xml")
            .post(xml.toRequestBody(CONTENT_TYPE.toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            // Un Fault llega con HTTP 500 y XML en el cuerpo: por eso leemos el cuerpo siempre.
            val body = response.body?.string().orEmpty()
            return SoapExchange(request = xml, httpCode = response.code, response = body)
        }
    }

    companion object {
        // ---------- CONSTANTES (tomadas del WSDL) ----------
        const val ENDPOINT =
            "https://www.superfinanciera.gov.co/SuperfinancieraWebServiceTRM/" +
                "TCRMServicesWebService/TCRMServicesWebService"
        const val NAMESPACE =
            "http://action.trm.services.generic.action.superfinanciera.nexura.sc.com.co/"
        const val OPERATION = "queryTCRM"
        const val PARAM_DATE = "tcrmQueryAssociatedDate"
        const val SOAP_ACTION = ""
        const val CONTENT_TYPE = "text/xml; charset=utf-8"
        // ---------------------------------------------------

        private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

        /**
         * Construye el sobre. La fecha se valida con una expresión estricta para
         * que nunca se inserte texto arbitrario dentro del XML.
         */
        fun buildEnvelope(date: String): String {
            require(DATE_REGEX.matches(date)) { "Fecha con formato inválido" }
            return """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:act="$NAMESPACE">
  <soapenv:Header/>
  <soapenv:Body>
    <act:$OPERATION>
      <$PARAM_DATE>$date</$PARAM_DATE>
    </act:$OPERATION>
  </soapenv:Body>
</soapenv:Envelope>"""
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
