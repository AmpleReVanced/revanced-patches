package app.revanced.extension.unicorn.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class NativeFallback {
    private static final Map<String, Object> COMPANIONS = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private NativeFallback() {
    }

    static Object call(String owner, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return method(owner, methodName, parameterTypes).invoke(companion(owner), args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot call original native " + owner + "." + methodName, exception);
        }
    }

    private static Object companion(String owner) throws ReflectiveOperationException {
        Object existing = COMPANIONS.get(owner);
        if (existing != null) {
            return existing;
        }

        Object loaded = loadCompanion(owner);
        Object cached = COMPANIONS.putIfAbsent(owner, loaded);
        return cached == null ? loaded : cached;
    }

    private static Object loadCompanion(String owner) throws ReflectiveOperationException {
        String companionName = binaryName(owner);
        String outerName = companionName.endsWith("$Companion")
                ? companionName.substring(0, companionName.length() - "$Companion".length())
                : companionName;

        Class<?> outerClass = Class.forName(outerName);
        Field field = outerClass.getDeclaredField("Companion");
        field.setAccessible(true);
        return field.get(null);
    }

    private static Method method(String owner, String methodName, Class<?>[] parameterTypes)
            throws ReflectiveOperationException {
        String key = key(owner, methodName, parameterTypes);
        Method existing = METHODS.get(key);
        if (existing != null) {
            return existing;
        }

        Method loaded = companion(owner).getClass().getDeclaredMethod(methodName, parameterTypes);
        loaded.setAccessible(true);
        Method cached = METHODS.putIfAbsent(key, loaded);
        return cached == null ? loaded : cached;
    }

    private static String binaryName(String owner) {
        if (owner == null || !owner.startsWith("L") || !owner.endsWith(";")) {
            throw new IllegalArgumentException("Invalid owner descriptor: " + owner);
        }
        return owner.substring(1, owner.length() - 1).replace('/', '.');
    }

    private static String key(String owner, String methodName, Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder(owner).append('#').append(methodName).append('(');
        for (Class<?> parameterType : parameterTypes) {
            builder.append(parameterType.getName()).append(',');
        }
        return builder.append(')').toString();
    }
}
