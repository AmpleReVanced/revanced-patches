package app.revanced.extension.kakaotalk.keywordlog;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class KeywordLogPatch {

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
            KeywordLogEntry latest = KeywordLogStore.getInstance().latest();
            Logger.printDebug(() -> "Keyword log chat room prepared");
            return latest == null ? string("desc_for_keyword_log_list") : latest.message;
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to resolve the keyword log preview", ex);
            return "";
        }
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

}