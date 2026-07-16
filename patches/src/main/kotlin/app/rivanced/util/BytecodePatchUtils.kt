package app.rivanced.util

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.rivanced.patcher.classDefs
import app.rivanced.patcher.extensions.addInstructions
import app.rivanced.patcher.extensions.instructionsOrNull
import app.rivanced.patcher.firstMethod
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

fun <T> BytecodePatchContext.forEachInstructionAsSequence(
    match: (classDef: ClassDef, method: Method, instruction: Instruction, index: Int) -> T?,
    transform: (MutableMethod, T) -> Unit,
) {
    classDefs.flatMap { classDef ->
        classDef.methods.mapNotNull { method ->
            val matches = method.instructionsOrNull?.mapIndexedNotNull { index, instruction ->
                match(classDef, method, instruction, index)
            } ?: return@mapNotNull null

            if (matches.isEmpty()) null else method to matches
        }
    }.forEach { (method, matches) ->
        val mutableMethod = firstMethod(method)
        val pendingMatches = ArrayDeque(matches)

        while (pendingMatches.isNotEmpty()) {
            transform(mutableMethod, pendingMatches.removeLast())
        }
    }
}

fun MutableMethod.returnEarly() {
    addInstructions(0, "return-void")
}

fun MutableMethod.returnEarly(value: Boolean) {
    val encodedValue = if (value) "0x1" else "0x0"
    addInstructions(
        0,
        """
            const/4 v0, $encodedValue
            return v0
        """,
    )
}
