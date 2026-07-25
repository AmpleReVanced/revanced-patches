package app.revanced.extension.kakaotalk.chatlog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class ModifiedMessageHistory {
    private static final String MESSAGE_KEY = "message";
    private static final String REVISION_KEY = "revision";

    private ModifiedMessageHistory() {
    }

    static String merge(String currentHistory, String message, int revision) {
        List<Message> messages = parse(currentHistory);

        boolean exists = false;
        for (Message modifiedMessage : messages) {
            if (modifiedMessage.revision == revision) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            messages.add(new Message(revision, message));
        }

        sort(messages);

        JSONArray array = new JSONArray();
        for (Message modifiedMessage : messages) {
            array.put(modifiedMessage.toJson());
        }

        return array.toString();
    }

    static List<Message> parse(String historyJson) {
        List<Message> messages = new ArrayList<>();
        if (historyJson == null || historyJson.length() == 0) return messages;

        try {
            JSONArray array = new JSONArray(historyJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;

                messages.add(new Message(
                        object.optInt(REVISION_KEY),
                        object.optString(MESSAGE_KEY, "")
                ));
            }
        } catch (Exception e) {
            return messages;
        }

        sort(messages);
        return messages;
    }

    private static void sort(List<Message> messages) {
        Collections.sort(messages, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                return lhs.revision - rhs.revision;
            }
        });
    }

    static final class Message {
        final int revision;
        final String message;

        Message(int revision, String message) {
            this.revision = revision;
            this.message = message == null ? "" : message;
        }

        private JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put(REVISION_KEY, revision);
                object.put(MESSAGE_KEY, message);
            } catch (Exception e) {
                // ignore
            }
            return object;
        }
    }
}