package com.samsung.android.content.clipboard;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.PersistableBundle;
import android.text.Html;

import com.samsung.android.content.clipboard.data.SemClipData;
import com.samsung.android.content.clipboard.data.SemHtmlClipData;
import com.samsung.android.content.clipboard.data.SemImageClipData;
import com.samsung.android.content.clipboard.data.SemTextClipData;

import java.util.concurrent.CopyOnWriteArrayList;

import app.revanced.extension.samsungkeyboard.WindowCompat;

public final class SemClipboardManager {
    @SuppressLint("StaticFieldLeak")
    private static volatile SemClipboardManager instance;

    private final Context context;
    private final ClipboardManager clipboard;
    private final CopyOnWriteArrayList<SemClipboardEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile String lastClip;

    public SemClipboardManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext == null ? context : applicationContext;
        clipboard = this.context.getSystemService(ClipboardManager.class);
        if (clipboard != null) {
            clipboard.addPrimaryClipChangedListener(this::capturePrimaryClip);
            new Handler(Looper.getMainLooper()).post(this::capturePrimaryClip);
        }
    }

    public static SemClipboardManager getInstance(Context context) {
        SemClipboardManager manager = instance;
        if (manager != null) return manager;
        synchronized (SemClipboardManager.class) {
            manager = instance;
            if (manager == null) instance = manager = new SemClipboardManager(context);
        }
        return manager;
    }

    public boolean isEnabled() {
        return clipboard != null;
    }

    public boolean paste(ClipData data) {
        if (data == null) return false;
        if (WindowCompat.commitClipboard(data)) return true;
        if (clipboard == null) return false;
        try {
            clipboard.setPrimaryClip(data);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public boolean paste(String text) {
        return paste(ClipData.newPlainText(null, text));
    }

    public void registerClipboardEventListener(SemClipboardEventListener listener) {
        if (listener == null) return;
        listeners.addIfAbsent(listener);
        capturePrimaryClip();
    }

    public void unregisterClipboardEventListener(SemClipboardEventListener listener) {
        listeners.remove(listener);
    }

    private void capturePrimaryClip() {
        if (clipboard == null) return;
        try {
            ClipData data = clipboard.getPrimaryClip();
            if (data == null || data.getItemCount() == 0 || isSensitive(data.getDescription())) return;

            ClipData.Item item = data.getItemAt(0);
            ClipDescription description = data.getDescription();
            String signature = signature(description, item);
            if (signature.equals(lastClip)) return;

            if (description.hasMimeType("image/*") && item.getUri() != null) {
                insertImage(description, item.getUri());
            } else {
                insertText(description, item);
            }
            lastClip = signature;

            SemClipData semData = toSemClipData(description, item, data);
            for (SemClipboardEventListener listener : listeners) {
                listener.onClipboardUpdated(1, semData);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void insertText(ClipDescription description, ClipData.Item item) {
        String html = item.getHtmlText();
        CharSequence value = item.getText();
        if (value == null) value = item.coerceToText(context);
        if (value == null && html != null) value = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        if (value == null) return;

        ContentValues values = clipValues(description);
        values.put("clip_text", value.toString());
        values.put("clip_html", html);
        context.getContentResolver().insert(clipboardUri(), values);
    }

    private void insertImage(ClipDescription description, Uri uri) {
        ContentValues values = clipValues(description);
        values.put("clip_uri", uri.toString());
        context.getContentResolver().insert(clipboardUri(), values);
    }

    private static ContentValues clipValues(ClipDescription description) {
        ContentValues values = new ContentValues();
        values.put("time_stamp", System.currentTimeMillis());
        values.put("caller_app_uid", Process.myUid());
        values.put("user_id", Process.myUid() / 100000);
        values.put("locked", false);
        CharSequence label = description.getLabel();
        values.put("clip_label", label == null ? "" : label.toString());
        values.put("clip_mimetypes", mimeTypes(description));
        return values;
    }

    private Uri clipboardUri() {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".provider.RichcontentProvider")
                .appendPath("clipboard")
                .build();
    }

    private static SemClipData toSemClipData(
            ClipDescription description,
            ClipData.Item item,
            ClipData data
    ) {
        String html = item.getHtmlText();
        if (html != null) {
            SemHtmlClipData result = new SemHtmlClipData();
            result.setHtml(html);
            return result;
        }
        if (description.hasMimeType("image/*") && item.getUri() != null) {
            SemImageClipData result = new SemImageClipData();
            result.setClipData(data);
            return result;
        }
        SemTextClipData result = new SemTextClipData();
        result.setText(item.getText());
        return result;
    }

    private static boolean isSensitive(ClipDescription description) {
        PersistableBundle extras = description.getExtras();
        return extras != null && extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
    }

    private static String mimeTypes(ClipDescription description) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < description.getMimeTypeCount(); i++) {
            if (i != 0) result.append(',');
            result.append(description.getMimeType(i));
        }
        return result.toString();
    }

    private static String signature(ClipDescription description, ClipData.Item item) {
        return description.getTimestamp() + "\n" + item.getText() + "\n" + item.getHtmlText() + "\n" + item.getUri();
    }
}