package app.revanced.extension.samsungkeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class ToolbarCompat {
    private static final String GIF = "com.samsung.android.icecone.gif";
    private static final String HIDDEN_ITEMS = "icecone_hidden_list";
    private static final String MIGRATION = "gif_toolbar_enabled_2";
    private static volatile SharedPreferences toolbarItems;

    private ToolbarCompat() {
    }

    public static void initialize(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("sticker_shared_prefs", Context.MODE_PRIVATE);
        toolbarItems = preferences;
        SharedPreferences settings = context.getSharedPreferences("revanced_toolbar", Context.MODE_PRIVATE);
        if (settings.getBoolean(MIGRATION, false)) return;

        String hiddenItems = preferences.getString(HIDDEN_ITEMS, "");
        if (hiddenItems != null && !hiddenItems.isEmpty()) {
            String visibleItems = Arrays.stream(hiddenItems.split("\\|"))
                    .filter(item -> !GIF.equals(item))
                    .collect(Collectors.joining("|"));
            preferences.edit().putString(HIDDEN_ITEMS, visibleItems).apply();
        }
        settings.edit().putBoolean(MIGRATION, true).apply();
    }

    public static int gifVisibility(int visibility) {
        SharedPreferences preferences = toolbarItems;
        if (preferences == null) return visibility;

        String hiddenItems = preferences.getString(HIDDEN_ITEMS, "");
        if (hiddenItems == null || hiddenItems.isEmpty()) return 0;
        return Arrays.asList(hiddenItems.split("\\|")).contains(GIF) ? visibility : 0;
    }
}