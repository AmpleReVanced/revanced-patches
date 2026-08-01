package app.revanced.extension.kakaotalk.patches;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class AllowProfileMediaDownloadPatch {
    private static final String EXTRA_PROFILE_IMAGE_URL = "profileImageUrl";
    private static final String EXTRA_PROFILE_VIDEO_URL = "profileVideoUrl";

    private static final int MENU_ITEM_ID = 1;

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 10_000;
    private static final int READ_TIMEOUT_MILLISECONDS = 30_000;

    private static final String STRING_SAVE = "morphe_kakaotalk_profile_media_download_save";
    private static final String STRING_STARTED = "morphe_kakaotalk_profile_media_download_started";
    private static final String STRING_SAVED = "morphe_kakaotalk_profile_media_download_saved";
    private static final String STRING_FAILED = "morphe_kakaotalk_profile_media_download_failed";

    // The app bar is drawn over the photo and therefore uses shadowed icons instead of theme colours.
    private static final String DRAWABLE_MORE = "profile_ico_more_shadow";
    private static final String KAKAO_STRING_MORE = "label_for_more";

    /**
     * Set when the action bar of the viewer starts composing, so that it is refreshed within the
     * same composition that renders the edit button.
     */
    private static volatile boolean myProfile;

    private AllowProfileMediaDownloadPatch() {
    }

    public static boolean onProfileActionBar(boolean original) {
        myProfile = original;
        return original || Settings.allowProfileMediaDownload();
    }

    public static boolean hideProfileEditButton() {
        return !myProfile && Settings.allowProfileMediaDownload();
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    /**
     * The app bar never presents an overflow, so the entry must be shown as an action,
     * which is also how KakaoTalk adds entries to this app bar itself.
     */
    public static void setUpProfileItemDetailDownload(Activity activity) {
        if (!Settings.allowProfileMediaDownload()) return;

        try {
            // onCreate returns from more than one branch, so the entry is only added once, and only
            // when the viewer was actually given media to save.
            if (isBlank(mediaUrl(activity))) return;

            View root = activity.findViewById(android.R.id.content);
            if (!(root instanceof ViewGroup)) return;

            Toolbar toolbar = Utils.getChildView((ViewGroup) root, true, view -> view instanceof Toolbar);
            if (toolbar == null) {
                Logger.printDebug(() -> "No app bar to add the profile media save entry to");
                return;
            }
            if (toolbar.getMenu().findItem(MENU_ITEM_ID) != null) return;

            MenuItem item = toolbar.getMenu()
                    .add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, title(activity));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            int iconId = ResourceHelper.getResourceId("drawable", DRAWABLE_MORE);
            if (iconId != 0) item.setIcon(iconId);

            item.setOnMenuItemClickListener(menuItem -> {
                showMenu(activity, toolbar);
                return true;
            });
        } catch (Exception ex) {
            Logger.printException(() -> "Could not set up profile media download", ex);
        }
    }

    private static CharSequence title(Activity activity) {
        int moreId = ResourceHelper.getResourceId("string", KAKAO_STRING_MORE);
        return moreId == 0 ? string(activity, STRING_SAVE) : activity.getString(moreId);
    }

    private static void showMenu(Activity activity, Toolbar toolbar) {
        // The action view of a menu item carries the item id as its view id.
        View anchor = toolbar.findViewById(MENU_ITEM_ID);

        PopupMenu popup = new PopupMenu(activity, anchor == null ? toolbar : anchor);
        popup.getMenu()
                .add(Menu.NONE, Menu.NONE, Menu.NONE, string(activity, STRING_SAVE))
                .setOnMenuItemClickListener(menuItem -> {
                    download(activity);
                    return true;
                });
        popup.show();
    }

    private static String videoUrl(Activity activity) {
        Intent intent = activity.getIntent();
        return intent == null ? null : intent.getStringExtra(EXTRA_PROFILE_VIDEO_URL);
    }

    private static String mediaUrl(Activity activity) {
        String video = videoUrl(activity);
        if (!isBlank(video)) return video;

        Intent intent = activity.getIntent();
        return intent == null ? null : intent.getStringExtra(EXTRA_PROFILE_IMAGE_URL);
    }

    private static void download(Activity activity) {
        String url = mediaUrl(activity);
        if (isBlank(url)) return;

        boolean video = !isBlank(videoUrl(activity));

        // Only strings and the application context are handed to the worker so that leaving the
        // viewer mid download does not keep the activity alive. Toasts are thread safe.
        String saved = string(activity, STRING_SAVED);
        String failed = string(activity, STRING_FAILED);
        Utils.showToastShort(string(activity, STRING_STARTED));

        Context context = activity.getApplicationContext();
        new Thread(() -> {
            boolean succeeded;
            try {
                succeeded = save(context, url, video);
            } catch (Exception ex) {
                succeeded = false;
                Logger.printException(() -> "Could not save profile media", ex);
            }

            Utils.showToastShort(succeeded ? saved : failed);
        }).start();
    }

    /**
     * KakaoTalk's own utility copies the image out of the image loader disk cache, which the mini
     * profile viewer does not populate, so the media is fetched and stored here instead.
     * Writing through MediaStore needs no storage permission of its own on Android 10 and above.
     */
    private static boolean save(Context context, String url, boolean video) throws IOException {
        String extension = video ? "mp4" : extensionOf(url);
        String displayName = "KakaoTalk_profile_" + System.currentTimeMillis() + "." + extension;

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeOf(extension, video));

        String directory = video ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, directory + "/KakaoTalk");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri collection = video
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Uri item = resolver.insert(collection, values);
        if (item == null) return false;

        try {
            copy(url, resolver, item);
        } catch (Exception ex) {
            resolver.delete(item, null, null);
            throw ex instanceof IOException ? (IOException) ex : new IOException(ex);
        }

        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(item, values, null, null);

        return true;
    }

    private static void copy(String url, ContentResolver resolver, Uri item) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Unexpected response " + responseCode);
            }

            copy(connection, resolver, item);
        } finally {
            connection.disconnect();
        }
    }

    private static void copy(HttpURLConnection connection, ContentResolver resolver, Uri item) throws IOException {
        try (InputStream input = connection.getInputStream();
             OutputStream output = resolver.openOutputStream(item)) {
            if (output == null) throw new IOException("Could not open the media store entry");

            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    private static String extensionOf(String url) {
        String path = url;

        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);

        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot < path.lastIndexOf('/')) return "jpg";

        String extension = path.substring(dot + 1).toLowerCase();
        return extension.length() > 4 || extension.isEmpty() ? "jpg" : extension;
    }

    private static String mimeTypeOf(String extension, boolean video) {
        if (video) return "video/mp4";

        switch (extension) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String string(Activity activity, String stringName) {
        int resourceId = ResourceHelper.getResourceId("string", stringName);
        return resourceId == 0 ? "" : activity.getString(resourceId);
    }

}