package co.edu.udenar.trmcolombia

import co.edu.udenar.trmcolombia.data.SoapClient
import org.junit.Assert.assertTrue
import org.junit.Test

class SoapClientTest {

    @Test
    fun sobre_contieneOperacionYFecha() {
        val xml = SoapClient.buildEnvelope("2026-10-27")
        assertTrue(xml.contains("<act:queryTCRM>"))
        assertTrue(xml.contains("<tcrmQueryAssociatedDate>2026-10-27</tcrmQueryAssociatedDate>"))
        assertTrue(xml.contains("soapenv:Envelope"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fechaConTextoArbitrario_serechaza() {
        SoapClient.buildEnvelope("2026-10-27</x><inyeccion/>")
    }
}
