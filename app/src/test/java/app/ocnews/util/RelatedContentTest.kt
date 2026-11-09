package app.ocnews.util

import org.junit.Assert.assertEquals
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
    fun `related headings work for other publishers`() {
        assertTrue(isRelatedHeading("Artículos relacionados"))
        assertTrue(isRelatedHeading("Noticias relacionadas"))
        assertTrue(isRelatedHeading("Contenido relacionado"))
        assertTrue(isRelatedHeading("Te recomendamos"))
        assertTrue(isRelatedHeading("Lee también:"))
        assertTrue(isRelatedHeading("More from The Verge"))
        assertTrue(isRelatedHeading("You might also like"))
        assertTrue(isRelatedHeading("Related stories"))
    }

    @Test
    fun `cross references match for any site, not just the one in the feed`() {
        assertTrue(isRelatedLink("En Genbeta | Cómo cambiar el DNS de tu router"))
        assertTrue(isRelatedLink("En Applesfera | Los mejores accesorios para tu Mac"))
        assertTrue(isRelatedLink("En Motorpasión | El nuevo eléctrico que llega en 2027"))
        assertTrue(isRelatedLink("En El País | La noticia del día"))
    }

    @Test
    fun `cross references to other articles match`() {
        assertTrue(
            isRelatedLink(
                "En Xataka | La luna estaría compuesta por un fragmento de la Tierra que se desprendió " +
                    "hace miles de millones de años"
            )
        )
        assertTrue(isRelatedLink("En Xataka Móvil | Lo nuevo de Braun te da información"))
    }

    @Test
    fun `a normal sentence starting with En does not match`() {
        assertFalse(isRelatedLink("En Xataka hemos hablado muchas veces de este tema."))
        assertFalse(isRelatedLink("En 2026 el mercado cambió por completo."))
        assertFalse(
            isRelatedLink(
                "En Xataka | " + "un título larguísimo ".repeat(30)
            )
        )
    }

    @Test
    fun `a short block that is almost entirely a link is boilerplate`() {
        assertTrue(isCardBlock("""<p>En Xataka | <a href="https://x.com/a">La luna estaría compuesta por un fragmento de la Tierra</a></p>"""))
        assertTrue(isCardBlock("""<p><a href="https://x.com/a">Título de otro artículo</a></p>"""))
        assertTrue(isCardBlock("""<p>Imagen | <a href="https://x.com/a">Grebenkov</a></p>"""))
    }

    @Test
    fun `a card without a pipe between label and title is boilerplate`() {
        // Xataka also emits cards as "<a>En Xataka</a><a>Title</a>", no separator.
        assertTrue(
            isCardBlock(
                """<a href="https://x.com/a">En Xataka</a>
                   <a href="https://x.com/a">Ya es oficial: Elon Musk anuncia el principio del fin del Cybertruck</a>"""
            )
        )
    }

    @Test
    fun `prose with links is not boilerplate`() {
        assertFalse(
            isCardBlock(
                """<p>Las <a href="https://x.com/a">aspiradoras Dyson</a> son de las más demandadas por quienes quieren un dispositivo de limpieza para su hogar con buena calidad y a buen precio.</p>"""
            )
        )
        assertFalse(isCardBlock("""<p>Texto sin enlaces de ningún tipo.</p>"""))
        // A real sentence that happens to contain a link keeps text outside it.
        assertFalse(
            isCardBlock("""<p>Y eso es un problema en la <a href="https://x.com/a">gran huerta de cítricos de la UE</a>.</p>""")
        )
        // Long link lists are ignored by length, and prose is kept.
        assertFalse(isCardBlock("""<p><a href="https://x.com/a">""" + "palabra ".repeat(60) + """</a></p>"""))
    }

    @Test
    fun `a card glued to the next paragraph loses the card but keeps the prose`() {
        val html =
            """<p><a href="https://x.com/a"><img src="https://cdn/t.jpg"></a><a href="https://x.com/a">En Xataka</a><a href="https://x.com/a">Ya es oficial: Elon Musk anuncia el principio del fin del Cybertruck</a>El despliegue. Latinoamérica es la primera.</p>"""
        val cleaned = stripCardLinkRuns(html)
        assertFalse(cleaned.contains("Elon Musk"))
        assertFalse(cleaned.contains("En Xataka"))
        assertTrue(cleaned.contains("El despliegue. Latinoamérica es la primera."))
    }

    @Test
    fun `an inline image wrapped in a link is kept`() {
        val html = """<p><a href="https://x.com/foto"><img src="https://cdn/foto.jpg"></a></p><p>Texto del artículo.</p>"""
        assertEquals(html, stripCardLinkRuns(html))
    }

    @Test
    fun `prose with links is untouched by the card stripper`() {
        val html = """<p>Las <a href="https://x.com/a">aspiradoras Dyson</a> son las más demandadas del mercado.</p>"""
        assertEquals(html, stripCardLinkRuns(html))
    }

    @Test
    fun `unrelated text does not match`() {
        assertFalse(isRelatedHeading("El Gobierno lleva a TikTok ante la Fiscalía"))
        assertFalse(isRelatedHeading(""))
        assertFalse(isRelatedHeading("Leer más tarde"))
    }
}
