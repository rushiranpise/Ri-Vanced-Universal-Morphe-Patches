package app.rivanced.patches.shared.misc.pairip.license

import app.rivanced.patcher.definingClass
import app.rivanced.patcher.gettingFirstMethodDeclarativelyOrNull
import app.rivanced.patcher.name
import app.rivanced.patcher.parameterTypes
import app.morphe.patcher.patch.BytecodePatchContext
import app.rivanced.patcher.returnType

internal val BytecodePatchContext.processLicenseResponseMethod by gettingFirstMethodDeclarativelyOrNull {
    name("processResponse")
    definingClass("Lcom/pairip/licensecheck/LicenseClient;")
}

internal val BytecodePatchContext.validateLicenseResponseMethod by gettingFirstMethodDeclarativelyOrNull {
    name("validateResponse")
    definingClass("Lcom/pairip/licensecheck/ResponseValidator;")
}

internal val BytecodePatchContext.checkLocalInstallerMethod by gettingFirstMethodDeclarativelyOrNull {
    name("performLocalInstallerCheck")
    definingClass("Lcom/pairip/licensecheck/LicenseClient;")
}

internal val BytecodePatchContext.licenseClientClinit by gettingFirstMethodDeclarativelyOrNull {
    name("<clinit>")
    definingClass("Lcom/pairip/licensecheck/LicenseClient;")
}

internal val BytecodePatchContext.verifyIntegrityMethod by gettingFirstMethodDeclarativelyOrNull(
    "Apk signature is invalid.",
    "Signature check ok",
) {
    parameterTypes("Landroid/content/Context;")
    returnType("V")
}

internal val BytecodePatchContext.launchVMMethod by gettingFirstMethodDeclarativelyOrNull {
    name("launch")
    definingClass("Lcom/pairip/StartupLauncher;")
    returnType("V")
}

internal val BytecodePatchContext.isDebuggingEnabledMethod by gettingFirstMethodDeclarativelyOrNull {
    name("isDebuggingEnabled")
    definingClass("Lcom/pairip/VMRunner;")
    returnType("Z")
}