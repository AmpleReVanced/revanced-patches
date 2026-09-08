package app.revanced.extension.navermap.liveupdate;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

@SuppressWarnings({"unused", "deprecation"})
public final class LiveUpdatePatch {
    private static final String PREFIX = "android.ongoingActivityNoti.";
    private static final int PROGRESS_MAX = 1000;
    private static volatile Context context;

    private LiveUpdatePatch() {
    }

    public static void initialize(Context source) {
        context = source.getApplicationContext();
    }

    public static boolean supportsLiveUpdates(boolean supportsNowBar) {
        return supportsNowBar || Build.VERSION.SDK_INT >= 36;
    }

    public static Notification promote(Notification notification) {
        Context applicationContext = context;
        if (Build.VERSION.SDK_INT < 36 || applicationContext == null || notification == null ||
                notification.extras == null || !notification.extras.containsKey(PREFIX + "style")) {
            return notification;
        }
        try {
            return Api36Impl.promote(applicationContext, notification);
        } catch (RuntimeException | LinkageError exception) {
            Log.w("NaverMapLiveUpdates", "Could not convert navigation notification", exception);
            return notification;
        }
    }

    @RequiresApi(36)
    private static final class Api36Impl {
        static Notification promote(Context context, Notification notification) {
            Bundle extras = notification.extras;
            CharSequence title = extras.getCharSequence(PREFIX + "primaryInfo");
            if (TextUtils.isEmpty(title)) {
                title = extras.getCharSequence(Notification.EXTRA_TITLE);
            }
            if (TextUtils.isEmpty(title)) {
                return notification;
            }
            CharSequence secondary = extras.getCharSequence(PREFIX + "secondaryInfo");
            CharSequence description = extras.getCharSequence(PREFIX + "description");
            CharSequence text = TextUtils.isEmpty(secondary) ? description
                    : TextUtils.isEmpty(description) ? secondary : secondary + " · " + description;
            if (TextUtils.isEmpty(text)) {
                text = extras.getCharSequence(Notification.EXTRA_TEXT);
            }

            Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification.clone())
                    .setCustomContentView(null)
                    .setCustomBigContentView(null)
                    .setCustomHeadsUpContentView(null)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .setGroupSummary(false)
                    .setColorized(false)
                    .setCategory(Notification.CATEGORY_NAVIGATION)
                    .setStyle(progressStyle(extras));

            String chipText = extras.getString(PREFIX + "chipExpandedText");
            if (!TextUtils.isEmpty(chipText) && chipText.length() <= 7) {
                builder.setShortCriticalText(chipText);
            }
            Bundle convertedExtras = builder.getExtras();
            for (String key : new ArrayList<>(convertedExtras.keySet())) {
                if (key.startsWith(PREFIX)) {
                    convertedExtras.remove(key);
                }
            }
            convertedExtras.remove("android.contains.customView");
            convertedExtras.putBoolean("android.requestPromotedOngoing", true);
            Notification converted = builder.build();
            if (converted.hasPromotableCharacteristics()) {
                return converted;
            }
            Notification colorized = builder.setColorized(true).build();
            return colorized.hasPromotableCharacteristics() ? colorized : converted;
        }

        private static Notification.ProgressStyle progressStyle(Bundle extras) {
            Notification.ProgressStyle style = new Notification.ProgressStyle();
            int max = extras.getInt(PREFIX + "progressMax");
            if (max > 0) {
                int progress = Math.max(0, Math.min(extras.getInt(PREFIX + "progress"), max));
                style.setProgress((int) ((long) progress * PROGRESS_MAX / max));
                if (!addSegments(style, extras)) {
                    Notification.ProgressStyle.Segment segment =
                            new Notification.ProgressStyle.Segment(PROGRESS_MAX);
                    String colorKey = PREFIX + "progressSegments.progressColor";
                    if (extras.containsKey(colorKey)) {
                        segment.setColor(extras.getInt(colorKey));
                    }
                    style.addProgressSegment(segment);
                }
            } else {
                style.setProgressIndeterminate(true);
            }
            Icon tracker = extras.getParcelable(PREFIX + "progressSegments.icon", Icon.class);
            if (tracker == null) {
                tracker = extras.getParcelable(PREFIX + "chipIcon", Icon.class);
            }
            if (tracker != null) {
                style.setProgressTrackerIcon(tracker);
            }
            return style.setStyledByProgress(false);
        }

        private static boolean addSegments(Notification.ProgressStyle style, Bundle extras) {
            Parcelable[] values = extras.getParcelableArray(PREFIX + "progressSegments");
            if (values == null || values.length == 0) {
                return false;
            }
            ArrayList<Notification.ProgressStyle.Segment> segments = new ArrayList<>();
            int start = 0;
            for (int index = 0; index < values.length; index++) {
                if (!(values[index] instanceof Bundle)) {
                    return false;
                }
                Bundle segment = (Bundle) values[index];
                float fraction = segment.getFloat(PREFIX + "progressSegments.segmentStart", Float.NaN);
                if (!Float.isFinite(fraction) || fraction < 0 || fraction > 1 ||
                        Math.round(fraction * PROGRESS_MAX) != start) {
                    return false;
                }
                int end = PROGRESS_MAX;
                if (index + 1 < values.length) {
                    if (!(values[index + 1] instanceof Bundle)) {
                        return false;
                    }
                    float next = ((Bundle) values[index + 1]).getFloat(
                            PREFIX + "progressSegments.segmentStart", Float.NaN);
                    if (!Float.isFinite(next) || next < fraction || next > 1) {
                        return false;
                    }
                    end = Math.round(next * PROGRESS_MAX);
                }
                if (end > start) {
                    segments.add(new Notification.ProgressStyle.Segment(end - start)
                            .setColor(segment.getInt(PREFIX + "progressSegments.segmentColor")));
                }
                start = end;
            }
            if (segments.isEmpty()) {
                return false;
            }
            style.setProgressSegments(segments);
            return true;
        }
    }
}