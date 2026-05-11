package app.revanced.extension.unicorn.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

final class LicenseNative {
    private static final Class<?>[] LONG_ARGS = { long.class };

    private static final String AUTH_ID = "revanced-auth";
    private static final String AUTH_EMAIL = "revanced@example.com";
    private static final String DEVICE_ID = "revanced-device";
    private static final String LICENSE_ID = "revanced-license";
    private static final String LICENSE_NAME = "Unicorn Pro";
    private static final String LICENSE_EXPIRED_AT = "2099-12-31T23:59:59.000Z";
    private static final String LICENSE_SCOPE = "pro";

    private static final String SIGN_IN_REQUIRE_TYPE =
            "N7unicorn8pro_core7license7Manager9Exception6Common13SignInRequireE";
    private static final String UNCONFIRMED_EMAIL_TYPE =
            "N7unicorn7pro_api4Auth9Exception12RequestToken18UnconfirmedAccountE";

    private LicenseNative() {
    }

    static boolean handles(String owner) {
        return NativeOwners.LICENSE.equals(owner)
                || NativeOwners.LICENSE_MANAGER.equals(owner)
                || NativeOwners.LICENSE_AUTH.equals(owner)
                || NativeOwners.LICENSE_DEVICE.equals(owner)
                || NativeOwners.LICENSE_TRACKER.equals(owner)
                || NativeOwners.SIGN_IN_REQUIRE.equals(owner)
                || NativeOwners.UNCONFIRMED_EMAIL.equals(owner);
    }

    static Object call(String owner, String method, Object[] args) {
        if (NativeOwners.LICENSE.equals(owner)) return callLicense(method, args);
        if (NativeOwners.LICENSE_MANAGER.equals(owner)) return callManager(method, args);
        if (NativeOwners.LICENSE_AUTH.equals(owner)) return callAuth(method, args);
        if (NativeOwners.LICENSE_DEVICE.equals(owner)) return callDevice(method, args);
        if (NativeOwners.LICENSE_TRACKER.equals(owner)) return callTracker(method, args);
        if (NativeOwners.SIGN_IN_REQUIRE.equals(owner)) return callSignInRequire(method);
        if (NativeOwners.UNCONFIRMED_EMAIL.equals(owner)) return callUnconfirmedEmail(method, args);

        throw NativeRuntime.unsupported(owner + "." + method);
    }

    private static Object callLicense(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        if (isOriginalHandle(handle)) {
            return original(NativeOwners.LICENSE, method, handle);
        }

        LicenseRec license = NativeRuntime.get(handle, LicenseRec.class);

        switch (method) {
            case "native_GetId":
                return license.id;
            case "native_GetDeviceIds":
                return NativeRuntime.handle(new LinkedHashSet<>(license.deviceIds));
            case "native_GetExpiredAt":
                return license.expiredAt;
            case "native_GetIsExpired":
                return Boolean.valueOf(license.expired);
            case "native_GetIsTrial":
                return Boolean.valueOf(license.trial);
            case "native_GetLife":
                return Integer.valueOf(license.life);
            case "native_GetLocalizations":
                return NativeRuntime.handle(new LinkedHashMap<>(license.localizations));
            case "native_GetMaxDevice":
                return Integer.valueOf(license.maxDevice);
            case "native_GetScope":
                return license.scope;
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE + "." + method);
        }
    }

    private static Object callManager(String method, Object[] args) {
        switch (method) {
            case "native_GetLicense":
                return NativeRuntime.longObject(newLicenseHandle());
            case "native_GetAuth":
                return NativeRuntime.longObject(NativeRuntime.put(defaultAuth()));
            case "native_GetDevice":
                return NativeRuntime.longObject(NativeRuntime.put(defaultDevice()));
            case "native_StartAppProcess":
                NativeRuntime.invoke(args[1], NativeRuntime.longObject(newLicenseHandle()));
                return null;
            case "native_SignIn":
                NativeRuntime.invoke(args[4], NativeRuntime.longObject(newLicenseHandle()));
                return null;
            case "native_DetachLicense":
                NativeRuntime.invoke(args[1]);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_MANAGER + "." + method);
        }
    }

    private static Object callAuth(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        if (isOriginalHandle(handle)) {
            return original(NativeOwners.LICENSE_AUTH, method, handle);
        }

        AuthRec auth = NativeRuntime.get(handle, AuthRec.class);

        switch (method) {
            case "native_GetId":
                return auth.id;
            case "native_GetEmail":
                return auth.email;
            case "native_GetScope":
                return Integer.valueOf(auth.scope);
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_AUTH + "." + method);
        }
    }

    private static Object callDevice(String method, Object[] args) {
        long handle = NativeRuntime.longValue(args[0]);
        if (isOriginalHandle(handle)) {
            return original(NativeOwners.LICENSE_DEVICE, method, handle);
        }

        DeviceRec device = NativeRuntime.get(handle, DeviceRec.class);

        switch (method) {
            case "native_GetServerId":
                return device.serverId;
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_DEVICE + "." + method);
        }
    }

    private static Object callTracker(String method, Object[] args) {
        switch (method) {
            case "native_new":
                return NativeRuntime.handle(new LicenseTrackerRec(args[2], args[3]));
            case "native_Interrupt":
                NativeRuntime.get(NativeRuntime.longValue(args[0]), LicenseTrackerRec.class).interrupt();
                return null;
            case "native_delete": {
                long handle = NativeRuntime.longValue(args[0]);
                LicenseTrackerRec tracker = NativeRuntime.get(handle, LicenseTrackerRec.class);
                tracker.close();
                NativeRuntime.delete(handle);
                return null;
            }
            default:
                throw NativeRuntime.unsupported(NativeOwners.LICENSE_TRACKER + "." + method);
        }
    }

    private static Object callSignInRequire(String method) {
        if ("native_GetTypeId".equals(method)) {
            return NativeRuntime.longObject(NativeRuntime.typeId(SIGN_IN_REQUIRE_TYPE));
        }
        throw NativeRuntime.unsupported(NativeOwners.SIGN_IN_REQUIRE + "." + method);
    }

    private static Object callUnconfirmedEmail(String method, Object[] args) {
        switch (method) {
            case "native_GetTypeId":
                return NativeRuntime.longObject(NativeRuntime.typeId(UNCONFIRMED_EMAIL_TYPE));
            case "native_GetUserId": {
                long handle = NativeRuntime.longValue(args[0]);
                if (isOriginalHandle(handle)) {
                    return original(NativeOwners.UNCONFIRMED_EMAIL, method, handle);
                }

                Throwable throwable = NativeRuntime.throwable(handle);
                return throwable instanceof UserIdException ? ((UserIdException) throwable).userId : null;
            }
            default:
                throw NativeRuntime.unsupported(NativeOwners.UNCONFIRMED_EMAIL + "." + method);
        }
    }

    private static long newLicenseHandle() {
        return NativeRuntime.put(defaultLicense());
    }

    private static LicenseRec defaultLicense() {
        LinkedHashSet<String> deviceIds = new LinkedHashSet<>();
        deviceIds.add(DEVICE_ID);

        LinkedHashMap<String, String> localizations = new LinkedHashMap<>();
        localizations.put("", LICENSE_NAME);
        localizations.put("en", LICENSE_NAME);
        localizations.put("ko", LICENSE_NAME);

        return new LicenseRec(
                LICENSE_ID,
                deviceIds,
                LICENSE_EXPIRED_AT,
                false,
                false,
                Integer.MAX_VALUE,
                localizations,
                Integer.MAX_VALUE,
                LICENSE_SCOPE);
    }

    private static AuthRec defaultAuth() {
        return new AuthRec(AUTH_ID, AUTH_EMAIL, Integer.MAX_VALUE);
    }

    private static DeviceRec defaultDevice() {
        return new DeviceRec(DEVICE_ID);
    }

    private static boolean isOriginalHandle(long handle) {
        return handle != 0L && NativeRuntime.getOrNull(handle) == null;
    }

    private static Object original(String owner, String method, long handle) {
        return NativeFallback.call(owner, method, LONG_ARGS, Long.valueOf(handle));
    }

    private static final class LicenseRec {
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
    }

    private static final class AuthRec {
        private final String id;
        private final String email;
        private final int scope;

        AuthRec(String id, String email, int scope) {
            this.id = id;
            this.email = email;
            this.scope = scope;
        }
    }

    private static final class DeviceRec {
        private final String serverId;

        DeviceRec(String serverId) {
            this.serverId = serverId;
        }
    }

    private static final class LicenseTrackerRec {
        private static final long CHECK_INTERVAL_MILLIS = 60_000L;

        private final Object onExcept;
        private final Object onChecked;
        private final Object lock = new Object();
        private final Thread worker;
        private volatile boolean stopped;

        LicenseTrackerRec(Object onExcept, Object onChecked) {
            this.onExcept = onExcept;
            this.onChecked = onChecked;
            this.worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    runLoop();
                }
            }, "LicenseTracker");
            this.worker.setDaemon(true);
            this.worker.start();
        }

        void interrupt() {
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        void close() {
            stopped = true;
            interrupt();
        }

        private void runLoop() {
            while (!stopped) {
                synchronized (lock) {
                    try {
                        lock.wait(CHECK_INTERVAL_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (!stopped) {
                    NativeRuntime.invoke(onChecked);
                }
            }
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
