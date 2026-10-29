package co.edu.udenar.trmcolombia

import co.edu.udenar.trmcolombia.data.TrmXmlParser
import co.edu.udenar.trmcolombia.model.TrmParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser

class TrmXmlParserTest {

    private fun load(name: String): String =
        javaClass.classLoader!!.getResource(name)!!.readText()

    private fun parse(xml: String) = TrmXmlParser.parse(xml) { KXmlParser() }

    @Test
    fun respuestaCorrecta_extraeElValor() {
        val r = parse(load("respuesta_ok.xml"))
        assertTrue(r is TrmParseResult.Success)
        r as TrmParseResult.Success
        assertEquals(3912.45, r.value, 0.0001)
        assertEquals("COP", r.unit)
    }

    @Test
    fun respuestaConFault_sedetecta() {
        val r = parse(load("respuesta_fault.xml"))
        assertTrue(r is TrmParseResult.Fault)
        assertEquals("Error interno del servicio", (r as TrmParseResult.Fault).message)
    }

    @Test
    fun successFalso_esSinDato() {
        assertEquals(TrmParseResult.NoData, parse(load("respuesta_sin_dato.xml")))
    }

    @Test
    fun xmlMalFormado_esInvalido() {
        assertTrue(parse("<html><body>Error 404") is TrmParseResult.Invalid)
    }
}
