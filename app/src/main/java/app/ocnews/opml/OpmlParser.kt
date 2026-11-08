package app.ocnews.opml

import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class OpmlEntry(
    val feedUrl: String,
    val folderName: String?,
)

// Parses an OPML subscription list into (feedUrl, folderName) entries.
// Pure JVM (uses javax DOM, available on Android too), testable without Android.
object OpmlParser {
    fun parse(xml: String): List<OpmlEntry> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val body = doc.getElementsByTagName("body").item(0) ?: return emptyList()
        val entries = mutableListOf<OpmlEntry>()
        collectOutlines(body, null, entries)
        return entries
    }

    private fun collectOutlines(node: Node, folder: String?, entries: MutableList<OpmlEntry>) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName != "outline") continue
            val attrs = child.attributes ?: continue
            val xmlUrl = attrs.getNamedItem("xmlUrl")?.nodeValue
            val text = attrs.getNamedItem("text")?.nodeValue
                ?: attrs.getNamedItem("title")?.nodeValue
            if (!xmlUrl.isNullOrBlank()) {
                entries.add(OpmlEntry(xmlUrl, folder))
            } else {
                // A folder/group: recurse with its name (fall back to parent folder).
                collectOutlines(child, text?.takeIf { it.isNotBlank() } ?: folder, entries)
            }
        }
    }
}
