package app.ocnews.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedContentTest {

    @Test
    fun `spanish related headings match with or without accents`() {
        assertTrue(isRelatedHeading("También pueden interesarte"))
        assertTrue(isRelatedHeading("TAMBIÉN TE PUEDE INTERESAR:"))
        assertTrue(isRelatedHeading("También te pueden interesar"))
        assertTrue(isRelatedHeading("  También puedes leer  "))
        assertTrue(isRelatedHeading("Más noticias"))
    }

    @Test
    fun `english related headings match`() {
        assertTrue(isRelatedHeading("Related articles"))
        assertTrue(isRelatedHeading("You may also like"))
    }

    @Test
    fun `a paragraph that mentions the phrase does not match`() {
        assertFalse(
            isRelatedHeading(
                "En este artículo te puede interesar ver cómo la compañía ha cambiado su estrategia " +
                    "durante los últimos años y qué supone para sus clientes."
            )
        )
    }

    @Test
    fun `unrelated text does not match`() {
        assertFalse(isRelatedHeading("El Gobierno lleva a TikTok ante la Fiscalía"))
        assertFalse(isRelatedHeading(""))
        assertFalse(isRelatedHeading("Leer más tarde"))
    }
}
