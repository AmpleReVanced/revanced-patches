package app.revanced.extension.dcinside.patches;

import java.util.ArrayList;
import java.util.List;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class PostDcconPatch {
    private PostDcconPatch() {
    }

    public static void processImages(Object document) {
        boolean block = DisableDcconLoadingPatch.isPatchIncluded() && Settings.blockPostDcconLoading();
        boolean normal = RenderBigDcconAsNormalPatch.isPatchIncluded() && Settings.renderBigDcconAsNormal();
        if (!block && !normal) return;
        for (Object image : new ArrayList<>(getImages(document))) {
            if (!Settings.isDcconUrl(attribute(image, "src"))
                    && !Settings.isDcconUrl(attribute(image, "data-src"))
                    && !Settings.isDcconUrl(attribute(image, "data-original"))) continue;
            if (block) {
                removeImage(image);
            } else if (normal && (attribute(image, "class").matches("(?s).*\\bbigdccon\\b.*")
                    || attribute(image, "width").matches("(?i)150(?:px)?"))) {
                removeClass(image, "bigdccon");
                setAttribute(image, "width", "100px");
                setAttribute(image, "height", "100px");
            }
        }
    }

    private static List<?> getImages(Object document) {
        return null;
    }

    private static String attribute(Object image, String name) {
        return "";
    }

    private static void setAttribute(Object image, String name, String value) {
    }

    private static void removeClass(Object image, String name) {
    }

    private static void removeImage(Object image) {
    }
}