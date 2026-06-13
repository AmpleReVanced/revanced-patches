package app.revanced.extension.unicorn.core;

public final class NativeRouter {
    private NativeRouter() {
    }

    public static Object call(String owner, String method, String returnType, boolean hasReceiver, Object... rawArgs) {
        Object[] args = NativeRuntime.stripReceiver(hasReceiver, rawArgs);

        if (LicenseNative.handles(owner)) {
            return LicenseNative.call(owner, method, args);
        }
        if (owner.startsWith(NativeOwners.STD_PREFIX)) {
            return StdNative.call(owner, method, args);
        }

        throw NativeRuntime.unsupported(owner + "." + method);
    }
}
