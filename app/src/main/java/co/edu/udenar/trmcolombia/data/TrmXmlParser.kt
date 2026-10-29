package co.edu.udenar.trmcolombia.data

import android.util.Xml
import co.edu.udenar.trmcolombia.model.TrmParseResult
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Capa "Analizador XML": lee la respuesta con XmlPullParser (viene con Android).
 * Detecta tres situaciones: Fault, success=false y respuesta correcta.
 *
 * Se ignoran los prefijos de espacio de nombres (ns2:return -> return), así que
 * el análisis no depende de cómo el servidor los nombre.
 *
 * [newParser] se puede reemplazar en las pruebas unitarias (JVM) por otro XmlPullParser.
 */
object TrmXmlParser {

    fun parse(
        xml: String,
        newParser: () -> XmlPullParser = { Xml.newPullParser() }
    ): TrmParseResult {
        val fields = HashMap<String, String>()
        var sawFault = false
        var insideFault = false
        var faultMessage: String? = null
        var current: String? = null

        try {
            val parser = newParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        current = localName(parser.name)
                        if (current == "Fault") {
                            sawFault = true
                            insideFault = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        val name = current
                        if (text.isNotEmpty() && name != null) {
                            if (insideFault) {
                                // SOAP 1.1: faultstring · SOAP 1.2: Reason/Text
                                if (name == "faultstring" || name == "Text") faultMessage = text
                            } else {
                                fields[name] = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (localName(parser.name) == "Fault") insideFault = false
                        current = null
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            return TrmParseResult.Invalid("XML mal formado")
        }

        if (sawFault) return TrmParseResult.Fault(faultMessage ?: "Fault sin descripción")

        val success = fields["success"] ?: return TrmParseResult.Invalid("Falta el campo success")
        if (!success.equals("true", ignoreCase = true)) return TrmParseResult.NoData

        val value = fields["value"]?.replace(',', '.')?.toDoubleOrNull()
            ?: return TrmParseResult.Invalid("Falta el campo value o no es numérico")

        return TrmParseResult.Success(
            value = value,
            unit = fields["unit"] ?: "COP",
            validityFrom = fields["validityFrom"].orEmpty(),
            validityTo = fields["validityTo"].orEmpty()
        )
    }

    private fun localName(name: String): String = name.substringAfter(':')
}
