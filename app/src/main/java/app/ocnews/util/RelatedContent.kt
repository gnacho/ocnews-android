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

// Single-line cross references such as "En Xataka | Another article title",
// which the same publisher appends to the end of its articles.
fun isRelatedLink(text: String): Boolean {
    val normalized = normalizeForMatch(text)
    if (normalized.isEmpty() || normalized.length > 300) return false
    if (!normalized.startsWith("en ")) return false
    return normalized.contains(" | ")
}
