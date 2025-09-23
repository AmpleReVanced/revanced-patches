package app.revanced.patches.kakaotalk.changemodel.fingerprints

import app.revanced.patcher.fingerprint

// 25.8.0 patch completed
@Suppress("unused")
internal val changeModelFingerprint = fingerprint {
    strings("<this>", "MODEL", "\\s", "-", "US", "toUpperCase(...)")
    custom {
            _, classDef ->
        classDef.methods.indexOf(classDef.methods.last()) >= 2
    }
}