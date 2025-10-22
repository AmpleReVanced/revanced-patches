package app.revanced.extension.kakaotalk.feature;

import java.util.HashMap;
import java.util.Map;

public class Flag {

    private static final Map<String, Boolean> flags = new HashMap<>();

    static {
        flags.put("chatgpt_for_kakao_disabled", false);
        flags.put("normal_chat_room_comment_disabled", false);
        flags.put("open_chat_room_comment_disabled", false);
    }

    public static boolean canIntercept(String key) {
        return flags.containsKey(key);
    }

    public static boolean intercept(String key) {
        return Boolean.TRUE.equals(flags.getOrDefault(key, false));
    }

}
