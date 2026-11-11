package app.revanced.extension.riroschool.identity;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class FormParameters {
    /**
     * 역할: 정적 폼 필터의 인스턴스 생성을 막는다.
     * 입력: 없음.
     * 출력: 생성된 내부 인스턴스이며 외부에서는 호출할 수 없다.
     */
    private FormParameters() { }

    /**
     * 역할: 최상위 폼에서 UTF-8로 해석한 이름이 정확히 device_id인 항목만 제거한다.
     * 입력: 인코딩 전 최종 폼 문자열이며 null은 그대로 허용한다.
     * 출력: 나머지 필드의 원문과 순서를 보존한 문자열이며 잘못 인코딩된 키는 보존한다.
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
     * 역할: 값에 접근하지 않고 폼 키만 엄격한 UTF-8 percent 규칙으로 해석한다.
     * 입력: 등호 앞의 원시 키 문자열.
     * 출력: 정상적으로 해석한 키가 정확히 device_id이면 true, 잘못된 인코딩이면 false.
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
     * 역할: URL percent escape에 허용되는 ASCII 16진 숫자만 판별한다.
     * 입력: percent escape의 한 문자.
     * 출력: 0~15 또는 허용되지 않은 문자이면 -1.
     */
    private static int asciiHex(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }
}
