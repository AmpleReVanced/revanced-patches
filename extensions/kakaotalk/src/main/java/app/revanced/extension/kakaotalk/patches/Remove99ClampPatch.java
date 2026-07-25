package app.revanced.extension.kakaotalk.patches;

@SuppressWarnings("unused")
public final class Remove99ClampPatch {
    private Remove99ClampPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
