package app.morphe.patches.shared.layout.branding

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.util.inputStreamFromBundledResource
import java.nio.file.Files
import java.util.logging.Logger

/**
 * Copies a branding license text file to the target apk.
 *
 * This patch must be a dependency for all patches that add RIVanced branding to the target app.
 */
internal val addBrandLicensePatch = rawResourcePatch {
    execute {
        val brandingLicenseFileName = "LICENSE_RIVANCED.TXT"

        val inputFileStream = inputStreamFromBundledResource(
            "branding-license",
            brandingLicenseFileName
        )!!

        val targetFile = get(brandingLicenseFileName, false).toPath()

        if (Files.exists(targetFile)) Logger.getLogger(this::class.java.name)
            .warning("Already patched by RIVanced")
        else Files.copy(inputFileStream, targetFile)
    }
}
