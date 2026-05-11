package app.revanced.extension.unicorn.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NativeJson {
    private final String source;
    private int index;

    private NativeJson(String source) {
        this.source = source == null ? "" : source;
    }

    static Map<String, Object> object(String json) {
        Map<String, Object> object = asObject(new NativeJson(json).parse());
        if (object == null) {
            throw new IllegalArgumentException("JSON root is not an object");
        }
        return object;
    }

    static Map<String, Object> objectAt(Map<String, Object> map, String key) {
        return asObject(map.get(key));
    }

    static List<Map<String, Object>> objectListAt(Map<String, Object> map, String key) {
        List<?> values = arrayAt(map, key);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<Map<String, Object>> objects = new ArrayList<>(values.size());
        for (Object value : values) {
            Map<String, Object> object = asObject(value);
            if (object != null) {
                objects.add(object);
            }
        }
        return objects;
    }

    static String stringAt(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    static boolean booleanAt(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    static int intAt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    static long longAt(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static List<?> arrayAt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List ? (List<?>) value : Collections.emptyList();
    }

    private Object parse() {
        Object value = parseValue();
        skipWhitespace();
        if (index != source.length()) {
            throw error("Trailing data");
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= source.length()) {
            throw error("Unexpected end");
        }

        char c = source.charAt(index);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (source.startsWith("true", index)) {
            index += 4;
            return Boolean.TRUE;
        }
        if (source.startsWith("false", index)) {
            index += 5;
            return Boolean.FALSE;
        }
        if (source.startsWith("null", index)) {
            index += 4;
            return null;
        }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        expect('{');
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (peek('}')) {
            index++;
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            map.put(key, parseValue());
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        expect('[');
        ArrayList<Object> values = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            index++;
            return values;
        }

        while (true) {
            values.add(parseValue());
            skipWhitespace();
            if (peek(']')) {
                index++;
                return values;
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (index < source.length()) {
            char c = source.charAt(index++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }

            if (index >= source.length()) {
                throw error("Bad escape");
            }

            char escaped = source.charAt(index++);
            if (escaped == '"' || escaped == '\\' || escaped == '/') {
                out.append(escaped);
            } else if (escaped == 'b') {
                out.append('\b');
            } else if (escaped == 'f') {
                out.append('\f');
            } else if (escaped == 'n') {
                out.append('\n');
            } else if (escaped == 'r') {
                out.append('\r');
            } else if (escaped == 't') {
                out.append('\t');
            } else if (escaped == 'u') {
                appendUnicodeEscape(out);
            } else {
                throw error("Bad escape");
            }
        }
        throw error("Unterminated string");
    }

    private void appendUnicodeEscape(StringBuilder out) {
        if (index + 4 > source.length()) {
            throw error("Bad unicode escape");
        }
        out.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
        index += 4;
    }

    private Number parseNumber() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
            index++;
        }

        boolean floating = false;
        if (peek('.')) {
            floating = true;
            index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }
        if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            floating = true;
            index++;
            if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                index++;
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }

        String number = source.substring(start, index);
        return floating ? Double.valueOf(number) : Long.valueOf(number);
    }

    private void skipWhitespace() {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
    }

    private boolean peek(char c) {
        return index < source.length() && source.charAt(index) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("Expected '" + c + "'");
        }
        index++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at " + index);
    }
}
