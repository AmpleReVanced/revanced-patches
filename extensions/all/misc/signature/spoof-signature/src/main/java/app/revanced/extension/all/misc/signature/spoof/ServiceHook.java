package app.revanced.extension.all.misc.signature.spoof;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Generic replacement of an Android framework {@code IBinder} backed service.
 *
 * <p>The original IBinder obtained via {@code ServiceManager.getService(name)} is wrapped into its
 * AIDL {@code asInterface} proxy, then a {@link Proxy} implementing the service interface intercepts
 * calls and delegates them to {@code MethodHook}. {@link Proxy} is also used to replace the binder
 * itself, intercepting {@code queryLocalInterface} to return the hooked service interface and
 * forwarding every other binder call to the original binder. The replacement is registered in
 * {@code ServiceManager.sCache} so that any future {@code ServiceManager.getService(name)} lookup
 * returns our hooked binder.</p>
 */
final class ServiceHook {

    private ServiceHook() {
    }

    interface MethodHook {
        Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    static IInterface replace(String serviceName, String stubClassName, String interfaceClassName,
                              MethodHook hook) throws Exception {
        IBinder originalBinder = (IBinder) ReflectionUtils.invokeStatic(
                "android.os.ServiceManager",
                "getService",
                new Class[]{String.class},
                new Object[]{serviceName});
        if (originalBinder == null) {
            return null;
        }

        Class<?> stubClass = Class.forName(stubClassName);
        Method asInterface = stubClass.getDeclaredMethod("asInterface", IBinder.class);
        Object originalService = asInterface.invoke(null, originalBinder);
        if (!(originalService instanceof IInterface)) {
            return null;
        }

        IInterface originalInterface = (IInterface) originalService;
        Class<?> interfaceClass = Class.forName(interfaceClassName);
        IInterface hookedInterface = (IInterface) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                (proxy, method, args) -> hook.invoke(originalInterface, method, args));

        IBinder hookedBinder = (IBinder) Proxy.newProxyInstance(
                IBinder.class.getClassLoader(),
                new Class[]{IBinder.class},
                new BinderInvocationHandler(hookedInterface, originalBinder));

        Object sCache = ReflectionUtils.getStaticField("android.os.ServiceManager", "sCache");
        if (sCache instanceof Map) {
            ((Map<String, IBinder>) sCache).put(serviceName, hookedBinder);
        }

        return hookedInterface;
    }

    /**
     * Invocation handler used for the replacement binder. Calls to {@code queryLocalInterface}
     * return the hooked service interface; everything else is forwarded verbatim to the original
     * binder so transactions keep working.
     */
    private static final class BinderInvocationHandler implements InvocationHandler {
        private final IInterface hookedInterface;
        private final IBinder originalBinder;

        BinderInvocationHandler(IInterface hookedInterface, IBinder originalBinder) {
            this.hookedInterface = hookedInterface;
            this.originalBinder = originalBinder;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("queryLocalInterface".equals(method.getName())) {
                return hookedInterface;
            }
            return method.invoke(originalBinder, args);
        }
    }
}