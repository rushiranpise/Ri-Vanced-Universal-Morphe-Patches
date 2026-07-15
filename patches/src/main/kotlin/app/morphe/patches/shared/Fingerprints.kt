package app.morphe.patches.shared

import app.morphe.patcher.accessFlags
import app.morphe.patcher.composingFirstMethod
import app.morphe.patcher.gettingFirstMethodDeclaratively
import app.morphe.patcher.instructions
import app.morphe.patcher.invoke
import app.morphe.patcher.parameterTypes
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.castContextFetchMethod by gettingFirstMethodDeclaratively(
    "Error fetching CastContext."
)

internal val BytecodePatchContext.primeMethod by gettingFirstMethodDeclaratively(
    "com.android.vending",
    "com.google.android.GoogleCamera"
)

// Flag is present in YouTube 20.23, but bold icons are missing and forcing them crashes the app.
// 20.31 is the first target with all the bold icons present.
internal val BytecodePatchContext.boldIconsFeatureFlagMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    instructions(
        45685201L(),
    )
}
