package app.revanced.extension.dcinside.patches;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.dcinside.helper.ResourceHelper;
import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class GalleryWatchModePatch {
    private static final long REFRESH_RESPONSE_WINDOW_MS = 120_000L;
    private static final long HIGHLIGHT_DURATION_MS = 300_000L;
    private static final long UNLIMITED_AUTO_REFRESH_SECONDS = 3_155_760_000L;
    private static final int HIGHLIGHT_COLOR = 0x263F8CFF;
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<Object, WeakReference<Object>> FRAGMENT_ADAPTERS =
            new WeakHashMap<>();
    private static final Map<Object, WatchState> ADAPTER_STATES = new WeakHashMap<>();
    private static int originalForegroundTagId;

    private GalleryWatchModePatch() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static long overrideAutoRefreshDuration(long originalDuration) {
        return Settings.galleryWatchMode() ? UNLIMITED_AUTO_REFRESH_SECONDS : originalDuration;
    }

    public static void register(Object adapter, Object fragment) {
        if (adapter == null || fragment == null) {
            return;
        }

        synchronized (LOCK) {
            FRAGMENT_ADAPTERS.put(fragment, new WeakReference<>(adapter));
            if (!ADAPTER_STATES.containsKey(adapter)) {
                ADAPTER_STATES.put(adapter, new WatchState());
            }
        }
    }

    public static void onAutoRefreshTick(Object fragment) {
        if (!Settings.galleryWatchMode() || fragment == null) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        synchronized (LOCK) {
            WeakReference<Object> adapterReference = FRAGMENT_ADAPTERS.get(fragment);
            Object adapter = adapterReference == null ? null : adapterReference.get();
            if (adapter == null) {
                FRAGMENT_ADAPTERS.remove(fragment);
                return;
            }

            WatchState state = ADAPTER_STATES.get(adapter);
            if (state == null) {
                state = new WatchState();
                ADAPTER_STATES.put(adapter, state);
            }
            state.waitingForRefresh = true;
            state.refreshStartedAt = now;
            state.refreshBoundary = state.newestPostNumber;
        }
    }

    public static void onPostListUpdated(Object adapter, Object response, int page) {
        if (adapter == null || page != 1) {
            return;
        }
        if (!Settings.galleryWatchMode()) {
            synchronized (LOCK) {
                WatchState state = ADAPTER_STATES.get(adapter);
                if (state != null) {
                    state.reset();
                }
            }
            return;
        }

        List<?> posts;
        try {
            posts = getPosts(response);
        } catch (Exception ignored) {
            return;
        }
        if (posts == null) {
            return;
        }

        Set<Integer> postNumbers = new HashSet<>();
        int newestPostNumber = 0;
        for (Object post : posts) {
            try {
                int postNumber = getPostNumber(post);
                if (postNumber > 0) {
                    postNumbers.add(postNumber);
                    newestPostNumber = Math.max(newestPostNumber, postNumber);
                }
            } catch (Exception ignored) {
            }
        }

        long now = SystemClock.elapsedRealtime();
        int newPostCount = 0;
        synchronized (LOCK) {
            WatchState state = ADAPTER_STATES.get(adapter);
            if (state == null) {
                state = new WatchState();
                ADAPTER_STATES.put(adapter, state);
            }
            state.removeExpiredHighlights(now);

            boolean isAutoRefreshResponse = state.waitingForRefresh
                    && now - state.refreshStartedAt <= REFRESH_RESPONSE_WINDOW_MS
                    && state.refreshBoundary > 0;
            if (isAutoRefreshResponse) {
                long highlightUntil = now + HIGHLIGHT_DURATION_MS;
                for (int postNumber : postNumbers) {
                    if (postNumber > state.refreshBoundary) {
                        if (!state.highlightedPosts.containsKey(postNumber)) {
                            newPostCount++;
                        }
                        state.highlightedPosts.put(postNumber, highlightUntil);
                    }
                }
            } else if (!state.waitingForRefresh) {
                state.highlightedPosts.clear();
            }

            state.waitingForRefresh = false;
            if (newestPostNumber > 0) {
                state.newestPostNumber = newestPostNumber;
            }
        }

        if (newPostCount > 0) {
            dispatchAlert(newPostCount);
        }
    }

    public static void bindPostRow(Object adapter, Object holder, int position) {
        View view;
        try {
            view = getItemView(holder);
        } catch (Exception ignored) {
            return;
        }
        if (view == null) {
            return;
        }

        boolean highlighted = false;
        if (Settings.galleryWatchMode()) {
            try {
                int postNumber = getPostNumber(getPostAt(adapter, position));
                long now = SystemClock.elapsedRealtime();
                synchronized (LOCK) {
                    WatchState state = ADAPTER_STATES.get(adapter);
                    Long highlightedUntil = state == null
                            ? null
                            : state.highlightedPosts.get(postNumber);
                    highlighted = highlightedUntil != null && highlightedUntil >= now;
                    if (highlightedUntil != null && !highlighted) {
                        state.highlightedPosts.remove(postNumber);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        int tagId = originalForegroundTagId;
        if (tagId == 0) {
            tagId = ResourceHelper.getResourceId(
                    "id",
                    "morphe_gallery_watch_original_foreground"
            );
            originalForegroundTagId = tagId;
        }
        if (tagId == 0) {
            return;
        }

        Object tag = view.getTag(tagId);
        OriginalForeground foreground;
        if (tag instanceof OriginalForeground) {
            foreground = (OriginalForeground) tag;
            if (view.getForeground() != foreground.highlight) {
                foreground.original = view.getForeground();
            }
        } else {
            foreground = new OriginalForeground(view.getForeground());
            view.setTag(tagId, foreground);
        }
        view.setForeground(highlighted ? foreground.highlight : foreground.original);
    }

    private static void dispatchAlert(final int count) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showAlert(count);
        } else {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    showAlert(count);
                }
            });
        }
    }

    private static void showAlert(int count) {
        Context context = Utils.getContext();
        int messageId = ResourceHelper.getResourceId("string", "morphe_gallery_watch_new_posts");
        String message = messageId == 0
                ? count + " new posts"
                : context.getString(messageId, count);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        if (Settings.galleryWatchSound()) {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(
                        context,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                );
                if (ringtone != null) {
                    ringtone.play();
                }
            } catch (Exception ignored) {
            }
        }

        if (Settings.galleryWatchVibration()) {
            try {
                Vibrator vibrator = context.getSystemService(Vibrator.class);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                            100L,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    ));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static List<?> getPosts(Object response) {
        return null;
    }

    private static Object getPostAt(Object adapter, int position) {
        return null;
    }

    private static int getPostNumber(Object post) {
        return 0;
    }

    private static View getItemView(Object holder) {
        return null;
    }

    private static final class WatchState {
        private int newestPostNumber;
        private int refreshBoundary;
        private long refreshStartedAt;
        private boolean waitingForRefresh;
        private final Map<Integer, Long> highlightedPosts = new HashMap<>();

        private void removeExpiredHighlights(long now) {
            List<Integer> expired = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : highlightedPosts.entrySet()) {
                if (entry.getValue() < now) {
                    expired.add(entry.getKey());
                }
            }
            for (int postNumber : expired) {
                highlightedPosts.remove(postNumber);
            }
        }

        private void reset() {
            newestPostNumber = 0;
            refreshBoundary = 0;
            refreshStartedAt = 0L;
            waitingForRefresh = false;
            highlightedPosts.clear();
        }
    }

    private static final class OriginalForeground {
        private Drawable original;
        private final Drawable highlight = new ColorDrawable(HIGHLIGHT_COLOR);

        private OriginalForeground(Drawable original) {
            this.original = original;
        }
    }
}