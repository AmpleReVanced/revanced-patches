package app.revanced.extension.dcinside.patches;

import android.view.View;

import app.revanced.extension.dcinside.NotificationClickListener;
import app.revanced.extension.dcinside.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class NotificationShortcutPatch {
    private static final String SHORTCUT_ID = "morphe_dcinside_post_list_quick_notifi";

    private NotificationShortcutPatch() {
    }

    public static void bind(View root) {
        View quickWrite = root.findViewById(ResourceHelper.getResourceId("id", "post_list_quick_write"));
        View shortcut = root.findViewById(ResourceHelper.getResourceId("id", SHORTCUT_ID));
        if (quickWrite == null || shortcut == null) return;
        shortcut.setVisibility(quickWrite.getVisibility());
        shortcut.setOnClickListener(new NotificationClickListener(root.getContext()));
    }

    public static void setQuickWriteVisibility(View quickWrite, int visibility) {
        quickWrite.setVisibility(visibility);
        if (!(quickWrite.getParent() instanceof View)) return;
        View shortcut = ((View) quickWrite.getParent()).findViewById(ResourceHelper.getResourceId("id", SHORTCUT_ID));
        if (shortcut != null) shortcut.setVisibility(visibility);
    }
}