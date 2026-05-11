package app.revanced.extension.unicorn.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

final class LicenseNative {
    private static final String DEFAULT_AUTH_ID = "revanced-auth";
    private static final String DEFAULT_AUTH_EMAIL = "revanced@example.com";
    private static final String DEFAULT_DEVICE_ID = "revanced-device";
    private static final String DEFAULT_LANGUAGE_CODE = "en";
    private static final String DEFAULT_COUNTRY_CODE = "US";
    private static final String DEFAULT_OS_TYPE = "android";
    private static final String DEFAULT_LICENSE_ID = "revanced-license";
    private static final String DEFAULT_LICENSE_NAME = "Unicorn Pro";
    private static final String DEFAULT_LICENSE_EXPIRES_AT = "2099-12-31T23:59:59.000Z";
    private static final String DEFAULT_LICENSE_SCOPE = "pro";

    private LicenseNative() {
    }

    static boolean handles(String owner) {
        return NativeOwners.LICENSE.equals(owner)
                || NativeOwners.LICENSE_MANAGER.equals(owner)
                || NativeOwners.LICENSE_AUTH.equals(owner)
                || NativeOwners.LICENSE_DEVICE.equals(owner)
                || NativeOwners.SIGN_IN_REQUIRE.equals(owner)
                || NativeOwners.UNCONFIRMED_EMAIL.equals(owner);
    }

    static Object call(String owner, String method, Object[] args) {
        if (NativeOwners.LICENSE.equals(owner)) return callLicense(method, args);
        if (NativeOwners.LICENSE_MANAGER.equals(owner)) return callManager(method, args);
        if (NativeOwners.LICENSE_AUTH.equals(owner)) return callAuth(method, args);
        if (NativeOwners.LICENSE_DEVICE.equals(owner)) return callDevice(method, args);

        if (NativeOwners.UNCONFIRMED_EMAIL.equals(owner) && "native_GetUserId".equals(method)) {
            Throwable throwable = NativeRuntime.throwable(NativeRuntime.longValue(args[0]));
            return throwable instanceof UserIdException ? ((UserIdException) throwable).userId : null;
        }

        if ("native_GetTypeId".equals(method)) {
            return NativeRuntime.longObject(NativeRuntime.typeId(owner));
        }

        throw NativeRuntime.unsupported(owner + "." + method);
    }

    @SuppressWarnings("unused")
    static long newLicenseFromJson(String json) {
        Map<String, Object> root = NativeJson.object(json);
        Map<String, Object> newLicense = NativeJson.objectAt(root, "newLicense");

        LinkedHashSet<String> deviceIds = new LinkedHashSet<>();
        for (Map<String, Object> device : NativeJson.objectListAt(root, "devices")) {
            if (device.containsKey("clientId")) {
                deviceIds.add(NativeJson.stringAt(device, "clientId", ""));
            }
        }

        LinkedHashMap<String, String> localizations = new LinkedHashMap<>();
        if (newLicense != null) {
            for (Map<String, Object> localization : NativeJson.objectListAt(newLicense, "localizations")) {
                localizations.put(
                        NativeJson.stringAt(localization, "langCode", ""),
                        NativeJson.stringAt(localization, "name", ""));
            }
        }

        return NativeRuntime.put(new LicenseRec(
                NativeJson.stringAt(root, "id", ""),
                deviceIds,
                NativeJson.stringAt(root, "expAt", ""),
                NativeJson.booleanAt(root, "expired", false),
                NativeJson.booleanAt(root, "trial", false),
                newLicense == null ? 0 : NativeJson.intAt(newLicense, "life", 0),
                localizations,
                newLicense == null ? 0 : NativeJson.intAt(newLicense, "maxDevicesCount", 0),
                newLicense == null ? "" : NativeJson.stringAt(newLicense, "scope", "")));
    }

    @SuppressWarnings("unused")
    static long newAuthFromJson(String json) {
        Map<String, Object> root = NativeJson.object(json);
        return NativeRuntime.put(new AuthRec(
                NativeJson.stringAt(root, "id", ""),
                NativeJson.stringAt(root, "email", ""),
                NativeJson.stringAt(root, "accessToken", ""),
                NativeJson.stringAt(root, "refreshToken", ""),
                NativeJson.intAt(root, "scope", 0),
                NativeJson.longAt(root, "exp", 0L)));
    }

    @SuppressWarnings("unused")
    static long newDeviceFromJson(String json) {
        Map<String, Object> root = NativeJson.object(json);
        return NativeRuntime.put(new DeviceRec(
                NativeJson.stringAt(root, "clientId", ""),
                NativeJson.stringAt(root, "id", ""),
                NativeJson.stringAt(root, "osType", DEFAULT_OS_TYPE),
                NativeJson.stringAt(root, "osVersion", ""),
                NativeJson.stringAt(root, "languageCode", ""),
                NativeJson.stringAt(root, "countryCode", "").toUpperCase(Locale.US),
                root.containsKey("pushToken") ? NativeJson.stringAt(root, "pushToken", null) : null,
                NativeJson.booleanAt(root, "pushEnabled", false)));
    }

    private static Object callLicense(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        LicenseRec license = NativeRuntime.get(handle, LicenseRec.class);

        switch (method) {
            case "native_GetId":
                return license.id();
            case "native_GetDeviceIds":
                return NativeRuntime.handle(new LinkedHashSet<>(license.deviceIds()));
            case "native_GetExpiredAt":
                return license.expiredAt();
            case "native_GetIsExpired":
                return Boolean.valueOf(license.expired());
            case "native_GetIsTrial":
                return Boolean.valueOf(license.trial());
            case "native_GetLife":
                return Integer.valueOf(license.life());
            case "native_GetLocalizations":
                return NativeRuntime.handle(new LinkedHashMap<>(license.localizations()));
            case "native_GetMaxDevice":
                return Integer.valueOf(license.maxDevice());
            case "native_GetScope":
                return license.scope();
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE + "." + method);
        }
    }

    private static Object callManager(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return NativeRuntime.handle(new ManagerRec(
                    NativeRuntime.longValue(args[0]),
                    NativeRuntime.string(args[1]),
                    NativeRuntime.string(args[2]),
                    NativeRuntime.string(args[3]),
                    NativeRuntime.string(args[4]),
                    NativeRuntime.string(args[5]),
                    args.length > 6 ? args[6] : null));
        }

        long handle = NativeRuntime.longValue(args[0]);
        ManagerRec manager = NativeRuntime.get(handle, ManagerRec.class);

        switch (method) {
            case "native_GetLicense":
                return NativeRuntime.longObject(manager.licenseHandle());
            case "native_GetAuth":
                return NativeRuntime.longObject(manager.authHandle());
            case "native_GetDevice":
                return NativeRuntime.longObject(manager.deviceHandle());
            case "native_StartAppProcess":
                return startAppProcess(manager, args);
            case "native_SignIn":
                return signIn(manager, args);
            case "native_DetachLicense":
                manager.detachLicense();
                NativeRuntime.invoke(args[1]);
                return null;
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_MANAGER + "." + method);
        }
    }

    private static Object startAppProcess(ManagerRec manager, Object[] args) {
        try {
            NativeRuntime.invoke(args[1], NativeRuntime.longObject(manager.licenseHandle()));
        } catch (RuntimeException e) {
            NativeRuntime.invoke(args[2], NativeRuntime.longObject(NativeRuntime.exceptionHandle(e)));
        }
        return null;
    }

    private static Object signIn(ManagerRec manager, Object[] args) {
        try {
            manager.signIn(NativeRuntime.string(args[1]));
            NativeRuntime.invoke(args[4], NativeRuntime.longObject(manager.rawLicenseHandle()));
        } catch (RuntimeException e) {
            NativeRuntime.invoke(args[5], NativeRuntime.longObject(NativeRuntime.exceptionHandle(e)));
        }
        return null;
    }

    private static Object callAuth(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        AuthRec auth = NativeRuntime.get(handle, AuthRec.class);

        switch (method) {
            case "native_GetId":
                return auth.id();
            case "native_GetEmail":
                return auth.email();
            case "native_GetScope":
                return Integer.valueOf(auth.scope());
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_AUTH + "." + method);
        }
    }

    private static Object callDevice(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        DeviceRec device = NativeRuntime.get(handle, DeviceRec.class);

        switch (method) {
            case "native_GetServerId":
                return device.serverId();
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_DEVICE + "." + method);
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.length() == 0 ? defaultValue : value;
    }

    static final class LicenseRec {
        private final String id;
        private final LinkedHashSet<String> deviceIds;
        private final String expiredAt;
        private final boolean expired;
        private final boolean trial;
        private final int life;
        private final LinkedHashMap<String, String> localizations;
        private final int maxDevice;
        private final String scope;

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

        String id() {
            return id;
        }

        LinkedHashSet<String> deviceIds() {
            return deviceIds;
        }

        String expiredAt() {
            return expiredAt;
        }

        boolean expired() {
            return expired;
        }

        boolean trial() {
            return trial;
        }

        int life() {
            return life;
        }

        LinkedHashMap<String, String> localizations() {
            return localizations;
        }

        int maxDevice() {
            return maxDevice;
        }

        String scope() {
            return scope;
        }

        LicenseRec copy() {
            return new LicenseRec(id, new LinkedHashSet<>(deviceIds), expiredAt, expired, trial, life,
                    new LinkedHashMap<>(localizations), maxDevice, scope);
        }
    }

    static final class AuthRec {
        private final String id;
        private final String email;
        private final String accessToken;
        private final String refreshToken;
        private final int scope;
        private final long exp;

        AuthRec(String id, String email, String accessToken, String refreshToken, int scope, long exp) {
            this.id = id;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.scope = scope;
            this.exp = exp;
        }

        String id() {
            return id;
        }

        String email() {
            return email;
        }

        int scope() {
            return scope;
        }

        AuthRec copy() {
            return new AuthRec(id, email, accessToken, refreshToken, scope, exp);
        }
    }

    static final class DeviceRec {
        private final String clientId;
        private final String serverId;
        private final String osType;
        private final String osVersion;
        private final String languageCode;
        private final String countryCode;
        private final String pushToken;
        private final boolean pushEnabled;

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

        String clientId() {
            return clientId;
        }

        String serverId() {
            return serverId;
        }

        DeviceRec copy() {
            return new DeviceRec(clientId, serverId, osType, osVersion, languageCode, countryCode, pushToken, pushEnabled);
        }
    }

    private static final class ManagerRec {
        private AuthRec auth;
        private DeviceRec device;
        private LicenseRec license;

        ManagerRec(long ignoredApiHandle, String ignoredHomeDir, String osVersion, String deviceId, String languageCode,
                String countryCode, Object ignoredOnSignOut) {
            seedDefaults(osVersion, deviceId, languageCode, countryCode);
        }

        private void seedDefaults(String osVersion, String deviceId, String languageCode, String countryCode) {
            String normalizedDeviceId = valueOrDefault(deviceId, DEFAULT_DEVICE_ID);
            String normalizedOsVersion = valueOrDefault(osVersion, DEFAULT_OS_TYPE);
            String normalizedLanguageCode = valueOrDefault(languageCode, DEFAULT_LANGUAGE_CODE);
            String normalizedCountryCode = valueOrDefault(countryCode, DEFAULT_COUNTRY_CODE).toUpperCase(Locale.US);

            LinkedHashSet<String> deviceIds = new LinkedHashSet<>();
            deviceIds.add(normalizedDeviceId);

            LinkedHashMap<String, String> localizations = new LinkedHashMap<>();
            localizations.put(DEFAULT_LANGUAGE_CODE, DEFAULT_LICENSE_NAME);
            localizations.put(normalizedLanguageCode, DEFAULT_LICENSE_NAME);

            auth = new AuthRec(
                    DEFAULT_AUTH_ID,
                    DEFAULT_AUTH_EMAIL,
                    "revanced-access-token",
                    "revanced-refresh-token",
                    Integer.MAX_VALUE,
                    Long.MAX_VALUE);
            device = new DeviceRec(
                    normalizedDeviceId,
                    DEFAULT_DEVICE_ID,
                    DEFAULT_OS_TYPE,
                    normalizedOsVersion,
                    normalizedLanguageCode,
                    normalizedCountryCode,
                    null,
                    false);
            license = new LicenseRec(
                    DEFAULT_LICENSE_ID,
                    deviceIds,
                    DEFAULT_LICENSE_EXPIRES_AT,
                    false,
                    false,
                    Integer.MAX_VALUE,
                    localizations,
                    Integer.MAX_VALUE,
                    DEFAULT_LICENSE_SCOPE);
        }

        private long licenseHandle() {
            if (!hasUsableLicense()) {
                return 0L;
            }
            return rawLicenseHandle();
        }

        private long rawLicenseHandle() {
            return license == null ? 0L : NativeRuntime.put(license.copy());
        }

        private boolean hasUsableLicense() {
            return auth != null
                    && license != null
                    && (device == null
                    || license.deviceIds().isEmpty()
                    || license.deviceIds().contains(device.clientId()));
        }

        private long authHandle() {
            return auth == null ? 0L : NativeRuntime.put(auth.copy());
        }

        private long deviceHandle() {
            return device == null ? 0L : NativeRuntime.put(device.copy());
        }

        private void signIn(String id) {
            if (license == null) {
                throw NativeRuntime.unsupported("LicenseManager.native_SignIn");
            }
            auth = new AuthRec(id, id, "", "", 0, 0L);
        }

        private void detachLicense() {
            license = null;
        }
    }

    @SuppressWarnings("unused")
    private static final class UserIdException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        final String userId;

        UserIdException(String message, String userId) {
            super(message);
            this.userId = userId;
        }
    }
}
