package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object VodQuickViewCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    returnType = "Z",
    strings = listOf("QUICKVIEW_PLUS"),
    custom = { _, classDef ->
        classDef.type == "Lkr/co/nowcom/mobile/afreeca/player/vod/vod/player/VodPlayerFragment;"
    },
)