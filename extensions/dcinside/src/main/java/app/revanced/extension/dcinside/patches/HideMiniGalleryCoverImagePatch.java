package app.revanced.extension.dcinside.patches;

@SuppressWarnings("unused")
public final class HideMiniGalleryCoverImagePatch {
    private HideMiniGalleryCoverImagePatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}
