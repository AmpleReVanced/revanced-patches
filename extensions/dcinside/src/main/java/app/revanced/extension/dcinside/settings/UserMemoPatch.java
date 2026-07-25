package app.revanced.extension.dcinside.settings;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.dcinside.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class UserMemoPatch {
    private static final int MAX_MEMO_LENGTH = 15;
    private static final String GLOBAL_GALLERY_ID = "";
    private static final Preset[] PRESETS = {
            new Preset(
                    "user_memo_preset_1",
                    "morphe_settings_user_memo_preset_classic_title",
                    "Preset 1: Classic IP list",
                    "morphe_settings_user_memo_preset_classic_description",
                    "Traditional IP list. Conservative and stable."
            ),
            new Preset(
                    "user_memo_preset_2",
                    "morphe_settings_user_memo_preset_enhanced_title",
                    "Preset 2: Enhanced IP list",
                    "morphe_settings_user_memo_preset_enhanced_description",
                    "Slightly improved list with broader coverage. Experimental."
            ),
    };


    private UserMemoPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static Preset[] getPresets() {
        return PRESETS.clone();
    }

    public static void registerUserMemoPreset(Context context) {
        registerUserMemoPreset(context, PRESETS[0]);
    }

    public static void registerUserMemoPreset(Context context, Preset preset) {
        if (context == null) {
            return;
        }
        if (preset == null) {
            preset = PRESETS[0];
        }

        Context applicationContext = context.getApplicationContext();
        Utils.setContext(applicationContext == null ? context : applicationContext);

        List<MemoEntry> entries = parsePreset(getPreset(context, preset));
        if (entries.isEmpty()) {
            toast(context, "morphe_settings_user_memo_preset_empty", "The bundled user memo preset is empty.");
            return;
        }

        if (registerEntries(entries)) {
            toastRegistered(context, entries.size());
        } else {
            toast(context, "morphe_settings_user_memo_preset_failed", "Could not register the user memo preset.");
        }
    }

    public static void clearUserMemos(Context context) {
        if (context == null) {
            return;
        }

        Context applicationContext = context.getApplicationContext();
        Utils.setContext(applicationContext == null ? context : applicationContext);

        Object realm = openDefaultRealm(null);
        if (realm == null) {
            toast(context, "morphe_settings_user_memo_clear_failed", "Could not clear user memos.");
            return;
        }

        try {
            if (clearUserMemosWithApp(realm, null, null)) {
                toast(context, "morphe_settings_user_memo_clear_done", "User memos were cleared.");
            } else {
                toast(context, "morphe_settings_user_memo_clear_failed", "Could not clear user memos.");
            }
        } catch (Exception ignored) {
            toast(context, "morphe_settings_user_memo_clear_failed", "Could not clear user memos.");
        } finally {
            closeRealm(realm);
        }
    }

    private static boolean registerEntries(List<MemoEntry> entries) {
        Object realm = openDefaultRealm(null);
        if (realm == null) {
            return false;
        }

        try {
            Object pairArray = newMemoPairArray(entries.size(), null);
            for (int i = 0; i < entries.size(); i++) {
                MemoEntry entry = entries.get(i);
                Array.set(pairArray, i, newMemoPair(entry.userValue, entry.memo, null));
            }
            return registerEntriesWithApp(realm, pairArray, GLOBAL_GALLERY_ID, null, false);
        } catch (Exception ignored) {
            return false;
        } finally {
            closeRealm(realm);
        }
    }

    private static String getPreset(Context context, Preset preset) {
        String content = preset.cachedContent;
        if (content != null) {
            return content;
        }

        content = readPreset(context, preset.resourceName);
        preset.cachedContent = content;
        return content;
    }

    private static String readPreset(Context context, String resourceName) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier(
                resourceName,
                "raw",
                context.getPackageName()
        );
        if (resourceId == 0) {
            return "";
        }

        try (InputStream input = resources.openRawResource(resourceId);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            String value = output.toString("UTF-8")
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .trim();
            return value.startsWith("\uFEFF") ? value.substring(1) : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static List<MemoEntry> parsePreset(String preset) {
        ArrayList<MemoEntry> entries = new ArrayList<>();
        if (preset == null || preset.length() == 0) {
            return entries;
        }

        String[] lines = preset.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }

            int separatorIndex = trimmed.indexOf('-');
            if (separatorIndex < 0 || separatorIndex != trimmed.lastIndexOf('-')) {
                continue;
            }

            String userValue = trimmed.substring(0, separatorIndex);
            String memo = trimmed.substring(separatorIndex + 1);
            if (memo.length() > MAX_MEMO_LENGTH) {
                memo = memo.substring(0, MAX_MEMO_LENGTH);
            }
            entries.add(new MemoEntry(userValue, memo));
        }
        return entries;
    }

    private static void closeRealm(Object realm) {
        if (realm instanceof Closeable) {
            try {
                ((Closeable) realm).close();
            } catch (Exception ignored) {
            }
        }
    }

    private static Object openDefaultRealm(Object unused) {
        return null;
    }

    private static Object newMemoPairArray(int size, Object unused) {
        return null;
    }

    private static Object newMemoPair(String userValue, String memo, Object unused) {
        return null;
    }

    private static boolean registerEntriesWithApp(
            Object realm,
            Object pairArray,
            String galleryId,
            String color,
            boolean applySameMemoColor
    ) {
        return false;
    }

    private static boolean clearUserMemosWithApp(Object realm, Object unused1, Object unused2) {
        try {
            beginUserMemoTransaction(realm, null);
            deleteRealmResults(queryAllUserMemoEntries(realm, null));
            deleteRealmResults(queryAllUserMemoContainers(realm, null));
            deleteRealmResults(queryAllUserMemoCounts(realm, null));
            commitUserMemoTransaction(realm, null);
            reloadUserMemoCache(realm, null);
            return true;
        } catch (Exception ignored) {
            rollbackUserMemoTransaction(realm);
            return false;
        }
    }

    private static void rollbackUserMemoTransaction(Object realm) {
        try {
            if (isUserMemoTransactionActive(realm, null)) {
                cancelUserMemoTransaction(realm, null);
            }
        } catch (Exception ignored) {
        }
    }

    private static void beginUserMemoTransaction(Object realm, Object unused) {
    }

    private static Object queryAllUserMemoEntries(Object realm, Object unused) {
        return null;
    }

    private static Object queryAllUserMemoContainers(Object realm, Object unused) {
        return null;
    }

    private static Object queryAllUserMemoCounts(Object realm, Object unused) {
        return null;
    }

    private static void commitUserMemoTransaction(Object realm, Object unused) {
    }

    private static boolean isUserMemoTransactionActive(Object realm, Object unused) {
        return false;
    }

    private static void cancelUserMemoTransaction(Object realm, Object unused) {
    }

    private static void reloadUserMemoCache(Object realm, Object unused) {
    }

    private static int deleteRealmResults(Object results) {
        if (!(results instanceof Iterable)) {
            return 0;
        }

        ArrayList<Object> objects = new ArrayList<>();
        for (Object object : (Iterable<?>) results) {
            if (object != null) {
                objects.add(object);
            }
        }

        for (Object object : objects) {
            deleteRealmObject(object, null);
        }
        return objects.size();
    }

    private static void deleteRealmObject(Object object, Object unused) {
    }

    private static void toastRegistered(Context context, int count) {
        int stringId = ResourceHelper.getResourceId("string", "morphe_settings_user_memo_preset_registered");
        String message = stringId == 0
                ? "Registered " + count + " user memo preset entries."
                : context.getString(stringId, count);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private static void toast(Context context, String stringName, String fallback) {
        int stringId = ResourceHelper.getResourceId("string", stringName);
        String message = stringId == 0 ? fallback : context.getString(stringId);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static final class Preset {
        private final String resourceName;
        private final String titleStringName;
        private final String fallbackTitle;
        private final String descriptionStringName;
        private final String fallbackDescription;
        private volatile String cachedContent;

        private Preset(
                String resourceName,
                String titleStringName,
                String fallbackTitle,
                String descriptionStringName,
                String fallbackDescription
        ) {
            this.resourceName = resourceName;
            this.titleStringName = titleStringName;
            this.fallbackTitle = fallbackTitle;
            this.descriptionStringName = descriptionStringName;
            this.fallbackDescription = fallbackDescription;
        }

        public String getTitle(Context context) {
            return getString(context, titleStringName, fallbackTitle);
        }

        public String getDescription(Context context) {
            return getString(context, descriptionStringName, fallbackDescription);
        }

        private static String getString(Context context, String stringName, String fallback) {
            int stringId = ResourceHelper.getResourceId("string", stringName);
            return stringId == 0 ? fallback : context.getString(stringId);
        }
    }

    private static final class MemoEntry {
        private final String userValue;
        private final String memo;

        private MemoEntry(String userValue, String memo) {
            this.userValue = userValue;
            this.memo = memo;
        }
    }
}
