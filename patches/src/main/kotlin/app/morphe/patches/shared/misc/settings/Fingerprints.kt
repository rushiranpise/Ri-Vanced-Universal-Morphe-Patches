package app.morphe.patches.shared.misc.settings

import app.morphe.patcher.accessFlags
import app.morphe.patcher.firstMethodDeclaratively
import app.morphe.patcher.name
import app.morphe.patcher.parameterTypes
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef

context(_: BytecodePatchContext)
internal fun ClassDef.getThemeLightColorResourceNameMethod() = firstMethodDeclaratively {
    name("getThemeLightColorResourceName")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Ljava/lang/String;")
    parameterTypes()
}

context(_: BytecodePatchContext)
internal fun ClassDef.getThemeDarkColorResourceNameMethod() = firstMethodDeclaratively {
    name("getThemeDarkColorResourceName")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.STATIC)
    returnType("Ljava/lang/String;")
    parameterTypes()
}
