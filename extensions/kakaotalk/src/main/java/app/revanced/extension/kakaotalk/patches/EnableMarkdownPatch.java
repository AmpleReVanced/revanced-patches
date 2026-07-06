package app.revanced.extension.kakaotalk.patches;

import org.json.JSONObject;

import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class EnableMarkdownPatch {
    private EnableMarkdownPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static void enableMarkdown(JSONObject jsonObject, CharSequence inputText) {
        if (!Settings.enableMarkdown() || jsonObject == null || isBlank(inputText)) return;

        try {
            jsonObject.put("markdown", true);
        } catch (Exception ignored) {
        }
    }

    private static boolean isBlank(CharSequence inputText) {
        if (inputText == null) return true;

        for (int i = 0; i < inputText.length(); i++) {
            if (!Character.isWhitespace(inputText.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
