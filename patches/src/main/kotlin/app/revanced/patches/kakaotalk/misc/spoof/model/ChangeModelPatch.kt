package app.revanced.patches.kakaotalk.misc.spoof.model

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.revanced.patches.all.misc.build.BuildInfo
import app.revanced.patches.all.misc.build.baseSpoofBuildInfoPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val changeModelPatch = bytecodePatch(
    name = "Change model",
    description = "Changes the device model to supporting subdevice features",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    fun buildOption(key: String, title: String, field: String, default: String) = stringOption(
        key = key,
        default = default,
        title = title,
        description = "The value reported as Build.$field.",
    )

    val brand by buildOption("brand", "Brand", "BRAND", "samsung")
    val manufacturer by buildOption("manufacturer", "Manufacturer", "MANUFACTURER", "samsung")
    val model by buildOption("model", "Model", "MODEL", "SM-X710")
    val device by buildOption("device", "Device", "DEVICE", "qssi")
    val product by buildOption("product", "Product", "PRODUCT", "gts9wifixx")
    val board by buildOption("board", "Board", "BOARD", "kalama")
    val hardware by buildOption("hardware", "Hardware", "HARDWARE", "qcom")
    val cpuAbi by buildOption("cpuAbi", "CPU ABI", "CPU_ABI", "arm64-v8a")
    val socManufacturer by buildOption("socManufacturer", "SoC manufacturer", "SOC_MANUFACTURER", "QTI")
    val socModel by buildOption("socModel", "SoC model", "SOC_MODEL", "SM8550")
    val fingerprint by stringOption(
        key = "fingerprint",
        default = "samsung/gts9wifixx/qssi:14/UP1A.231005.007/X710XXU5BYA1:user/release-keys",
        title = "Fingerprint",
        description = "The value reported as Build.FINGERPRINT. " +
            "Keep the brand, product, device, build ID, build type and tags consistent with it.",
    )
    val id by buildOption("id", "Build ID", "ID", "UP1A.231005.007")
    val display by buildOption("display", "Display", "DISPLAY", "UP1A.231005.007.X710XXU5BYA1")
    val type by buildOption("type", "Build type", "TYPE", "user")
    val tags by buildOption("tags", "Tags", "TAGS", "release-keys")

    dependsOn(
        baseSpoofBuildInfoPatch {
            BuildInfo(
                brand = brand,
                manufacturer = manufacturer,
                model = model,
                device = device,
                product = product,
                board = board,
                hardware = hardware,
                cpuAbi = cpuAbi,
                socManufacturer = socManufacturer,
                socModel = socModel,
                fingerprint = fingerprint,
                id = id,
                display = display,
                type = type,
                tags = tags,
            )
        },
    )
}
