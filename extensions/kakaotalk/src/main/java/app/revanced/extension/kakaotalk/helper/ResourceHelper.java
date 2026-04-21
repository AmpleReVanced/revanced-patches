package app.revanced.extension.kakaotalk.helper;

import app.morphe.extension.shared.Utils;

public class ResourceHelper {

    public static int getResourceId(String defType, String name) {
        return Utils.getContext().getResources().getIdentifier(name, defType, Utils.getContext().getPackageName());
    }

}