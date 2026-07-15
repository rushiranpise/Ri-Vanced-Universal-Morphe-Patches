@file:Suppress("UNCHECKED_CAST")

package app.morphe.patcher.extensions

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.toInstructions as compileSmaliInstructions
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

fun MutableMethod.addInstruction(index: Int, instruction: String): Unit =
    with(InstructionExtensions) { this@addInstruction.addInstruction(index, instruction) }

fun MutableMethod.addInstruction(instruction: String): Unit =
    with(InstructionExtensions) { this@addInstruction.addInstruction(instruction) }

fun MutableMethod.addInstruction(
    index: Int,
    instruction: com.android.tools.smali.dexlib2.builder.BuilderInstruction,
): Unit = with(InstructionExtensions) { this@addInstruction.addInstruction(index, instruction) }

fun MutableMethod.addInstruction(index: Int, instruction: Instruction): Unit {
    if (instruction is com.android.tools.smali.dexlib2.builder.BuilderInstruction) {
        addInstruction(index, instruction)
    }
}

fun MutableMethod.addInstruction(
    instruction: com.android.tools.smali.dexlib2.builder.BuilderInstruction,
): Unit = addInstruction(implementation!!.instructions.count(), instruction)

fun MutableMethod.addInstructions(index: Int, instructions: String): Unit =
    with(InstructionExtensions) { this@addInstructions.addInstructions(index, instructions) }

fun MutableMethod.addInstructions(instructions: String): Unit =
    with(InstructionExtensions) { this@addInstructions.addInstructions(instructions) }

fun MutableMethod.addInstructions(index: Int, instructions: List<Instruction>): Unit {
    instructions.forEachIndexed { offset, instruction ->
        if (instruction is com.android.tools.smali.dexlib2.builder.BuilderInstruction) {
            addInstruction(index + offset, instruction)
        }
    }
}

fun MutableMethod.addInstructionsWithLabels(
    index: Int,
    instructions: String,
    vararg externalLabels: ExternalLabel,
): Unit = with(InstructionExtensions) {
    this@addInstructionsWithLabels.addInstructionsWithLabels(index, instructions, *externalLabels)
}

fun MutableMethod.removeInstruction(index: Int): Unit =
    with(InstructionExtensions) { this@removeInstruction.removeInstruction(index) }

fun MutableMethod.removeInstructions(index: Int, count: Int): Unit =
    with(InstructionExtensions) { this@removeInstructions.removeInstructions(index, count) }

fun MutableMethod.removeInstructions(index: Int): Unit =
    with(InstructionExtensions) { this@removeInstructions.removeInstructions(index) }

fun MutableMethod.replaceInstruction(index: Int, instruction: String): Unit =
    with(InstructionExtensions) { this@replaceInstruction.replaceInstruction(index, instruction) }

fun MutableMethod.replaceInstruction(
    index: Int,
    instruction: com.android.tools.smali.dexlib2.builder.BuilderInstruction,
): Unit {
    removeInstruction(index)
    addInstruction(index, instruction)
}

fun MutableMethod.replaceInstructions(index: Int, instructions: String): Unit =
    with(InstructionExtensions) { this@replaceInstructions.replaceInstructions(index, instructions) }

fun String.toInstructions(
    method: MutableMethod? = null,
): List<com.android.tools.smali.dexlib2.builder.BuilderInstruction> = compileSmaliInstructions(method)

fun <T> MutableMethod.getInstruction(index: Int): T =
    with(InstructionExtensions) { this@getInstruction.getInstruction<T>(index) }

fun MutableMethod.getInstruction(index: Int): Instruction =
    with(InstructionExtensions) { this@getInstruction.getInstruction(index) }

fun <T> Method.getInstruction(index: Int): T =
    with(InstructionExtensions) { this@getInstruction.getInstruction<T>(index) }

fun Method.getInstruction(index: Int): Instruction =
    with(InstructionExtensions) { this@getInstruction.getInstruction(index) }

fun MethodImplementation.getInstruction(index: Int): Instruction =
    with(InstructionExtensions) { this@getInstruction.getInstruction(index) }

val Method.instructionsOrNull: Iterable<Instruction>?
    get() = with(InstructionExtensions) { this@instructionsOrNull.instructionsOrNull }

val Method.instructions: Iterable<Instruction>
    get() = with(InstructionExtensions) { this@instructions.instructions }

val MutableMethod.instructions: List<com.android.tools.smali.dexlib2.builder.BuilderInstruction>
    get() = with(InstructionExtensions) { this@instructions.instructions }

val Instruction.reference: Reference?
    get() = (this as? ReferenceInstruction)?.reference

val Instruction.methodReference: MethodReference?
    get() = reference as? MethodReference

val Instruction.fieldReference: FieldReference?
    get() = reference as? FieldReference

val Instruction.stringReference: StringReference?
    get() = reference as? StringReference

val Instruction.typeReference: TypeReference?
    get() = reference as? TypeReference

val Instruction.wideLiteral: Long?
    get() = (this as? WideLiteralInstruction)?.wideLiteral

fun Iterable<Instruction>.anyInstruction(predicate: Instruction.() -> Boolean): Boolean =
    any { it.predicate() }
