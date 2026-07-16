// Compatibility bridge for RIVanced patcher extension helpers.
// Source: https://github.com/RIVanced/rivanced-patcher
package app.rivanced.patcher.extensions

import app.morphe.patcher.util.smali.ExternalLabel as MorpheExternalLabel
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

typealias ExternalLabel = MorpheExternalLabel

val Instruction.string: String?
    get() = ((this as? ReferenceInstruction)?.reference as? StringReference)?.string

