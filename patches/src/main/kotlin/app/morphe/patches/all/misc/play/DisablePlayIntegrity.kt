package app.morphe.patches.all.misc.play

import app.morphe.patcher.extensions.methodReference
import app.morphe.patcher.extensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/play/DisablePlayIntegrityPatch;"

private val CONTEXT_BIND_SERVICE_METHOD_REFERENCE = ImmutableMethodReference(
    "Landroid/content/Context;",
    "bindService",
    listOf("Landroid/content/Intent;", "Landroid/content/ServiceConnection;", "I"),
    "Z",
)

@Suppress("unused")
val disablePlayIntegrityPatch = bytecodePatch("Disable Play Integrity", "Prevents apps from using Play Integrity by pretending it is not available.", false) {
    extendWith("extensions/all/misc/disable-play-integrity.mpe")

    dependsOn(
        transformInstructionsPatch(
            filterMap = filterMap@{ classDef, method, instruction, instructionIndex ->
                val reference = instruction
                    .methodReference
                    ?.takeIf {
                        MethodUtil.methodSignaturesMatch(CONTEXT_BIND_SERVICE_METHOD_REFERENCE, it)
                    }
                    ?: return@filterMap null

                Triple(instruction as Instruction35c, instructionIndex, reference.parameterTypes)
            },
            transform = { method, entry ->
                val (instruction, index, parameterTypes) = entry
                val parameterString = parameterTypes.joinToString(separator = "")
                val registerString =
                    "v${instruction.registerC}, v${instruction.registerD}, v${instruction.registerE}, v${instruction.registerF}"

                method.replaceInstruction(
                    index,
                    "invoke-static { $registerString }, $EXTENSION_CLASS_DESCRIPTOR->bindService(Landroid/content/Context;$parameterString)Z",
                )
            },
        ),
    )
}
