package app.revanced.extension.riroschool.identity;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class FormParameters {
    /**
     * Prevents instantiation of this stateless form filter.
     */
    private FormParameters() { }

    /**
     * Removes root form entries whose strictly decoded UTF-8 key is device_id.
     * Preserves all other raw entries and their order, including malformed keys.
     * A null or empty body is returned unchanged.
     */
    public static String withoutDeviceId(String body) {
        if (body == null || body.length() == 0) return body;
        StringBuilder filtered = new StringBuilder(body.length());
        boolean retained = false;
        boolean removed = false;
        int start = 0;
        while (start <= body.length()) {
            int end = body.indexOf('&', start);
            if (end < 0) end = body.length();
            int keyEnd = start;
            while (keyEnd < end && body.charAt(keyEnd) != '=') keyEnd++;
            if (isDeviceId(body.substring(start, keyEnd))) {
                removed = true;
            } else {
                if (retained) filtered.append('&');
                filtered.append(body, start, end);
                retained = true;
            }
            if (end == body.length()) break;
            start = end + 1;
        }
        return removed ? filtered.toString() : body;
    }

    /**
     * Decodes a raw form key using strict UTF-8 percent escapes without examining values.
     * Returns false for malformed encoding or any key other than device_id.
     */
    private static boolean isDeviceId(String key) {
        StringBuilder decoded = new StringBuilder(key.length());
        for (int i = 0; i < key.length();) {
            char current = key.charAt(i);
            if (current == '%') {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while (i < key.length() && key.charAt(i) == '%') {
                    if (i + 2 >= key.length()) return false;
                    int high = asciiHex(key.charAt(i + 1));
                    int low = asciiHex(key.charAt(i + 2));
                    if (high < 0 || low < 0) return false;
                    bytes.write((high << 4) | low);
                    i += 3;
                }
                try {
                    decoded.append(StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())));
                } catch (CharacterCodingException malformed) {
                    return false;
                }
            } else {
                decoded.append(current == '+' ? ' ' : current);
                i++;
            }
        }
        return "device_id".contentEquals(decoded);
    }

    /**
     * Returns the value of an ASCII hexadecimal digit, or -1 for any other character.
     */
    private static int asciiHex(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }
}
