package app.revanced.extension.unicorn.core;

public final class NativeRouter {
    private NativeRouter() {
    }

    public static Object call(String owner, String method, String returnType, boolean hasReceiver, Object... rawArgs) {
        Object[] args = NativeRuntime.stripReceiver(hasReceiver, rawArgs);

        try {
            if (owner.startsWith("Lcom/unicornsoft/android/unicornpro/core/std/")) {
                return StdNative.call(owner, method, args);
            }
            if (LicenseNative.handles(owner)) {
                return LicenseNative.call(owner, method, args);
            }
            throw NativeRuntime.unsupported(owner + "." + method);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
