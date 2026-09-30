package app.revanced.extension.kakaotalk.chatlog.details;

import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MessageDetailsFormatter {
    private static final int MAX_VALUES = 20000;
    private static final int MAX_DEPTH = 24;
    private static final List<String> FIRST_FIELDS = Arrays.asList(
            "id", "chatRoomId", "userId", "type", "chatMessageType", "createdAt", "deletedAt",
            "message", "attachment", "v");
    private final Set<String> objects;
    private final Set<String> maps;
    private final Map<String, Object> captured;
    private final Map<String, String> references;
    private final Map<String, String> active = new HashMap<>();
    private int values;

    private MessageDetailsFormatter(Set<String> objects, Set<String> maps,
                                    Map<String, Object> captured, Map<String, String> references) {
        this.objects = objects;
        this.maps = maps;
        this.captured = captured;
        this.references = references;
    }

    static JSONObject format(JSONObject raw, Set<String> objects, Set<String> maps,
                             Map<String, Object> captured, Map<String, String> references) throws JSONException {
        return (JSONObject) new MessageDetailsFormatter(objects, maps, captured, references)
                .value(raw, "$", "$", 0, false);
    }

    private Object value(Object source, String path, String outputPath, int depth, boolean parseString) throws JSONException {
        if (source == null || source == JSONObject.NULL) return JSONObject.NULL;
        if (++values > MAX_VALUES || depth > MAX_DEPTH) return new JSONObject().put("$truncated", "display limit; see raw");
        String reference = references.get(path);
        if (reference != null && captured.containsKey(reference)) {
            return value(captured.get(reference), reference, outputPath, depth + 1, parseString);
        }
        String ancestor = active.get(path);
        if (ancestor != null) return new JSONObject().put("$ref", ancestor);
        if (source instanceof String) return parseString ? parseJson((String) source) : source;
        if (!(source instanceof JSONObject) && !(source instanceof JSONArray)) return source;
        active.put(path, outputPath);
        try {
            if (source instanceof JSONArray) {
                JSONArray array = (JSONArray) source;
                JSONArray result = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    result.put(value(array.opt(i), path + "[" + i + "]", outputPath + "[" + i + "]", depth + 1, false));
                    if (values > MAX_VALUES) break;
                }
                return result;
            }
            JSONObject object = (JSONObject) source;
            if (maps.contains(path) && stringKeys(object.getJSONArray("$entries"))) {
                JSONObject result = new JSONObject();
                JSONArray entries = object.getJSONArray("$entries");
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    String key = entry.getString("key");
                    result.put(key, value(entry.opt("value"), child(path, "$entries") + "[" + i + "][\"value\"]",
                            child(outputPath, key), depth + 1, false));
                    if (values > MAX_VALUES) break;
                }
                return result;
            }
            List<String> keys = keys(object);
            if (objects.contains(path)) keys.removeAll(Arrays.asList("$type", "$class"));
            keys.sort(Comparator.comparingInt((String key) -> "$".equals(path) ? priority(key) : FIRST_FIELDS.size())
                    .thenComparing(Comparator.naturalOrder()));
            if (path.equals(child("$", "v")) && objects.contains(path) && keys.size() == 1) {
                String key = keys.get(0);
                if (object.opt(key) instanceof JSONObject) {
                    return value(object.opt(key), child(path, key), outputPath, depth + 1, false);
                }
            }
            JSONObject result = new JSONObject();
            for (String key : keys) {
                result.put(key, value(object.opt(key), child(path, key), child(outputPath, key), depth + 1,
                        objects.contains(path) && !"message".equals(key)));
            }
            if ("$".equals(path) && result.has("attachment") && result.has("_attachmentJson")
                    && result.opt("attachment") instanceof JSONObject
                    && equal(result.opt("attachment"), result.opt("_attachmentJson"))) {
                result.remove("_attachmentJson");
            }
            return result;
        } finally {
            active.remove(path);
        }
    }

    private static Object parseJson(String source) {
        String trimmed = source.trim();
        try {
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) return new JSONObject(trimmed);
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) return new JSONArray(trimmed);
        } catch (JSONException ignored) {
        }
        return source;
    }

    private static boolean stringKeys(JSONArray entries) throws JSONException {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            Object key = entries.getJSONObject(i).opt("key");
            if (!(key instanceof String) || !keys.add((String) key)) return false;
        }
        return true;
    }

    private static boolean equal(Object first, Object second) throws JSONException {
        if (first instanceof JSONObject && second instanceof JSONObject) {
            JSONObject a = (JSONObject) first;
            JSONObject b = (JSONObject) second;
            if (a.length() != b.length()) return false;
            for (String key : keys(a)) if (!b.has(key) || !equal(a.opt(key), b.opt(key))) return false;
            return true;
        }
        if (first instanceof JSONArray && second instanceof JSONArray) {
            JSONArray a = (JSONArray) first;
            JSONArray b = (JSONArray) second;
            if (a.length() != b.length()) return false;
            for (int i = 0; i < a.length(); i++) if (!equal(a.opt(i), b.opt(i))) return false;
            return true;
        }
        return first == second || first != null && first.equals(second);
    }

    private static List<String> keys(JSONObject object) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) keys.add(iterator.next());
        return keys;
    }

    private static int priority(String key) {
        int index = FIRST_FIELDS.indexOf(key);
        return index < 0 ? FIRST_FIELDS.size() : index;
    }

    private static String child(String path, String key) {
        return path + "[" + JSONObject.quote(key) + "]";
    }

    static String prettyPrint(JSONObject object) throws JSONException {
        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.setIndent("  ");
            writeJson(writer, object, true);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not format message JSON", exception);
        }
        return output.toString();
    }

    private static void writeJson(JsonWriter writer, Object value, boolean root) throws IOException, JSONException {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            List<String> keys = keys(object);
            keys.sort(Comparator.comparingInt((String key) -> root ? priority(key) : FIRST_FIELDS.size())
                    .thenComparing(Comparator.naturalOrder()));
            writer.beginObject();
            for (String key : keys) {
                writer.name(key);
                writeJson(writer, object.opt(key), false);
            }
            writer.endObject();
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            writer.beginArray();
            for (int i = 0; i < array.length(); i++) writeJson(writer, array.opt(i), false);
            writer.endArray();
        } else if (value == null || value == JSONObject.NULL) {
            writer.nullValue();
        } else if (value instanceof Number) {
            writer.value((Number) value);
        } else if (value instanceof Boolean) {
            writer.value((Boolean) value);
        } else {
            writer.value(value.toString());
        }
    }
}