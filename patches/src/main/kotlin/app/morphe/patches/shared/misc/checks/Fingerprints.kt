package app.morphe.patches.shared.misc.checks

import app.morphe.patcher.gettingFirstClassDef
import app.morphe.patcher.gettingFirstClassDefDeclaratively
import app.morphe.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.patchInfoClassDef by gettingFirstClassDef(
    "Lapp/morphe/extension/shared/checks/PatchInfo;"
)

internal val BytecodePatchContext.patchInfoBuildClassDef by gettingFirstClassDef(
    $$"Lapp/morphe/extension/shared/checks/PatchInfo$Build;"
)
