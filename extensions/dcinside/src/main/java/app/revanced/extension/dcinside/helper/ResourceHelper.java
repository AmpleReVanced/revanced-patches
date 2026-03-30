package app.revanced.extension.dcinside.helper;

import app.revanced.extension.shared.Utils;

public final class ResourceHelper {
    private ResourceHelper() {
    }

    public static int getResourceId(String defType, String name) {
        return Utils.getContext().getResources().getIdentifier(
                name,
                defType,
                Utils.getContext().getPackageName()
        );
    }
}
