package app.revanced.extension.kakaotalk.chatlog.details;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class MessageDetailsSnapshot {
    private final JSONObject formatted;
    private final JSONObject raw;
    final String formattedJson;
    final String rawJson;
    final String messageId;
    final String messageType;
    final String localTime;

    MessageDetailsSnapshot(JSONObject formatted, JSONObject raw) throws JSONException {
        this.formatted = formatted;
        this.raw = raw;
        formattedJson = MessageDetailsFormatter.prettyPrint(formatted);
        rawJson = raw.toString(2);
        messageId = raw.optString("id", "?");
        messageType = raw.optString("chatMessageType", raw.optString("$type", "ChatLog"));
        long createdAt = raw.optLong("createdAt", 0);
        localTime = createdAt <= 0 ? "" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                Instant.ofEpochSecond(createdAt).atZone(ZoneId.systemDefault()));
    }

    static MessageDetailsSnapshot parse(String contents) throws JSONException {
        JSONObject stored = new JSONObject(contents);
        if (stored.optInt("version") == 2 && stored.optJSONObject("raw") != null
                && stored.optJSONObject("formatted") != null) {
            return new MessageDetailsSnapshot(stored.getJSONObject("formatted"), stored.getJSONObject("raw"));
        }
        return new MessageDetailsSnapshot(stored, stored);
    }

    String serialize() throws JSONException {
        return new JSONObject().put("version", 2).put("formatted", formatted).put("raw", raw).toString();
    }

    String json(boolean original) {
        return original ? rawJson : formattedJson;
    }
}