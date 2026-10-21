package app.revanced.extension.kakaotalk.patches;

@SuppressWarnings("unused")
public final class BypassMoatCheckPatch {
    private BypassMoatCheckPatch() {
    }

    public static String getSecurityInfo(Enum<?> type) {
        return switch (type.name()) {
            case "WIFI" -> "{\"is_connected\":false,\"ssid\":\"\",\"signal_level\":0,\"security_level\":\"\"}";
            case "UNKNOWN_SOURCE_PACKAGES" -> "{\"packages\":[]}";
            case "OS_VERSION_SUPPORT" -> "{\"is_supported\":true}";
            default -> "{}";
        };
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
