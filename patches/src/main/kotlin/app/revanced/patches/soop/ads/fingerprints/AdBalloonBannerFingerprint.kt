package app.revanced.patches.soop.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

internal object AdBalloonBannerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(resourceLiteral(ResourceType.STRING, "ad_balloon_of")),
)

context(_: BytecodePatchContext)
internal fun adBalloonBannerContainerFingerprint(): Fingerprint {
    val banner = AdBalloonBannerFingerprint.originalMethod
    return Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
        returnType = "V",
        filters = listOf(methodCall(banner.toString())),
        custom = { method, _ -> method.parameterTypes.firstOrNull() == banner.parameterTypes.first() },
    )
}