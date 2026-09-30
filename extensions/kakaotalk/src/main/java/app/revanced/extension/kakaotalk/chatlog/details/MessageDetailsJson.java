package app.revanced.extension.kakaotalk.chatlog.details;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

final class MessageDetailsJson {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_VALUES = 20000;

    private final IdentityHashMap<Object, String> seen = new IdentityHashMap<>();
    private final Set<String> objects = new HashSet<>();
    private final Set<String> maps = new HashSet<>();
    private final Map<String, Object> captured = new HashMap<>();
    private final Map<String, String> references = new HashMap<>();
    private int values;

    private MessageDetailsJson() {
    }

    static MessageDetailsSnapshot capture(Object message) throws JSONException {
        MessageDetailsJson encoder = new MessageDetailsJson();
        JSONObject raw = (JSONObject) encoder.encode(message, "$", 0);
        return new MessageDetailsSnapshot(
                MessageDetailsFormatter.format(raw, encoder.objects, encoder.maps, encoder.captured, encoder.references),
                raw);
    }

    private Object encode(Object value, String path, int depth) throws JSONException {
        if (value == null || value == JSONObject.NULL) return JSONObject.NULL;
        if (++values > MAX_VALUES) return marker("$truncated", "value limit");
        if (value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Character || value instanceof CharSequence) return value.toString();
        if (value instanceof Number) {
            if (value instanceof Double && !Double.isFinite((Double) value)
                    || value instanceof Float && !Float.isFinite((Float) value)) {
                return value.toString();
            }
            return value;
        }
        if (value instanceof Enum<?>) return ((Enum<?>) value).name();
        if (value instanceof Class<?>) return ((Class<?>) value).getName();
        if (value instanceof java.io.File) return ((java.io.File) value).getPath();
        String previous = seen.get(value);
        if (previous != null) {
            references.put(path, previous);
            return marker("$ref", previous);
        }
        if (depth >= MAX_DEPTH) return marker("$truncated", "depth limit: " + value.getClass().getName());
        seen.put(value, path);

        if (value instanceof JSONObject) {
            JSONObject result = object(path);
            JSONObject source = (JSONObject) value;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, encode(source.opt(key), childPath(path, key), depth + 1));
                if (values > MAX_VALUES) {
                    result.put("$truncated", "value limit");
                    break;
                }
            }
            return result;
        }
        if (value instanceof JSONArray) {
            JSONArray source = (JSONArray) value;
            JSONArray result = array(path);
            for (int i = 0; i < source.length(); i++) {
                result.put(encode(source.opt(i), path + "[" + i + "]", depth + 1));
                if (values > MAX_VALUES) break;
            }
            return result;
        }
        if (value.getClass().isArray()) {
            JSONArray result = array(path);
            for (int i = 0; i < Array.getLength(value); i++) {
                result.put(encode(Array.get(value, i), path + "[" + i + "]", depth + 1));
                if (values > MAX_VALUES) break;
            }
            return result;
        }
        if (value instanceof Map<?, ?>) {
            maps.add(path);
            JSONObject result = object(path);
            JSONArray entries = new JSONArray();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String entryPath = path + "[\"$entries\"][" + entries.length() + "]";
                JSONObject pair = new JSONObject();
                pair.put("key", encode(entry.getKey(), childPath(entryPath, "key"), depth + 1));
                pair.put("value", encode(entry.getValue(), childPath(entryPath, "value"), depth + 1));
                entries.put(pair);
                if (values > MAX_VALUES) break;
            }
            return result.put("$entries", entries);
        }
        if (value instanceof Iterable<?>) {
            JSONArray result = array(path);
            for (Object item : (Iterable<?>) value) {
                result.put(encode(item, path + "[" + result.length() + "]", depth + 1));
                if (values > MAX_VALUES) break;
            }
            return result;
        }

        Class<?> type = value.getClass();
        objects.add(path);
        JSONObject result = object(path);
        result.put("$type", typeName(type));
        result.put("$class", type.getName());
        if (isRuntimeObject(type)) {
            result.put("$state", "runtime object; not traversed");
            return result;
        }
        for (Class<?> owner = type; owner != null && owner != Object.class; owner = owner.getSuperclass()) {
            Field[] fields = owner.getDeclaredFields();
            Arrays.sort(fields, Comparator.comparing(MessageDetailsJson::fieldName));
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String name = fieldName(field);
                if (result.has(name)) name = owner.getName() + "." + name;
                Object encoded;
                try {
                    field.setAccessible(true);
                    encoded = encode(field.get(value), childPath(path, name), depth + 1);
                } catch (Exception exception) {
                    encoded = marker("$error", exception.getClass().getSimpleName());
                }
                result.put(name, encoded);
            }
        }
        return result;
    }

    private JSONObject object(String path) {
        JSONObject value = new JSONObject();
        captured.put(path, value);
        return value;
    }

    private JSONArray array(String path) {
        JSONArray value = new JSONArray();
        captured.put(path, value);
        return value;
    }

    private static boolean isRuntimeObject(Class<?> type) {
        String name = type.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")
                || name.startsWith("androidx.") || name.startsWith("sun.") || name.startsWith("dalvik.")) {
            return true;
        }
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            if (owner.getName().startsWith("android.") || owner.getName().startsWith("androidx.")) return true;
            for (Class<?> contract : owner.getInterfaces()) {
                if (contract.getName().startsWith("kotlin.jvm.functions.")) return true;
            }
        }
        return false;
    }

    private static String fieldName(Field field) {
        MessageDetailsName name = field.getAnnotation(MessageDetailsName.class);
        return name == null ? field.getName() : name.value();
    }

    private static String typeName(Class<?> type) {
        MessageDetailsName name = type.getAnnotation(MessageDetailsName.class);
        return name == null ? type.getSimpleName() : name.value();
    }

    private static String childPath(String path, String key) {
        return path + "[" + JSONObject.quote(key) + "]";
    }

    private static JSONObject marker(String key, String value) throws JSONException {
        return new JSONObject().put(key, value);
    }
}