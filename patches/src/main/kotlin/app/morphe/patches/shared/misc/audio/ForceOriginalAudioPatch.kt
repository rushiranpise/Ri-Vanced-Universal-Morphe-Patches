package app.morphe.patches.shared.misc.audio

import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.extensions.addInstruction
import app.morphe.patcher.extensions.addInstructions
import app.morphe.patcher.extensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.patches.shared.misc.settings.preference.BasePreferenceScreen
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutable
import app.morphe.util.findMethodFromToString
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/ForceOriginalAudioPatch;"

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun forceOriginalAudioPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
    fixUseLocalizedAudioTrackFlag: BytecodePatchContext.() -> Boolean,
    getMainActivityOnCreateMethod: BytecodePatchContext.() -> MutableMethod,
    subclassExtensionClassDescriptor: String,
    preferenceScreen: BasePreferenceScreen.Screen,
) = bytecodePatch(
    name = "Force original audio",
    description = "Adds an option to always use the original audio track.",
) {
    block()

    dependsOn(addResourcesPatch)

    execute {
        addResources("shared", "misc.audio.forceOriginalAudioPatch")

        preferenceScreen.addPreferences(
            SwitchPreference(
                key = "rivanced_force_original_audio",
                tag = "app.morphe.extension.shared.settings.preference.ForceOriginalAudioSwitchPreference",
            ),
        )

        getMainActivityOnCreateMethod().addInstruction(
            0,
            "invoke-static { }, $subclassExtensionClassDescriptor->setEnabled()V",
        )

        // Disable feature flag that ignores the default track flag
        // and instead overrides to the user region language.
        if (fixUseLocalizedAudioTrackFlag()) {
            selectAudioStreamMethodMatch.method.insertLiteralOverride(
                selectAudioStreamMethodMatch[0],
                "$EXTENSION_CLASS_DESCRIPTOR->ignoreDefaultAudioStream(Z)Z",
            )
        }

        val isDefaultAudioTrackMethod =
            formatStreamModelToStringMethodMatch.immutableMethod.findMethodFromToString("isDefaultAudioTrack=")
        val audioTrackDisplayNameMethod =
            formatStreamModelToStringMethodMatch.immutableMethod.findMethodFromToString("audioTrackDisplayName=")
        val audioTrackIdMethod =
            formatStreamModelToStringMethodMatch.immutableMethod.findMethodFromToString("audioTrackId=")

        formatStreamModelToStringMethodMatch.classDef.apply {
            // Add a new field to store the override.
            val helperFieldName = "patch_isDefaultAudioTrackOverride"
            fields.add(
                ImmutableField(
                    type,
                    helperFieldName,
                    "Ljava/lang/Boolean;",
                    // Boolean is a 100% immutable class (all fields are final)
                    // and safe to write to a shared field without volatile/synchronization,
                    // but without volatile the field can show stale data
                    // and the same field is calculated more than once by different threads.
                    AccessFlags.PRIVATE.value or AccessFlags.VOLATILE.value,
                    null,
                    null,
                    null,
                ).toMutable(),
            )

            // Clone the method to add additional registers because the
            // isDefaultAudioTrack() has only 1 or 2 registers and 3 are needed.
            val clonedMethod = isDefaultAudioTrackMethod.cloneMutable(
                additionalRegisters = 4
            )

            // Replace existing method with cloned with more registers.
            methods.apply {
                remove(isDefaultAudioTrackMethod)
                add(clonedMethod)
            }

            clonedMethod.apply {
                // Free registers are added
                val free1 = isDefaultAudioTrackMethod.implementation!!.registerCount + 1
                val free2 = free1 + 1
                val insertIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN)
                val originalResultRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                clonedMethod.addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                            iget-object v$free1, p0, $type->$helperFieldName:Ljava/lang/Boolean;
                            if-eqz v$free1, :call_extension            
                            invoke-virtual { v$free1 }, Ljava/lang/Boolean;->booleanValue()Z
                            move-result v$free1
                            return v$free1
                            
                            :call_extension
                            invoke-virtual { p0 }, $audioTrackIdMethod
                            move-result-object v$free1
                            
                            invoke-virtual { p0 }, $audioTrackDisplayNameMethod
                            move-result-object v$free2
        
                            invoke-static { v$originalResultRegister, v$free1, v$free2 }, ${EXTENSION_CLASS_DESCRIPTOR}->isDefaultAudioStream(ZLjava/lang/String;Ljava/lang/String;)Z
                            move-result v$free1
                            
                            invoke-static { v$free1 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                            move-result-object v$free2
                            iput-object v$free2, p0, $type->$helperFieldName:Ljava/lang/Boolean;
                            return v$free1
                        """
                )
            }
        }

        executeBlock()
    }
}
