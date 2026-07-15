package app.morphe.patches.shared.misc.debugging

import app.morphe.patcher.ClassDefComposing
import app.morphe.patcher.gettingFirstImmutableMethodDeclaratively
import app.morphe.patcher.firstMethodDeclaratively
import app.morphe.patcher.accessFlags
import app.morphe.patcher.custom
import app.morphe.patcher.parameterTypes
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.returnType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef

internal val BytecodePatchContext.experimentalFeatureFlagUtilMethod by gettingFirstImmutableMethodDeclaratively(
    "Unable to parse proto typed experiment flag: "
) {
    returnType("L")
    custom {
        // 'public static' or 'public static final'
        AccessFlags.STATIC.isSet(accessFlags)
                && AccessFlags.PUBLIC.isSet(accessFlags)
                // "L", "J", "[B" or "L", "J"
                && parameters.let { (it.size == 2 || it.size == 3) && it[1].type == "J" }
    }
}

internal val ClassDef.experimentalBooleanFeatureFlagMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("Z")
    parameterTypes("L", "J", "Z")
    custom {
        // 'public static' or 'public static final'
        AccessFlags.STATIC.isSet(accessFlags) && AccessFlags.PUBLIC.isSet(accessFlags)
    }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalDoubleFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("D")
    parameterTypes("L", "J", "D")
    custom { AccessFlags.STATIC.isSet(accessFlags) }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalLongFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("J")
    parameterTypes("L", "J", "J")
    custom { AccessFlags.STATIC.isSet(accessFlags) }
}

context(_: BytecodePatchContext)
internal fun ClassDef.getExperimentalStringFeatureFlagMethod() = firstMethodDeclaratively {
    returnType("Ljava/lang/String;")
    parameterTypes("L", "J", "Ljava/lang/String;")
    custom { AccessFlags.STATIC.isSet(accessFlags) }
}
