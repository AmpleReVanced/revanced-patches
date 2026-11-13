package app.revanced.patches.riroschool

import app.morphe.patcher.patch.Patch

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

/** Runs the standalone artifact regression suite using RIRO_TEST_APK and RIRO_TEST_MANIFEST. */
fun main() {
    val publicPatches = listOf(disableFirebaseAnalyticsPatch, disableGoogleAnalyticsPatch,
        disableRirosoftAnalyticsPatch, disableVpnDetectionPatch, disableAdbDetectionPatch, spoofGmsAvailabilityPatch)
    check(publicPatches.map { it.name }.toSet().size == 6)
    check(!spoofGmsAvailabilityPatch.default && spoofGmsAvailabilityPatch.dependencies.isEmpty())
    publicPatches.forEach { checkDependencies(it, mutableSetOf()) }
    requireRiroMetadata("com.rirosoft.riroschool", "7.5.6", "179")
    for (target in listOf(
        Triple("other.app", "7.5.6", "179"),
        Triple("com.rirosoft.riroschool", "7.5.7", "179"),
        Triple("com.rirosoft.riroschool", "7.5.6", "180"),
    )) check(runCatching { requireRiroMetadata(target.first, target.second, target.third) }.isFailure)
    runDevicePatchTests()
    runIdentityPatchTests()
    runFirebasePatchTests()
    runFirebaseFingerprintTests()
    runNativeCombinationTests()
    runGooglePatchTests()
    println("PASS: target, detector, identity, Firebase and ML Kit contracts")
}

/** Rejects null or cyclic dependencies caused by invalid static initialization order. */
private fun checkDependencies(patch: Patch<*>, active: MutableSet<Patch<*>>) {
    check(active.add(patch)) { "Cyclic patch dependency" }
    patch.dependencies.forEach { dependency -> checkDependencies(requireNotNull(dependency), active) }
    active.remove(patch)
}
