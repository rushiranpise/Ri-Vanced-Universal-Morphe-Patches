package app.rivanced.patches.all.misc.network

import app.morphe.patcher.patch.resourcePatch
import app.rivanced.patches.all.misc.debugging.enableAndroidDebuggingPatch
import app.rivanced.util.Utils.trimIndentMultiline
import app.rivanced.util.getNode
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val overrideCertificatePinningPatch = resourcePatch(
    name = "Override certificate pinning",
    description = "Overrides certificate pinning, allowing to inspect traffic via a proxy.",
    default = false,
) {
    dependsOn(enableAndroidDebuggingPatch)

    execute {
        val resXmlDirectory = get("res/xml")

        // Add android:networkSecurityConfig="@xml/network_security_config" and the "networkSecurityConfig" attribute if it does not exist.
        document("AndroidManifest.xml").use { document ->
            val applicationNode = document.getNode("application") as Element
            applicationNode.apply {
                if (!hasAttribute("networkSecurityConfig")) {
                    attributes.setNamedItem(
                        document.createAttribute("android:networkSecurityConfig").apply {
                            value = "@xml/network_security_config"
                        }
                    )
                }
            }
        }

        // In case the file does not exist create the "network_security_config.xml" file.
        File(resXmlDirectory, "network_security_config.xml").apply {
            writeText(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <network-security-config>
                        <base-config cleartextTrafficPermitted="true">
                            <trust-anchors>
                                <certificates src="system" />
                                <certificates
                                    src="user"
                                    overridePins="true" />
                            </trust-anchors>
                        </base-config>
                        <debug-overrides>
                            <trust-anchors>
                                <certificates src="system" />
                                <certificates
                                    src="user"
                                    overridePins="true" />
                            </trust-anchors>
                        </debug-overrides>
                    </network-security-config>
                    """.trimIndentMultiline(),
            )
        }
    }
}
