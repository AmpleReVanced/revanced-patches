package app.revanced.patches.kakaotalk.changemodel.fingerprints

import app.revanced.patcher.fingerprint

@Suppress("unused")
internal val changeModelFingerprint = fingerprint {
    strings("<this>", "MODEL", "\\s", "-", "US", "toUpperCase(...)")
    custom {
            _, classDef ->
        classDef.methods.indexOf(classDef.methods.last()) >= 2
    }
}