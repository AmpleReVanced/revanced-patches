package app.revanced.extension.samsungkeyboard;

import android.content.Context;
import android.net.Uri;

import com.samsung.android.content.clipboard.SemClipboardManager;

public final class ClipboardCompat {
    private static final String SEM_CLIPBOARD = "semclipboard";
    private static volatile boolean localClipboard;

    private ClipboardCompat() {
    }

    public static Object getSystemService(Context context, String name) {
        Object service = context.getSystemService(name);
        if (!SEM_CLIPBOARD.equals(name)) return service;
        localClipboard = service == null;
        return localClipboard ? SemClipboardManager.getInstance(context) : service;
    }

    public static Uri getLocalClipboardUri(Uri uri, int userId) {
        Context context = SettingsStore.getContext();
        if (!localClipboard || context == null || uri == null || userId != 0 ||
                !"content".equals(uri.getScheme())) return null;
        String authority = context.getPackageName() + ".provider.RichcontentProvider";
        if (!authority.equals(uri.getHost()) || uri.getPathSegments().isEmpty() ||
                !"clipboard".equals(uri.getPathSegments().get(0))) return null;
        String userInfo = uri.getUserInfo();
        if (userInfo == null) return uri;
        return "0".equals(userInfo) ? uri.buildUpon().authority(authority).build() : null;
    }
}