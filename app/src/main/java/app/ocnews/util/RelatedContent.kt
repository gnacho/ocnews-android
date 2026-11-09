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

// Removes runs of consecutive links with no text between them, which is how
// feeds embed "related article" cards: an optional thumbnail, a label ("En
// Xataka") and the title, all as anchors glued to the next paragraph. A single
// anchor with only an image (the usual lightbox around an inline image) is kept.
fun stripCardLinkRuns(html: String): String {
    val anchor = Regex("(?is)<a\\b[^>]*>(.*?)</a>")
    val matches = anchor.findAll(html).toList()
    if (matches.isEmpty()) return html

    val groups = mutableListOf<MutableList<MatchResult>>()
    for (match in matches) {
        val last = groups.lastOrNull()
        if (last != null) {
            val gap = html.substring(last.last().range.last + 1, match.range.first)
            if (gap.isBlank()) {
                last.add(match)
                continue
            }
        }
        groups.add(mutableListOf(match))
    }

    fun anchorText(match: MatchResult): Int =
        match.groupValues[1].replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ")
            .count { !it.isWhitespace() }

    // Two or more glued anchors is a card. A single anchor inside a paragraph is
    // an ordinary inline link and must stay.
    val runs = groups.filter { group ->
        group.size >= 2 && group.sumOf { anchorText(it) } >= 15
    }
    if (runs.isEmpty()) return html

    val builder = StringBuilder()
    var cursor = 0
    for (run in runs) {
        val start = run.first().range.first
        val end = run.last().range.last + 1
        if (start < cursor) continue
        builder.append(html, cursor, start)
        cursor = end
    }
    builder.append(html, cursor, html.length)
    return builder.toString()
}

// A card is a short block that is basically a link: the text outside the link is
// just a label ("En Xataka", "Imágenes", "Vía"...), so it contributes nothing to
// the article wherever it appears. Prose keeps at least a sentence outside links.
private class LinkCounts(val total: Int, val link: Int) {
    val free: Int get() = total - link
}

private fun linkCounts(html: String): LinkCounts {
    val total = visibleLength(html)
    val link = Regex("<a\\b[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .findAll(html)
        .sumOf { it.groupValues[1].replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").count { c -> !c.isWhitespace() } }
    return LinkCounts(total, link)
}

fun isCardBlock(
    html: String,
    maxChars: Int = 300,
    minLinkChars: Int = 8,
    maxFreeChars: Int = 12,
): Boolean {
    val counts = linkCounts(html)
    return counts.total in 15..maxChars &&
        counts.link >= minLinkChars &&
        counts.free <= maxFreeChars
}

// Single-line cross references such as "En Xataka | Another article title",
// which the same publisher appends to the end of its articles.
fun isRelatedLink(text: String): Boolean {
    val normalized = normalizeForMatch(text)
    if (normalized.isEmpty() || normalized.length > 300) return false
    if (!normalized.startsWith("en ")) return false
    return normalized.contains(" | ")
}
