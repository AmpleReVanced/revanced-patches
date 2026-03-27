package app.revanced.patches.kakaotalk.changemodel.fingerprints

import app.morphe.patcher.Fingerprint

@Suppress("unused")
internal object ChangeModelFingerprint : Fingerprint(
    strings = listOf("<this>", "MODEL", "\\s", "-", "US", "toUpperCase(...)"),
    custom = { _, classDef ->
        classDef.methods.indexOf(classDef.methods.last()) >= 2
    }
)