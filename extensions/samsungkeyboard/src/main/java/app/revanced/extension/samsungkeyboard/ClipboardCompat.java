package app.revanced.extension.samsungkeyboard;

import android.content.Context;

import com.samsung.android.content.clipboard.SemClipboardManager;

public final class ClipboardCompat {
    private static final String SEM_CLIPBOARD = "semclipboard";

    private ClipboardCompat() {
    }

    public static Object getSystemService(Context context, String name) {
        Object service = context.getSystemService(name);
        if (service != null || !SEM_CLIPBOARD.equals(name)) return service;
        return SemClipboardManager.getInstance(context);
    }
}