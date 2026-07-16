package app.rivanced.patches.all.misc.shortcut.sharetargets

import app.morphe.patcher.patch.resourcePatch
import app.rivanced.util.asSequence
import app.rivanced.util.getNode
import org.w3c.dom.Element
import java.io.FileNotFoundException
import java.util.logging.Logger

@Suppress("unused")
val removeShareTargetsPatch = resourcePatch("Remove share targets", "Removes share targets like directly sharing to a frequent contact.", false) {
    execute {
        try {
            document("res/xml/shortcuts.xml")
        } catch (_: FileNotFoundException) {
            return@execute Logger.getLogger(this::class.java.name).warning(
                "The app has no shortcuts. No changes applied.",
            )
        }.use { document ->
            val rootNode = document.getNode("shortcuts") as? Element ?: return@use

            document.getElementsByTagName("share-target").asSequence().forEach {
                rootNode.removeChild(it)
            }
        }
    }
}
