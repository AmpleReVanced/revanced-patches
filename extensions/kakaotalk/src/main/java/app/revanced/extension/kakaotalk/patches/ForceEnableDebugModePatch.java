package app.revanced.extension.kakaotalk.patches;

@SuppressWarnings("unused")
public final class ForceEnableDebugModePatch {
    private ForceEnableDebugModePatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
