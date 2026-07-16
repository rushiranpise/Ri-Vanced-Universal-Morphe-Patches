package app.rivanced.util

import app.morphe.patcher.util.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.asSequence(): Sequence<Node> =
    (0 until length).asSequence().map { item(it) }

@Suppress("UNCHECKED_CAST")
fun Node.childElementsSequence(): Sequence<Element> =
    childNodes.asSequence().filter { it.nodeType == Node.ELEMENT_NODE } as Sequence<Element>

internal fun Document.getNode(tagName: String): Node =
    getElementsByTagName(tagName).item(0)
