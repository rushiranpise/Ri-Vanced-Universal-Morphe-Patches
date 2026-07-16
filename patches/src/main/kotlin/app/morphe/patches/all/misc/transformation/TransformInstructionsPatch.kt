package app.morphe.patches.all.misc.transformation

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.forEachInstructionAsSequence
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

@Deprecated(
    "Use forEachInstructionAsSequence directly within a bytecodePatch", ReplaceWith(
        "bytecodePatch { apply { forEachInstructionAsSequence(filterMap, transform) } }",
        "app.morphe.util.forEachInstructionAsSequence",
        "app.morphe.patcher.patch.bytecodePatch",
    )
)
fun <T> transformInstructionsPatch(
    filterMap: (ClassDef, Method, Instruction, Int) -> T?,
    transform: (MutableMethod, T) -> Unit,
) = bytecodePatch {
    execute { forEachInstructionAsSequence(filterMap, transform) }
}
