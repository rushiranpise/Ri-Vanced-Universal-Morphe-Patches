package app.morphe.patches.shared.misc.fix.verticalscroll

import app.morphe.patcher.*
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.canScrollVerticallyMethodMatch by composingFirstMethod {
    definingClass("SwipeRefreshLayout;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    opcodes(
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    )
}
