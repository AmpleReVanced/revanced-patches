package app.revanced.extension.kakaotalk.keywordlog;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class KeywordLogPatch {
    private static final String REFRESH_ROOM_LIST_METHOD = "patch_refreshKeywordLogChatRoom";
    private static volatile RoomSnapshot latestRoomSnapshot;
    private static volatile boolean roomSnapshotLoaded;
    private static volatile WeakReference<Object> chatRoomListRefreshTarget =
            new WeakReference<>(null);

    private KeywordLogPatch() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static void recordFromChatLog(Object chatLog, boolean matched) {
    }

    public static Intent createChatRoomIntent(Context context, long chatRoomId) {
        return null;
    }

    public static String resolveChatRoomName(long chatRoomId) {
        return null;
    }

    public static CharSequence highlight(SpannableStringBuilder message) {
        return message;
    }

    public static boolean isEnabled() {
        try {
            return isPatchIncluded() && Settings.RESTORE_KEYWORD_LOG.get();
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to read the keyword log setting", ex);
            return false;
        }
    }

    public static void record(
            long id,
            long chatRoomId,
            long userId,
            String sender,
            String profileUrl,
            String message,
            long createdAt
    ) {
        if (!isEnabled()) return;

        updateRoomSnapshot(id, message, createdAt);
        requestChatRoomListRefresh();
        Utils.runOnBackgroundThread(() -> {
            String chatRoomName = null;
            try {
                chatRoomName = resolveChatRoomName(chatRoomId);
            } catch (Throwable ex) {
                Logger.printException(() -> "Failed to resolve a chat room name", ex);
            }
            KeywordLogStore.getInstance().insert(
                    id,
                    chatRoomId,
                    userId,
                    sender,
                    profileUrl,
                    chatRoomName,
                    message,
                    createdAt
            );
        });
    }

    public static String roomTitle() {
        try {
            return string("title_for_keyword_log_list");
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log room title", ex);
            return "";
        }
    }

    public static String roomLastMessage() {
        try {
            RoomSnapshot latest = roomSnapshot();
            Logger.printDebug(() -> "Keyword log chat room prepared");
            return latest == null ? string("desc_for_keyword_log_list") : latest.message;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log preview", ex);
            return "";
        }
    }

    public static long roomLastLogId() {
        try {
            RoomSnapshot latest = roomSnapshot();
            return latest == null ? 0L : latest.id;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log id", ex);
            return 0L;
        }
    }

    public static int roomLastUpdatedAt() {
        try {
            RoomSnapshot latest = roomSnapshot();
            if (latest == null || latest.createdAt <= 0L) return 0;

            long seconds = latest.createdAt / 1000L;
            return seconds >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log timestamp", ex);
            return 0;
        }
    }

    public static void registerChatRoomListRefreshTarget(Object target) {
        if (target == null || chatRoomListRefreshTarget.get() == target) return;
        chatRoomListRefreshTarget = new WeakReference<>(target);
    }

    public static int roomProfileDrawable() {
        try {
            return ResourceUtils.getDrawableIdentifier("morphe_kakaotalk_keyword_log_profile");
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log profile", ex);
            return 0;
        }
    }

    public static void openList(Context context) {
        try {
            Intent intent = new Intent(context, KeywordLogListActivity.class);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to open the keyword log list", ex);
        }
    }

    static String string(String name) {
        int identifier = ResourceUtils.getStringIdentifier(name);
        return identifier == 0 ? "" : Utils.getContext().getString(identifier);
    }

    private static RoomSnapshot roomSnapshot() {
        if (roomSnapshotLoaded) return latestRoomSnapshot;

        synchronized (KeywordLogPatch.class) {
            if (roomSnapshotLoaded) return latestRoomSnapshot;

            KeywordLogEntry entry = KeywordLogStore.getInstance().latest();
            latestRoomSnapshot = entry == null
                    ? null
                    : new RoomSnapshot(entry.id, entry.message, entry.createdAt);
            roomSnapshotLoaded = true;
            return latestRoomSnapshot;
        }
    }

    private static void updateRoomSnapshot(long id, String message, long createdAt) {
        synchronized (KeywordLogPatch.class) {
            RoomSnapshot latest = latestRoomSnapshot;
            if (latest == null
                    || createdAt > latest.createdAt
                    || (createdAt == latest.createdAt && id >= latest.id)) {
                latestRoomSnapshot = new RoomSnapshot(id, message, createdAt);
            }
            roomSnapshotLoaded = true;
        }
    }

    private static void requestChatRoomListRefresh() {
        Object target = chatRoomListRefreshTarget.get();
        if (target == null) return;

        Utils.runOnMainThread(() -> {
            try {
                Method method = target.getClass().getDeclaredMethod(
                        REFRESH_ROOM_LIST_METHOD,
                        Object.class
                );
                method.invoke(null, target);
            } catch (Throwable ex) {
                Logger.printException(() -> "Failed to refresh the keyword log chat room", ex);
            }
        });
    }

    private static final class RoomSnapshot {
        final long id;
        final String message;
        final long createdAt;

        RoomSnapshot(long id, String message, long createdAt) {
            this.id = id;
            this.message = message == null ? "" : message;
            this.createdAt = createdAt;
        }
    }

}