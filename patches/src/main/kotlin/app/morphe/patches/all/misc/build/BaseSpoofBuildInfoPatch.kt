package app.morphe.patches.all.misc.build

import app.morphe.patcher.extensions.fieldReference
import app.morphe.patcher.extensions.getInstruction
import app.morphe.patcher.extensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BUILD_CLASS_DESCRIPTOR = "Landroid/os/Build;"

private fun String.toSmaliStringLiteral() = buildString {
    append('"')
    this@toSmaliStringLiteral.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

class BuildInfo(
    // The build information supported32BitAbis, supported64BitAbis, and supportedAbis are not supported for now,
    // because initializing an array in transform is a bit more complex.
    val board: String? = null,
    val bootloader: String? = null,
    val brand: String? = null,
    val cpuAbi: String? = null,
    val cpuAbi2: String? = null,
    val device: String? = null,
    val display: String? = null,
    val fingerprint: String? = null,
    val hardware: String? = null,
    val host: String? = null,
    val id: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val odmSku: String? = null,
    val product: String? = null,
    val radio: String? = null,
    val serial: String? = null,
    val sku: String? = null,
    val socManufacturer: String? = null,
    val socModel: String? = null,
    val tags: String? = null,
    val time: Long? = null,
    val type: String? = null,
    val user: String? = null,
)

fun baseSpoofBuildInfoPatch(buildInfoSupplier: () -> BuildInfo) = bytecodePatch {
    // Lazy, so that patch options above are initialized before they are accessed.
    val replacements by lazy {
        with(buildInfoSupplier()) {
            buildMap {
                if (board != null) put("BOARD", "const-string" to board.toSmaliStringLiteral())
                if (bootloader != null) put("BOOTLOADER", "const-string" to bootloader.toSmaliStringLiteral())
                if (brand != null) put("BRAND", "const-string" to brand.toSmaliStringLiteral())
                if (cpuAbi != null) put("CPU_ABI", "const-string" to cpuAbi.toSmaliStringLiteral())
                if (cpuAbi2 != null) put("CPU_ABI2", "const-string" to cpuAbi2.toSmaliStringLiteral())
                if (device != null) put("DEVICE", "const-string" to device.toSmaliStringLiteral())
                if (display != null) put("DISPLAY", "const-string" to display.toSmaliStringLiteral())
                if (fingerprint != null) put("FINGERPRINT", "const-string" to fingerprint.toSmaliStringLiteral())
                if (hardware != null) put("HARDWARE", "const-string" to hardware.toSmaliStringLiteral())
                if (host != null) put("HOST", "const-string" to host.toSmaliStringLiteral())
                if (id != null) put("ID", "const-string" to id.toSmaliStringLiteral())
                if (manufacturer != null) put("MANUFACTURER", "const-string" to manufacturer.toSmaliStringLiteral())
                if (model != null) put("MODEL", "const-string" to model.toSmaliStringLiteral())
                if (odmSku != null) put("ODM_SKU", "const-string" to odmSku.toSmaliStringLiteral())
                if (product != null) put("PRODUCT", "const-string" to product.toSmaliStringLiteral())
                if (radio != null) put("RADIO", "const-string" to radio.toSmaliStringLiteral())
                if (serial != null) put("SERIAL", "const-string" to serial.toSmaliStringLiteral())
                if (sku != null) put("SKU", "const-string" to sku.toSmaliStringLiteral())
                if (socManufacturer != null) put("SOC_MANUFACTURER", "const-string" to socManufacturer.toSmaliStringLiteral())
                if (socModel != null) put("SOC_MODEL", "const-string" to socModel.toSmaliStringLiteral())
                if (tags != null) put("TAGS", "const-string" to tags.toSmaliStringLiteral())
                if (time != null) put("TIME", "const-wide" to "$time")
                if (type != null) put("TYPE", "const-string" to type.toSmaliStringLiteral())
                if (user != null) put("USER", "const-string" to user.toSmaliStringLiteral())
            }
        }
    }

    dependsOn(
        transformInstructionsPatch(
            filterMap = filterMap@{ _, _, instruction, instructionIndex ->
                val reference = instruction.fieldReference ?: return@filterMap null
                if (reference.definingClass != BUILD_CLASS_DESCRIPTOR) return@filterMap null

                return@filterMap replacements[reference.name]?.let { instructionIndex to it }
            },
            transform = { mutableMethod, entry ->
                val (index, replacement) = entry
                val (opcode, operand) = replacement
                val register = mutableMethod.getInstruction<OneRegisterInstruction>(index).registerA

                mutableMethod.replaceInstruction(index, "$opcode v$register, $operand")
            },
        ),
    )
}
