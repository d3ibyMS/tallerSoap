package co.edu.udenar.trmcolombia

import co.edu.udenar.trmcolombia.model.TrmConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrmConverterTest {
    @Test fun convierte() = assertEquals(39124.5, TrmConverter.usdToCop(10.0, 3912.45), 0.0001)
    @Test fun aceptaComa() = assertEquals(12.5, TrmConverter.parseAmount("12,5")!!, 0.0001)
    @Test fun rechazaTexto() = assertNull(TrmConverter.parseAmount("abc"))
    @Test fun rechazaNegativo() = assertNull(TrmConverter.parseAmount("-3"))
}
