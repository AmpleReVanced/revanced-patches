package app.revanced.extension.unicorn.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class LicenseNative {
    private static final String LICENSE = "Lcom/unicornsoft/android/unicornpro/core/License$Companion;";
    private static final String MANAGER = "Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;";
    private static final String AUTH = "Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Auth$Companion;";
    private static final String DEVICE = "Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Device$Companion;";
    private static final String SIGN_IN_REQUIRE =
            "Lcom/unicornsoft/android/unicornpro/core/LicenseManager$SignInRequireException$Companion;";
    private static final String UNCONFIRMED_EMAIL =
            "Lcom/unicornsoft/android/unicornpro/core/LicenseManager$UnconfirmedEmailException$Companion;";

    private LicenseNative() {
    }

    static boolean handles(String owner) {
        return owner.equals(LICENSE)
                || owner.equals(MANAGER)
                || owner.equals(AUTH)
                || owner.equals(DEVICE)
                || owner.equals(SIGN_IN_REQUIRE)
                || owner.equals(UNCONFIRMED_EMAIL);
    }

    static Object call(String owner, String method, Object[] args) {
        if (owner.equals(LICENSE)) return callLicense(method, args);
        if (owner.equals(MANAGER)) return callManager(method, args);
        if (owner.equals(AUTH)) return callAuth(method, args);
        if (owner.equals(DEVICE)) return callDevice(method, args);

        if (owner.equals(UNCONFIRMED_EMAIL) && "native_GetUserId".equals(method)) {
            Throwable throwable = NativeRuntime.throwable(NativeRuntime.longValue(args[0]));
            return throwable instanceof UserIdException ? ((UserIdException) throwable).userId : null;
        }

        if ("native_GetTypeId".equals(method)) return Long.valueOf(NativeRuntime.typeId(owner));

        throw NativeRuntime.unsupported(owner + "." + method);
    }

    static long newLicenseFromJson(String json) {
        Map<String, Object> root = Json.object(json);
        Map<String, Object> newLicense = Json.objectAt(root, "newLicense");

        LinkedHashSet<String> deviceIds = new LinkedHashSet<>();
        for (Object item : Json.arrayAt(root, "devices")) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> device = (Map<String, Object>) item;
                if (device.containsKey("clientId")) {
                    deviceIds.add(Json.stringAt(device, "clientId", ""));
                }
            }
        }

        LinkedHashMap<String, String> localizations = new LinkedHashMap<>();
        if (newLicense != null) {
            for (Object item : Json.arrayAt(newLicense, "localizations")) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> localization = (Map<String, Object>) item;
                    localizations.put(
                            Json.stringAt(localization, "langCode", ""),
                            Json.stringAt(localization, "name", ""));
                }
            }
        }

        return NativeRuntime.put(new LicenseRec(
                Json.stringAt(root, "id", ""),
                deviceIds,
                Json.stringAt(root, "expAt", ""),
                Json.booleanAt(root, "expired", false),
                Json.booleanAt(root, "trial", false),
                newLicense == null ? 0 : Json.intAt(newLicense, "life", 0),
                localizations,
                newLicense == null ? 0 : Json.intAt(newLicense, "maxDevicesCount", 0),
                newLicense == null ? "" : Json.stringAt(newLicense, "scope", "")));
    }

    static long newAuthFromJson(String json) {
        Map<String, Object> root = Json.object(json);
        return NativeRuntime.put(new AuthRec(
                Json.stringAt(root, "id", ""),
                Json.stringAt(root, "email", ""),
                Json.stringAt(root, "accessToken", ""),
                Json.stringAt(root, "refreshToken", ""),
                Json.intAt(root, "scope", 0),
                Json.longAt(root, "exp", 0L)));
    }

    static long newDeviceFromJson(String json) {
        Map<String, Object> root = Json.object(json);
        return NativeRuntime.put(new DeviceRec(
                Json.stringAt(root, "clientId", ""),
                Json.stringAt(root, "id", ""),
                Json.stringAt(root, "osType", "android"),
                Json.stringAt(root, "osVersion", ""),
                Json.stringAt(root, "languageCode", ""),
                Json.stringAt(root, "countryCode", "").toUpperCase(),
                root.containsKey("pushToken") ? Json.stringAt(root, "pushToken", null) : null,
                Json.booleanAt(root, "pushEnabled", false)));
    }

    private static Object callLicense(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        LicenseRec license = NativeRuntime.get(handle, LicenseRec.class);
        if ("native_GetId".equals(method)) return license.id;
        if ("native_GetDeviceIds".equals(method)) return Long.valueOf(NativeRuntime.put(new LinkedHashSet<>(license.deviceIds)));
        if ("native_GetExpiredAt".equals(method)) return license.expiredAt;
        if ("native_GetIsExpired".equals(method)) return Boolean.valueOf(license.expired);
        if ("native_GetIsTrial".equals(method)) return Boolean.valueOf(license.trial);
        if ("native_GetLife".equals(method)) return Integer.valueOf(license.life);
        if ("native_GetLocalizations".equals(method)) return Long.valueOf(NativeRuntime.put(new LinkedHashMap<>(license.localizations)));
        if ("native_GetMaxDevice".equals(method)) return Integer.valueOf(license.maxDevice);
        if ("native_GetScope".equals(method)) return license.scope;
        NativeRuntime.delete(handle);
        return null;
    }

    private static Object callManager(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return Long.valueOf(NativeRuntime.put(new ManagerRec(
                    NativeRuntime.longValue(args[0]),
                    NativeRuntime.string(args[1]),
                    NativeRuntime.string(args[2]),
                    NativeRuntime.string(args[3]),
                    NativeRuntime.string(args[4]),
                    NativeRuntime.string(args[5]),
                    args.length > 6 ? args[6] : null)));
        }

        long handle = NativeRuntime.longValue(args[0]);
        ManagerRec manager = NativeRuntime.get(handle, ManagerRec.class);

        if ("native_GetLicense".equals(method)) {
            if (manager.auth == null || manager.license == null) {
                return Long.valueOf(0L);
            }
            if (manager.device != null && !manager.license.deviceIds.isEmpty()
                    && !manager.license.deviceIds.contains(manager.device.clientId)) {
                return Long.valueOf(0L);
            }
            return Long.valueOf(NativeRuntime.put(manager.license.copy()));
        }
        if ("native_GetAuth".equals(method)) {
            return Long.valueOf(manager.auth == null ? 0L : NativeRuntime.put(manager.auth.copy()));
        }
        if ("native_GetDevice".equals(method)) {
            return Long.valueOf(manager.device == null ? 0L : NativeRuntime.put(manager.device.copy()));
        }
        if ("native_StartAppProcess".equals(method)) {
            try {
                NativeRuntime.invoke(args[1], callManager("native_GetLicense", new Object[]{Long.valueOf(handle)}));
            } catch (RuntimeException e) {
                NativeRuntime.invoke(args[2], Long.valueOf(NativeRuntime.exceptionHandle(e)));
            }
            return null;
        }
        if ("native_SignIn".equals(method)) {
            try {
                if (manager.license == null) {
                    throw NativeRuntime.unsupported("LicenseManager.native_SignIn");
                }
                manager.auth = new AuthRec(
                        NativeRuntime.string(args[1]),
                        NativeRuntime.string(args[1]),
                        "",
                        "",
                        0,
                        0L);
                NativeRuntime.invoke(args[4], Long.valueOf(NativeRuntime.put(manager.license.copy())));
            } catch (RuntimeException e) {
                NativeRuntime.invoke(args[5], Long.valueOf(NativeRuntime.exceptionHandle(e)));
            }
            return null;
        }
        if ("native_DetachLicense".equals(method)) {
            manager.license = null;
            NativeRuntime.invoke(args[1]);
            return null;
        }

        NativeRuntime.delete(handle);
        return null;
    }

    private static Object callAuth(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        AuthRec auth = NativeRuntime.get(handle, AuthRec.class);
        if ("native_GetId".equals(method)) return auth.id;
        if ("native_GetEmail".equals(method)) return auth.email;
        if ("native_GetScope".equals(method)) return Integer.valueOf(auth.scope);
        NativeRuntime.delete(handle);
        return null;
    }

    private static Object callDevice(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        DeviceRec device = NativeRuntime.get(handle, DeviceRec.class);
        if ("native_GetServerId".equals(method)) return device.serverId;
        NativeRuntime.delete(handle);
        return null;
    }

    static final class LicenseRec {
        final String id;
        final LinkedHashSet<String> deviceIds;
        final String expiredAt;
        final boolean expired;
        final boolean trial;
        final int life;
        final LinkedHashMap<String, String> localizations;
        final int maxDevice;
        final String scope;

        LicenseRec(String id, LinkedHashSet<String> deviceIds, String expiredAt, boolean expired,
                boolean trial, int life, LinkedHashMap<String, String> localizations, int maxDevice, String scope) {
            this.id = id;
            this.deviceIds = deviceIds;
            this.expiredAt = expiredAt;
            this.expired = expired;
            this.trial = trial;
            this.life = life;
            this.localizations = localizations;
            this.maxDevice = maxDevice;
            this.scope = scope;
        }

        LicenseRec copy() {
            return new LicenseRec(id, new LinkedHashSet<>(deviceIds), expiredAt, expired, trial, life,
                    new LinkedHashMap<>(localizations), maxDevice, scope);
        }
    }

    static final class AuthRec {
        final String id;
        final String email;
        final String accessToken;
        final String refreshToken;
        final int scope;
        final long exp;

        AuthRec(String id, String email, String accessToken, String refreshToken, int scope, long exp) {
            this.id = id;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.scope = scope;
            this.exp = exp;
        }

        AuthRec copy() {
            return new AuthRec(id, email, accessToken, refreshToken, scope, exp);
        }
    }

    static final class DeviceRec {
        final String clientId;
        final String serverId;
        final String osType;
        final String osVersion;
        final String languageCode;
        final String countryCode;
        final String pushToken;
        final boolean pushEnabled;

        DeviceRec(String clientId, String serverId, String osType, String osVersion, String languageCode,
                String countryCode, String pushToken, boolean pushEnabled) {
            this.clientId = clientId;
            this.serverId = serverId;
            this.osType = osType;
            this.osVersion = osVersion;
            this.languageCode = languageCode;
            this.countryCode = countryCode;
            this.pushToken = pushToken;
            this.pushEnabled = pushEnabled;
        }

        DeviceRec copy() {
            return new DeviceRec(clientId, serverId, osType, osVersion, languageCode, countryCode, pushToken, pushEnabled);
        }
    }

    private static final class ManagerRec {
        final long apiHandle;
        final String homeDir;
        final String osVersion;
        final String deviceId;
        final String languageCode;
        final String countryCode;
        final Object onSignOut;
        AuthRec auth;
        DeviceRec device;
        LicenseRec license;

        ManagerRec(long apiHandle, String homeDir, String osVersion, String deviceId, String languageCode,
                String countryCode, Object onSignOut) {
            this.apiHandle = apiHandle;
            this.homeDir = homeDir;
            this.osVersion = osVersion;
            this.deviceId = deviceId;
            this.languageCode = languageCode;
            this.countryCode = countryCode;
            this.onSignOut = onSignOut;
        }
    }

    private static final class UserIdException extends RuntimeException {
        final String userId;

        UserIdException(String message, String userId) {
            super(message);
            this.userId = userId;
        }
    }

    private static final class Json {
        private final String source;
        private int index;

        private Json(String source) {
            this.source = source == null ? "" : source;
        }

        static Map<String, Object> object(String json) {
            Object value = new Json(json).parse();
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("JSON root is not an object");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> objectAt(Map<String, Object> map, String key) {
            Object value = map.get(key);
            return value instanceof Map ? (Map<String, Object>) value : null;
        }

        @SuppressWarnings("unchecked")
        static List<Object> arrayAt(Map<String, Object> map, String key) {
            Object value = map.get(key);
            return value instanceof List ? (List<Object>) value : new ArrayList<Object>();
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

        private Object parse() {
            Object value = parseValue();
            skipWs();
            if (index != source.length()) {
                throw error("Trailing data");
            }
            return value;
        }

        private Object parseValue() {
            skipWs();
            if (index >= source.length()) throw error("Unexpected end");
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
            skipWs();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                String key = parseString();
                skipWs();
                expect(':');
                map.put(key, parseValue());
                skipWs();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            ArrayList<Object> out = new ArrayList<>();
            skipWs();
            if (peek(']')) {
                index++;
                return out;
            }
            while (true) {
                out.add(parseValue());
                skipWs();
                if (peek(']')) {
                    index++;
                    return out;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (index >= source.length()) throw error("Bad escape");
                char e = source.charAt(index++);
                if (e == '"' || e == '\\' || e == '/') out.append(e);
                else if (e == 'b') out.append('\b');
                else if (e == 'f') out.append('\f');
                else if (e == 'n') out.append('\n');
                else if (e == 'r') out.append('\r');
                else if (e == 't') out.append('\t');
                else if (e == 'u') {
                    if (index + 4 > source.length()) throw error("Bad unicode escape");
                    out.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                    index += 4;
                } else {
                    throw error("Bad escape");
                }
            }
            throw error("Unterminated string");
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            boolean floating = false;
            if (peek('.')) {
                floating = true;
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            }
            if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                floating = true;
                index++;
                if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            }
            String number = source.substring(start, index);
            return floating ? Double.valueOf(number) : Long.valueOf(number);
        }

        private void skipWs() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        private boolean peek(char c) {
            return index < source.length() && source.charAt(index) == c;
        }

        private void expect(char c) {
            if (!peek(c)) throw error("Expected '" + c + "'");
            index++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + index);
        }
    }
}
