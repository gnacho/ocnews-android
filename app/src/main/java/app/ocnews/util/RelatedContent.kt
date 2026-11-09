package app.ocnews.util

import java.text.Normalizer

// Headings that introduce the "you may also like" block many sites append to an
// article, written without accents and in lower case.
private val relatedHeadings = listOf(
    // Spanish
    "tambien pueden interesarte",
    "tambien te puede interesar",
    "tambien te pueden interesar",
    "te puede interesar",
    "te pueden interesar",
    "tambien puedes leer",
    "tambien podria interesarte",
    "podria interesarte",
    "te recomendamos",
    "articulos relacionados",
    "noticias relacionadas",
    "contenido relacionado",
    "lee tambien",
    "mas noticias",
    // English
    "related articles",
    "related posts",
    "related stories",
    "you may also like",
    "you might also like",
    "more from",
    "also read",
    "read more",
    "recommended for you",
    // Others
    "a lire aussi",
    "das konnte sie auch interessieren",
)

// Lower case without diacritics, so the comparison does not depend on accents.
fun normalizeForMatch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()

// True when the whole text looks like one of those headings. A real paragraph
// that merely mentions the phrase is too long to match.
fun isRelatedHeading(text: String): Boolean {
    val normalized = normalizeForMatch(text)
    if (normalized.isEmpty() || normalized.length > 60) return false
    return relatedHeadings.any { normalized.startsWith(it) }
}

// Visible characters of a HTML fragment: tags and whitespace removed.
private fun visibleLength(html: String): Int =
    html.replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .count { !it.isWhitespace() }

// Share of the visible text that sits inside links. Boilerplate blocks (related
// cards, cross references, tag lists) are almost entirely links, while prose
// keeps the links as a small fraction.
fun linkTextRatio(html: String): Float {
    val total = visibleLength(html)
    if (total == 0) return 0f
    val linkChars = Regex("<a\\b[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .findAll(html)
        .sumOf { it.groupValues[1].replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").count { c -> !c.isWhitespace() } }
    return linkChars.toFloat() / total
}

// A short block that is mostly a link is boilerplate, whatever the site calls it.
fun isLinkHeavyBlock(html: String, maxChars: Int = 300, threshold: Float = 0.5f): Boolean =
    visibleLength(html) in 1..maxChars && linkTextRatio(html) >= threshold

// Single-line cross references such as "En Xataka | Another article title",
// which the same publisher appends to the end of its articles.
fun isRelatedLink(text: String): Boolean {
    val normalized = normalizeForMatch(text)
    if (normalized.isEmpty() || normalized.length > 300) return false
    if (!normalized.startsWith("en ")) return false
    return normalized.contains(" | ")
}
