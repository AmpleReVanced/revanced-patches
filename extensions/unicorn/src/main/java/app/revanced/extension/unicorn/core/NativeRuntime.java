package app.revanced.extension.unicorn.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class NativeRuntime {
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(0x10000000L);
    private static final Map<Long, Object> HANDLES = new ConcurrentHashMap<>();

    private NativeRuntime() {
    }

    static long put(Object value) {
        if (value == null) {
            return 0L;
        }

        long handle = NEXT_HANDLE.getAndIncrement();
        HANDLES.put(handle, value);
        return handle;
    }

    static Object get(long handle) {
        if (handle == 0L) {
            return null;
        }

        Object value = HANDLES.get(handle);
        if (value == null) {
            throw new IllegalArgumentException("Invalid native handle: " + handle);
        }
        return value;
    }

    static <T> T get(long handle, Class<T> type) {
        Object value = get(handle);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Handle " + handle + " is " + value.getClass().getName()
                    + ", expected " + type.getName());
        }
        return type.cast(value);
    }

    static void delete(long handle) {
        if (handle != 0L) {
            HANDLES.remove(handle);
        }
    }

    static long typeId(String name) {
        return name == null ? 0L : name.hashCode() & 0xffffffffL;
    }

    static Object[] stripReceiver(boolean hasReceiver, Object[] args) {
        if (!hasReceiver) {
            return args;
        }
        if (args.length == 0) {
            return args;
        }
        return Arrays.copyOfRange(args, 1, args.length);
    }

    static String propertyName(String nativeMethod, String prefix) {
        String name = nativeMethod.substring(prefix.length());
        if (name.isEmpty()) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static String componentName(String owner) {
        int start = owner.lastIndexOf('/') + 1;
        int end = owner.endsWith("$Companion;") ? owner.length() - "$Companion;".length() : owner.length() - 1;
        return owner.substring(start, end).replace('$', '.');
    }

    static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value == null ? new byte[0] : value);
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void invoke(Object callback, Object... args) {
        if (callback == null) {
            return;
        }

        Method fallback = null;
        for (Method method : callback.getClass().getMethods()) {
            if (!"invoke".equals(method.getName()) || method.getParameterTypes().length != args.length) {
                continue;
            }
            if (accepts(method.getParameterTypes(), args)) {
                call(callback, method, args);
                return;
            }
            fallback = method;
        }

        if (fallback != null) {
            call(callback, fallback, args);
            return;
        }

        throw new IllegalArgumentException("No invoke method on " + callback.getClass().getName());
    }

    static RuntimeException unsupported(String name) {
        return new UnsupportedOperationException(name + " needs the original C++ engine/API implementation");
    }

    static Object defaultValue(String returnType) {
        if ("V".equals(returnType)) {
            return null;
        }
        if ("Z".equals(returnType)) {
            return Boolean.FALSE;
        }
        if ("B".equals(returnType) || "S".equals(returnType) || "C".equals(returnType) || "I".equals(returnType)) {
            return Integer.valueOf(0);
        }
        if ("J".equals(returnType)) {
            return Long.valueOf(0L);
        }
        if ("F".equals(returnType)) {
            return Float.valueOf(0f);
        }
        if ("D".equals(returnType)) {
            return Double.valueOf(0d);
        }
        if ("Ljava/lang/String;".equals(returnType)) {
            return "";
        }
        if ("[J".equals(returnType)) {
            return new long[0];
        }
        if ("[I".equals(returnType)) {
            return new int[0];
        }
        if ("[Ljava/lang/String;".equals(returnType)) {
            return new String[0];
        }
        if ("[B".equals(returnType)) {
            return new byte[0];
        }
        return null;
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    static int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    static boolean booleanValue(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    private static boolean accepts(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!accepts(parameterTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepts(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        if (!parameterType.isPrimitive()) {
            return parameterType.isInstance(arg);
        }
        return primitiveWrapper(parameterType).isInstance(arg);
    }

    private static Class<?> primitiveWrapper(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == char.class) return Character.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        return Void.class;
    }

    private static void call(Object receiver, Method method, Object[] args) {
        try {
            method.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    static long exceptionHandle(Throwable throwable) {
        return put(throwable);
    }

    static Throwable throwable(long handle) {
        return get(handle, Throwable.class);
    }

    static byte[] utf8(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
