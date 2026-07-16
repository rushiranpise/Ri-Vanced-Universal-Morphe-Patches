package app.rivanced.patches.all.misc.debugging

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

@Suppress("unused")
val enableAndroidDebuggingPatch = resourcePatch(
    name = "Enable Android debugging",
    description = "Enables Android debugging capabilities. This can slow down the app.",
    default = false,
) {
    execute {
        document("AndroidManifest.xml").use { document ->
            val applicationNode =
                document
                    .getElementsByTagName("application")
                    .item(0) as Element

            // set application as debuggable
            applicationNode.setAttribute("android:debuggable", "true")
        }
    }
}
