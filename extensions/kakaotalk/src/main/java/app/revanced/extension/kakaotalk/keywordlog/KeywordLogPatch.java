package app.revanced.extension.kakaotalk.keywordlog;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class KeywordLogPatch {
    private static final String REFRESH_ROOM_LIST_METHOD = "patch_refreshKeywordLogChatRoom";
    private static final Map<Long, RoomSnapshot> ROOM_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final RoomSnapshot EMPTY_ROOM = new RoomSnapshot(0L, "", 0L);
    private static volatile WeakReference<Object> chatRoomListRefreshTarget =
            new WeakReference<>(null);

    private KeywordLogPatch() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static void recordFromChatLog(Object chatLog, boolean matched) {
    }

    public static Intent createChatRoomIntent(Context context, long chatRoomId, long chatLogId, long ownerId) {
        return null;
    }

    public static String resolveChatRoomName(long chatRoomId, long ownerId) {
        return null;
    }

    public static CharSequence highlight(SpannableStringBuilder message) {
        return message;
    }

    public static long currentOwnerId() {
        return 0L;
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
            long createdAt,
            long ownerId
    ) {
        if (!isEnabled()) return;

        updateRoomSnapshot(ownerId, id, message, createdAt);
        if (ownerId == currentOwnerId()) requestChatRoomListRefresh();
        Utils.runOnBackgroundThread(() -> {
            String chatRoomName = null;
            try {
                chatRoomName = resolveChatRoomName(chatRoomId, ownerId);
            } catch (Throwable ex) {
                Logger.printException(() -> "Failed to resolve a chat room name", ex);
            }
            KeywordLogStore.getInstance(ownerId).insert(
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
            return string("morphe_kakaotalk_keyword_log_title");
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log room title", ex);
            return "";
        }
    }

    public static String roomLastMessage(long ownerId) {
        try {
            RoomSnapshot latest = roomSnapshot(ownerId);
            Logger.printDebug(() -> "Keyword log chat room prepared");
            return latest == EMPTY_ROOM ? string("morphe_kakaotalk_keyword_log_summary") : latest.message;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log preview", ex);
            return "";
        }
    }

    public static long roomLastLogId(long ownerId) {
        try {
            RoomSnapshot latest = roomSnapshot(ownerId);
            return latest.id;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log id", ex);
            return 0L;
        }
    }

    public static int roomLastUpdatedAt(long ownerId) {
        try {
            RoomSnapshot latest = roomSnapshot(ownerId);
            if (latest.createdAt <= 0L) return 0;

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

    private static RoomSnapshot roomSnapshot(long ownerId) {
        return ROOM_SNAPSHOTS.computeIfAbsent(ownerId, id -> {
            KeywordLogEntry entry = KeywordLogStore.getInstance(id).latest();
            return entry == null
                    ? EMPTY_ROOM
                    : new RoomSnapshot(entry.id, entry.message, entry.createdAt);
        });
    }

    private static void updateRoomSnapshot(long ownerId, long id, String message, long createdAt) {
        ROOM_SNAPSHOTS.merge(ownerId, new RoomSnapshot(id, message, createdAt),
                (latest, incoming) -> incoming.createdAt > latest.createdAt
                        || (incoming.createdAt == latest.createdAt && incoming.id >= latest.id)
                        ? incoming : latest);
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
