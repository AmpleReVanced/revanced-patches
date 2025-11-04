package app.revanced.patches.kakaotalk.settings.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val mainSettingItemTypeFingerprint = fingerprint {
    custom { method, classDef -> classDef.sourceFile == "MainSettingItemType.kt" && classDef.superclass == "Ljava/lang/Enum;" }
}

internal val setupSettingsItemFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Landroid/content/Context;", "Landroid/content/Intent;")
    returns("Ljava/util/List;")
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT
    )
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/activity/setting/SettingActivity\$a;" }
}