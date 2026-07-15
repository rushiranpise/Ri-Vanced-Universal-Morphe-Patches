package app.morphe.patches.all.misc.screenshot

import app.morphe.patcher.extensions.methodReference
import app.morphe.patcher.extensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private val registerScreenCaptureCallbackMethodReference = ImmutableMethodReference(
    "Landroid/app/Activity;",
    "registerScreenCaptureCallback",
    listOf(
        "Ljava/util/concurrent/Executor;",
        "Landroid/app/Activity\$ScreenCaptureCallback;",
    ),
    "V",
)

private val unregisterScreenCaptureCallbackMethodReference = ImmutableMethodReference(
    "Landroid/app/Activity;",
    "unregisterScreenCaptureCallback",
    listOf(
        "Landroid/app/Activity\$ScreenCaptureCallback;",
    ),
    "V",
)

@Suppress("unused")
val preventScreenshotDetectionPatch = bytecodePatch("Prevent screenshot detection", "Removes the registration of all screen capture callbacks. This prevents the app from detecting screenshots.", false) {
    dependsOn(
        transformInstructionsPatch(
            filterMap = { _, _, instruction, instructionIndex ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@transformInstructionsPatch null

                val reference = instruction.methodReference ?: return@transformInstructionsPatch null

                instructionIndex.takeIf {
                    MethodUtil.methodSignaturesMatch(reference, registerScreenCaptureCallbackMethodReference) ||
                        MethodUtil.methodSignaturesMatch(reference, unregisterScreenCaptureCallbackMethodReference)
                }
            },
            transform = { mutableMethod, instructionIndex ->
                mutableMethod.removeInstruction(instructionIndex)
            },
        ),
    )
}
