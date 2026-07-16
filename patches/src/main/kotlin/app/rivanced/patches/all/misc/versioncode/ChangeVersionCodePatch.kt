package app.rivanced.patches.all.misc.versioncode

import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.rivanced.util.getNode
import org.w3c.dom.Element

@Suppress("unused")
val changeVersionCodePatch = resourcePatch(
    name = "Change version code",
    description = "Changes the version code of the app. This will turn off app store updates " +
        "and allows downgrading an existing app install to an older app version.",
    default = false,
) {
    val versionCode by intOption(
        default = Int.MAX_VALUE,
        values = mapOf(
            "Lowest" to 1,
            "Highest" to Int.MAX_VALUE,
        ),
        key = "Version code",
        description = "The version code to use. Using the highest value turns off app store " +
            "updates and allows downgrading an existing app install to an older app version.",
        required = true,
    ) { versionCode -> versionCode!! >= 1 }

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifestElement = document.getNode("manifest") as Element
            manifestElement.setAttribute("android:versionCode", "$versionCode")
        }
    }
}
