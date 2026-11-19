package app.revanced.extension.kakaotalk.settings;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class AdaptiveMorpheSettingsItem {
    private static final AdaptiveMorpheSettingsItem INSTANCE = new AdaptiveMorpheSettingsItem();

    private AdaptiveMorpheSettingsItem() {
    }

    public String getKey() {
        return "main.morphe";
    }

    public int getTitleResId() {
        return ResourceHelper.getResourceId("string", "morphe_label_for_ample_settings");
    }

    public int getIconResId() {
        String icon = MorpheSettingsIconDynamicDrawable.isAppDarkMode(Utils.getContext())
                ? "morphe_settings_icon_dark" : "morphe_settings_icon_light";
        return ResourceHelper.getResourceId("drawable", icon);
    }

    public static List<?> appendTo(List<?> items) {
        List<Object> result = new ArrayList<>(items);
        result.add(INSTANCE);
        return result;
    }

    public static void open(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }
}