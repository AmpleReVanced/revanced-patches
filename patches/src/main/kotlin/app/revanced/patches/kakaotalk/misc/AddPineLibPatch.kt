package app.revanced.patches.kakaotalk.misc

import app.revanced.patcher.patch.resourcePatch
import java.io.File

@Suppress("unused")
val addPineLibPatch = resourcePatch(
    name = "Add Pine Lib",
    description = "add libpine.so to the app's native libraries",
    use = false
) {
    compatibleWith("com.kakao.talk")

    execute {
        val targetArm32Lib = get("root/lib/armeabi-v7a/")
        val targetArm64Lib = get("root/lib/arm64-v8a/")

        val pineLib32 =
            ::javaClass.javaClass.classLoader.getResourceAsStream("kakaotalk/libpine/armeabi-v7a/libpine.so")?.readAllBytes()
                ?: error("Failed to load libpine.so for armeabi-v7a")
        val pineLib64 =
            ::javaClass.javaClass.classLoader.getResourceAsStream("kakaotalk/libpine/arm64-v8a/libpine.so")?.readAllBytes()
                ?: error("Failed to load libpine.so for arm64-v8a")

        File(targetArm32Lib, "libpine.so").apply {
            writeBytes(pineLib32)
        }
        File(targetArm64Lib, "libpine.so").apply {
            writeBytes(pineLib64)
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