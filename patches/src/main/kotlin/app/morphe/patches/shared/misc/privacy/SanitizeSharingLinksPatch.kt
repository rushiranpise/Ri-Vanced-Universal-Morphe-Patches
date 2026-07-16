package app.morphe.patches.shared.misc.privacy

import app.morphe.patcher.CompositeMatch
import app.morphe.patcher.extensions.addInstructions
import app.morphe.patcher.extensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.PreferenceCategory
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/SanitizeSharingLinksPatch;"

/**
 * Patch shared by YouTube and YT Music.
 */
internal fun sanitizeSharingLinksPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
    preferenceScreen: BasePreferenceScreen.Screen,
    replaceMusicLinksWithYouTube: Boolean = false,
) = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links.",
) {
    block()

    dependsOn(addResourcesPatch)

    execute {
        executeBlock()

        addResources("shared", "misc.privacy.sanitizeSharingLinksPatch")

        val sanitizePreference = SwitchPreference("rivanced_sanitize_sharing_links")

        preferenceScreen.addPreferences(
            if (replaceMusicLinksWithYouTube) {
                PreferenceCategory(
                    titleKey = null,
                    sorting = Sorting.UNSORTED,
                    tag = "app.morphe.extension.shared.settings.preference.NoTitlePreferenceCategory",
                    preferences = setOf(
                        sanitizePreference,
                        SwitchPreference("rivanced_replace_music_with_youtube"),
                    ),
                )
            } else {
                sanitizePreference
            },
        )


        fun CompositeMatch.hookUrlString(matchIndex: Int) {
            val index = get(matchIndex)
            val urlRegister = method.getInstruction<OneRegisterInstruction>(index).registerA

            method.addInstructions(
                index + 1,
                """
                    invoke-static { v$urlRegister }, $EXTENSION_CLASS_DESCRIPTOR->sanitize(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$urlRegister
                """
            )
        }

        fun CompositeMatch.hookIntentPutExtra(matchIndex: Int) {
            val index = get(matchIndex)
            val urlRegister = method.getInstruction<FiveRegisterInstruction>(index).registerE

            method.addInstructionsAtControlFlowLabel(
                index,
                """
                    invoke-static { v$urlRegister }, $EXTENSION_CLASS_DESCRIPTOR->sanitize(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$urlRegister
                """
            )
        }


        // YouTube share sheet copy link.
        youTubeCopyTextMethodMatch.hookUrlString(0)

        // YouTube share sheet other apps.
        youTubeShareSheetMethodMatch.hookIntentPutExtra(3)

        // Native system share sheet.
        youTubeSystemShareSheetMethodMatch.hookIntentPutExtra(3)
    }
}
