package app.revanced.extension.kakaotalk.packet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Serializer {

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "LocoHeader\\(packetId=(-?\\d+), status=(-?\\d+), method=([^,]+), bodyLength=(-?\\d+)\\)"
    );

    public JSONObject packetToJson(Object packet) throws Exception {
        JSONObject json = new JSONObject();

        Object header = findPacketPart(packet, "LocoHeader(");
        Object body = findPacketPart(packet, "LocoBody(");

        json.put("header", header == null ? JSONObject.NULL : serializeHeader(header));
        json.put("body", body == null ? JSONObject.NULL : serializeBody(body));
        return json;
    }

    public byte[] jsonToBytes(JSONObject json) {
        String jsonString = json.toString() + "\n";
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    private JSONObject serializeHeader(Object header) throws JSONException {
        JSONObject json = new JSONObject();

        Matcher matcher = HEADER_PATTERN.matcher(header.toString());
        if (matcher.matches()) {
            json.put("packetId", Integer.parseInt(matcher.group(1)));
            json.put("status", Integer.parseInt(matcher.group(2)));
            json.put("method", matcher.group(3));
            json.put("bodyLength", Integer.parseInt(matcher.group(4)));
            return json;
        }

        Integer packetId = null;
        Integer bodyLength = null;
        Integer status = null;
        String method = null;

        for (Field field : getInstanceFields(header.getClass())) {
            Object value = getFieldValue(field, header);
            if (value instanceof Short) {
                status = ((Short) value).intValue();
            } else if (value instanceof Integer) {
                if (packetId == null) {
                    packetId = (Integer) value;
                } else if (bodyLength == null) {
                    bodyLength = (Integer) value;
                }
            } else if (value instanceof Enum<?>) {
                method = ((Enum<?>) value).name();
            } else if (value != null && method == null) {
                method = value.toString();
            }
        }

        json.put("packetId", packetId == null ? JSONObject.NULL : packetId);
        json.put("status", status == null ? JSONObject.NULL : status);
        json.put("method", method == null ? JSONObject.NULL : method);
        json.put("bodyLength", bodyLength == null ? JSONObject.NULL : bodyLength);
        return json;
    }

    private JSONObject serializeBody(Object body) throws Exception {
        Object bson = findBsonContainer(body);
        if (bson == null) {
            JSONObject json = new JSONObject();
            json.put("raw", body.toString());
            return json;
        }

        Method toMap = findToMapMethod(bson.getClass());
        if (toMap == null) {
            JSONObject json = new JSONObject();
            json.put("raw", bson.toString());
            return json;
        }

        Object map = invokeMethod(toMap, bson);
        if (!(map instanceof Map<?, ?>)) {
            JSONObject json = new JSONObject();
            json.put("raw", map == null ? JSONObject.NULL : map.toString());
            return json;
        }

        return toJsonObject((Map<?, ?>) map);
    }

    private Object findPacketPart(Object packet, String toStringPrefix) {
        if (packet == null) return null;

        for (Field field : getInstanceFields(packet.getClass())) {
            Object value = getFieldValue(field, packet);
            if (hasToStringPrefix(value, toStringPrefix)) {
                return value;
            }
        }

        for (Method method : getInstanceMethods(packet.getClass())) {
            if (method.getParameterTypes().length != 0 ||
                    method.getReturnType() == Void.TYPE ||
                    method.getReturnType().isPrimitive() ||
                    method.getReturnType().isArray()) {
                continue;
            }

            Object value = invokeMethodQuietly(method, packet);
            if (hasToStringPrefix(value, toStringPrefix)) {
                return value;
            }
        }

        return null;
    }

    private Object findBsonContainer(Object body) {
        if (body == null) return null;

        if (findToMapMethod(body.getClass()) != null) {
            return body;
        }

        for (Field field : getInstanceFields(body.getClass())) {
            Object value = getFieldValue(field, body);
            if (value != null && findToMapMethod(value.getClass()) != null) {
                return value;
            }
        }

        for (Method method : getInstanceMethods(body.getClass())) {
            if (method.getParameterTypes().length != 0 ||
                    method.getReturnType() == Void.TYPE ||
                    method.getReturnType().isPrimitive()) {
                continue;
            }

            Object value = invokeMethodQuietly(method, body);
            if (value != null && findToMapMethod(value.getClass()) != null) {
                return value;
            }
        }

        return null;
    }

    private JSONObject toJsonObject(Map<?, ?> map) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            json.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return json;
    }

    private JSONArray toJsonArray(Object value) throws JSONException {
        JSONArray json = new JSONArray();

        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) {
                json.put(normalizeValue(item));
            }
            return json;
        }

        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            json.put(normalizeValue(Array.get(value, i)));
        }
        return json;
    }

    private Object normalizeValue(Object value) throws JSONException {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value == JSONObject.NULL ||
                value instanceof JSONObject ||
                value instanceof JSONArray ||
                value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?>) {
            return toJsonObject((Map<?, ?>) value);
        }
        if (value instanceof Iterable<?> || value.getClass().isArray()) {
            return toJsonArray(value);
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        return value.toString();
    }

    private Method findToMapMethod(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("toMap") &&
                    method.getParameterTypes().length == 0 &&
                    Map.class.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        }

        for (Method method : getInstanceMethods(type)) {
            if (method.getName().equals("toMap") &&
                    method.getParameterTypes().length == 0 &&
                    Map.class.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        }

        return null;
    }

    private boolean hasToStringPrefix(Object value, String prefix) {
        if (value == null) return false;

        try {
            return value.toString().startsWith(prefix);
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<Field> getInstanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private List<Method> getInstanceMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (!Modifier.isStatic(modifiers) && !method.isSynthetic()) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private Object getFieldValue(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeMethod(Method method, Object instance) throws Exception {
        method.setAccessible(true);
        return method.invoke(instance);
    }

    private Object invokeMethodQuietly(Method method, Object instance) {
        try {
            return invokeMethod(method, instance);
        } catch (Exception ignored) {
            return null;
        }
    }
}
