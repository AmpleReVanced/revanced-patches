package app.revanced.extension.samsungkeyboard;

import android.content.res.Resources;

public final class ResourceLoader {
    private ResourceLoader() {
    }

    public static int getDimensionPixelSize(Resources resources, int id) {
        if (id == 0) return 0;

        try {
            return resources.getDimensionPixelSize(id);
        } catch (Resources.NotFoundException ignored) {
            return 0;
        }
    }
}