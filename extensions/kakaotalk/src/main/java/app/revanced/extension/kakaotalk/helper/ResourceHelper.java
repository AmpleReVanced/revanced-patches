package app.revanced.extension.kakaotalk.helper;

import app.revanced.extension.shared.Utils;

public class ResourceHelper {

    public static int getResourceId(String defType, String name) {
        // TODO: To prevent issues when cloning, you must change the package name in revanced-patcher along with the one in arsc.
        return Utils.getContext().getResources().getIdentifier(name, defType, "com.kakao.talk");
    }

}
