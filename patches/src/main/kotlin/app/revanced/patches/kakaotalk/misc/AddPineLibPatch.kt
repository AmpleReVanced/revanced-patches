package app.revanced.patches.kakaotalk.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import java.io.File

private const val PINE_LIB_RESOURCE_DIRECTORY = "kakaotalk/libpine"
private val PINE_LIB_ABIS = listOf("armeabi-v7a", "arm64-v8a")

@Suppress("unused")
val addPineLibPatch = resourcePatch(
    name = "Add Pine Lib",
    description = "add libpine.so to the app's native libraries",
    default = false
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val classLoader = ::javaClass.javaClass.classLoader

        PINE_LIB_ABIS.forEach { abi ->
            val resourcePath = "$PINE_LIB_RESOURCE_DIRECTORY/$abi/libpine.so"
            val pineLib = classLoader.getResourceAsStream(resourcePath)?.use { it.readAllBytes() }
                ?: throw PatchException("Failed to load libpine.so for $abi")

            File(get("lib/$abi").toString()).apply {
                mkdirs()
                File(this, "libpine.so").writeBytes(pineLib)
            }
        }

        document("AndroidManifest.xml").use { document ->
            val applicationElement = document.getElementsByTagName("application").item(0)
            applicationElement?.attributes?.getNamedItem("android:extractNativeLibs")?.let {
                it.nodeValue = "true"
            } ?: run {
                val attr = document.createAttribute("android:extractNativeLibs")
                attr.value = "true"
                applicationElement?.attributes?.setNamedItem(attr)
            }
        }
    }
}
