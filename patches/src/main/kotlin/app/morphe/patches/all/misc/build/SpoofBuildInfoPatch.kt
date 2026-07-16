package app.morphe.patches.all.misc.build

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.longOption
import app.morphe.patcher.patch.stringOption

@Suppress("unused")
val spoofBuildInfoPatch = bytecodePatch(
    name = "Spoof build info",
    description = "Spoofs the information about the current build.",
    default = false,
) {
    val board by stringOption(
        default = null,
        key = "Board",
        description = "The name of the underlying board, like \"goldfish\".",
    )

    val bootloader by stringOption(
        default = null,
        key = "Bootloader",
        description = "The system bootloader version number.",
    )

    val brand by stringOption(
        default = null,
        key = "Brand",
        description = "The consumer-visible brand with which the product/hardware will be associated, if any.",
    )

    val cpuAbi by stringOption(
        default = null,
        key = "CPU ABI",
        description = "This field was deprecated in API level 21. Use SUPPORTED_ABIS instead.",
    )

    val cpuAbi2 by stringOption(
        default = null,
        key = "CPU ABI 2",
        description = "This field was deprecated in API level 21. Use SUPPORTED_ABIS instead.",
    )

    val device by stringOption(
        default = null,
        key = "Device",
        description = "The name of the industrial design.",
    )

    val display by stringOption(
        default = null,
        key = "Display",
        description = "A build ID string meant for displaying to the user.",
    )

    val fingerprint by stringOption(
        default = null,
        key = "Fingerprint",
        description = "A string that uniquely identifies this build.",
    )

    val hardware by stringOption(
        default = null,
        key = "Hardware",
        description = "The name of the hardware (from the kernel command line or /proc).",
    )

    val host by stringOption(
        default = null,
        key = "Host",
        description = "The host.",
    )

    val id by stringOption(
        default = null,
        key = "ID",
        description = "Either a changelist number, or a label like \"M4-rc20\".",
    )

    val manufacturer by stringOption(
        default = null,
        key = "Manufacturer",
        description = "The manufacturer of the product/hardware.",
    )

    val model by stringOption(
        default = null,
        key = "Model",
        description = "The end-user-visible name for the end product.",
    )

    val odmSku by stringOption(
        default = null,
        key = "ODM SKU",
        description = "The SKU of the device as set by the original design manufacturer (ODM).",
    )

    val product by stringOption(
        default = null,
        key = "Product",
        description = "The name of the overall product.",
    )

    val radio by stringOption(
        default = null,
        key = "Radio",
        description = "This field was deprecated in API level 15. " +
            "The radio firmware version is frequently not available when this class is initialized, " +
            "leading to a blank or \"unknown\" value for this string. Use getRadioVersion() instead.",
    )

    val serial by stringOption(
        default = null,
        key = "Serial",
        description = "This field was deprecated in API level 26. Use getSerial() instead.",
    )

    val sku by stringOption(
        default = null,
        key = "SKU",
        description = "The SKU of the hardware (from the kernel command line).",
    )

    val socManufacturer by stringOption(
        default = null,
        key = "SOC manufacturer",
        description = "The manufacturer of the device's primary system-on-chip.",
    )

    val socModel by stringOption(
        default = null,
        key = "SOC model",
        description = "The model name of the device's primary system-on-chip.",
    )

    val tags by stringOption(
        default = null,
        key = "Tags",
        description = "Comma-separated tags describing the build, like \"unsigned,debug\".",
    )

    val time by longOption(
        default = null,
        key = "Time",
        description = "The time at which the build was produced, given in milliseconds since the UNIX epoch.",
    )

    val type by stringOption(
        default = null,
        key = "Type",
        description = "The type of build, like \"user\" or \"eng\".",
    )

    val user by stringOption(
        default = null,
        key = "User",
        description = "The user.",
    )

    dependsOn(
        baseSpoofBuildInfoPatch {
            BuildInfo(
                board,
                bootloader,
                brand,
                cpuAbi,
                cpuAbi2,
                device,
                display,
                fingerprint,
                hardware,
                host,
                id,
                manufacturer,
                model,
                odmSku,
                product,
                radio,
                serial,
                sku,
                socManufacturer,
                socModel,
                tags,
                time,
                type,
                user,
            )
        },
    )
}
