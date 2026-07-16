package app.morphe.patches.shared.misc.extension

import app.morphe.patcher.*
import app.morphe.patcher.gettingFirstMethodDeclaratively
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.getPatchesReleaseVersionMethod by gettingFirstMethodDeclaratively {
    name("getPatchesReleaseVersion")
    definingClass(EXTENSION_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Ljava/lang/String;")
    parameterTypes()
}
