package app.revanced.extension.kakaotalk.patches;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class HiddenMessageAuthorPatch {
    private static final String PREFERENCES = "morphe_kakaotalk_hidden_message_author";
    private static final String MEMBER_NAME_METHOD = "morphe_hiddenMessageAuthorName";
    private static final String MEMBER_PROFILE_METHOD = "morphe_openHiddenMessageAuthorProfile";
    private static final int OPENLINK_KICKED = 6;
    private static final int CHAT_KICKED = 7;
    private static final int OPENLINK_REWRITE_FEED = 13;
    private static final int OPENCHAT_REWRITES_FEED = 26;
    private static final int MAX_ENTRIES = 3000;
    private static final int MAX_LABEL_NAME_LENGTH = 10;

    private HiddenMessageAuthorPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static void onRewritesFeed(Object chatRoom, long authorId, long feedLogId, String feed) {
        try {
            JSONArray chatLogs = new JSONObject(feed).optJSONArray("chatLogInfos");
            if (chatLogs == null || chatLogs.length() == 0) return;

            String name = memberName(chatRoom, authorId);
            if (name == null) return;

            String author = ":" + authorId + ":" + name;
            SharedPreferences preferences = preferences();
            SharedPreferences.Editor editor = preferences.edit()
                    .putString(Long.toString(feedLogId), chatLogs.length() + author);
            for (int i = 0; i < chatLogs.length(); i++) {
                long logId = chatLogs.getJSONObject(i).optLong("logId");
                if (logId > 0L) editor.putString(Long.toString(logId), "0" + author);
            }
            editor.apply();
            trim(preferences);
        } catch (Exception exception) {
            Logger.printException(() -> "Could not record who hid messages", exception);
        }
    }

    public static CharSequence feedMessage(String feed, CharSequence message, Object chatRoom, Object chatLog, long userId) {
        if (message.length() == 0 || !Settings.showHiddenMessageAuthor()) return message;

        try {
            JSONObject json = new JSONObject(feed);
            switch (json.optInt("feedType")) {
                case OPENLINK_REWRITE_FEED:
                case OPENCHAT_REWRITES_FEED:
                    return hiddenMessage(message, chatRoom, longValue(chatLog, "getId"));
                case OPENLINK_KICKED:
                case CHAT_KICKED:
                    return kickedMessage(json, message, chatRoom, longValue(chatLog, "getUserId"), userId);
                default:
                    return message;
            }
        } catch (Exception exception) {
            Logger.printException(() -> "Could not show who hid messages or removed members", exception);
            return message;
        }
    }

    private static CharSequence hiddenMessage(CharSequence message, Object chatRoom, long logId) {
        Author author = author(logId);
        if (author == null) return message;

        String text = author.count > 1
                ? str("morphe_kakaotalk_hidden_by_messages", author.name, author.count)
                : str("morphe_kakaotalk_hidden_by_message", author.name);
        return withNameSpan(text, author.name, new ProfileSpan(chatRoom, author.id));
    }

    private static CharSequence kickedMessage(JSONObject feed, CharSequence message, Object chatRoom,
                                              long kickerId, long userId) throws Exception {
        JSONObject member = feed.optJSONObject("member");
        long kickedId = member == null ? 0L : member.optLong("userId");
        if (kickerId <= 0L || kickerId == kickedId) return message;

        String kicker = memberName(chatRoom, kickerId);
        if (kicker == null) return message;

        if (kickedId == userId) {
            return withNameSpan(str("morphe_kakaotalk_kicked_you_by", kicker), kicker, new ProfileSpan(chatRoom, kickerId));
        }

        SpannableString text = withNameSpan(str("morphe_kakaotalk_kicked_by", kicker, message), kicker, new ProfileSpan(chatRoom, kickerId));
        String kicked = memberName(chatRoom, kickedId);
        if (kicked != null) setNameSpan(text, kicked, new ProfileSpan(chatRoom, kickedId));
        return text;
    }

    public static CharSequence hiddenLabel(long chatLogId) {
        if (!Settings.showHiddenMessageAuthor()) return null;

        Author author = author(chatLogId);
        if (author == null) return null;

        String name = author.name;
        if (name.codePointCount(0, name.length()) > MAX_LABEL_NAME_LENGTH) {
            name = name.substring(0, name.offsetByCodePoints(0, MAX_LABEL_NAME_LENGTH)) + "…";
        }
        return withNameSpan(str("morphe_kakaotalk_chatlog_hidden_by", name), name, new UnderlineSpan());
    }

    public static boolean hasAuthor(long chatLogId) {
        return Settings.showHiddenMessageAuthor() && author(chatLogId) != null;
    }

    public static void openProfile(View view, Object chatRoom, long chatLogId) {
        Author author = author(chatLogId);
        if (author != null) openProfile(view.getContext(), chatRoom, author.id);
    }

    private static String memberName(Object chatRoom, long userId) throws Exception {
        String name = (String) chatRoom.getClass().getMethod(MEMBER_NAME_METHOD, long.class).invoke(chatRoom, userId);
        return name == null || name.isEmpty() ? null : name;
    }

    private static long longValue(Object target, String getter) throws Exception {
        return (Long) target.getClass().getMethod(getter).invoke(target);
    }

    private static void openProfile(Context context, Object chatRoom, long userId) {
        try {
            chatRoom.getClass()
                    .getMethod(MEMBER_PROFILE_METHOD, Context.class, long.class)
                    .invoke(chatRoom, context, userId);
        } catch (Exception exception) {
            Logger.printException(() -> "Could not open the profile of who hid the message", exception);
        }
    }

    private static SpannableString withNameSpan(String text, String name, Object span) {
        SpannableString spannable = new SpannableString(text);
        setNameSpan(spannable, name, span);
        return spannable;
    }

    private static void setNameSpan(SpannableString text, String name, Object span) {
        String plain = text.toString();
        for (int start = plain.indexOf(name); start >= 0; start = plain.indexOf(name, start + 1)) {
            int end = start + name.length();
            if (text.getSpans(start, end, Object.class).length == 0) {
                text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return;
            }
        }
    }

    private static Author author(long logId) {
        String value = preferences().getString(Long.toString(logId), null);
        if (value == null) return null;

        String[] parts = value.split(":", 3);
        try {
            return new Author(Integer.parseInt(parts[0]), Long.parseLong(parts[1]), parts[2]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void trim(SharedPreferences preferences) {
        Map<String, ?> entries = preferences.getAll();
        int excess = entries.size() - MAX_ENTRIES;
        if (excess <= 0) return;

        SharedPreferences.Editor editor = preferences.edit();
        entries.keySet().stream()
                .map(Long::parseLong)
                .sorted()
                .limit(excess)
                .forEach(logId -> editor.remove(Long.toString(logId)));
        editor.apply();
    }

    private static SharedPreferences preferences() {
        return Utils.getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static final class ProfileSpan extends ClickableSpan {
        private final Object chatRoom;
        private final long userId;

        ProfileSpan(Object chatRoom, long userId) {
            this.chatRoom = chatRoom;
            this.userId = userId;
        }

        @Override
        public void onClick(View widget) {
            openProfile(widget.getContext(), chatRoom, userId);
        }
    }

    private static final class Author {
        final int count;
        final long id;
        final String name;

        Author(int count, long id, String name) {
            this.count = count;
            this.id = id;
            this.name = name;
        }
    }
}
