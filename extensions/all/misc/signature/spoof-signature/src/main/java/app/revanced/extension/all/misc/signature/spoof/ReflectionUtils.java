package app.revanced.extension.all.misc.signature.spoof;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helpers used by the signature spoof extension.
 *
 * <p>All accessors use {@link Field#setAccessible(boolean)} and fall back to alternative field
 * names found in different Android versions, so the code keeps working across OEM changes to the
 * hidden {@code android.content.pm} classes.</p>
 */
final class ReflectionUtils {

    private ReflectionUtils() {
    }

    static Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object[] args)
            throws Exception {
        Method method = Class.forName(className).getDeclaredMethod(methodName, parameterTypes);
        makeAccessible(method);
        return method.invoke(null, args);
    }

    static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        makeAccessible(method);
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    static Object getStaticField(String className, String fieldName) throws Exception {
        return findField(Class.forName(className), fieldName).get(null);
    }

    static void setStaticField(String className, String fieldName, Object value) throws Exception {
        findField(Class.forName(className), fieldName).set(null, value);
    }

    static boolean setField(Object target, Object value, String... fieldNames) {
        if (target == null) return false;
        for (String name : fieldNames) {
            Field field = findFieldOrNull(target.getClass(), name);
            if (field == null) continue;
            try {
                field.set(target, value);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    static Object getFieldOrNull(Object target, String... fieldNames) {
        if (target == null) return null;
        for (String name : fieldNames) {
            Field field = findFieldOrNull(target.getClass(), name);
            if (field == null) continue;
            try {
                return field.get(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> declaringClass, String name) throws NoSuchFieldException {
        Field field = findFieldOrNull(declaringClass, name);
        if (field == null) throw new NoSuchFieldException(name);
        return field;
    }

    private static Field findFieldOrNull(Class<?> declaringClass, String name) {
        Class<?> current = declaringClass;
        while (current != null) {
            Field field;
            try {
                field = current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
                continue;
            }
            makeAccessible(field);
            return field;
        }
        return null;
    }

    private static void makeAccessible(java.lang.reflect.AccessibleObject accessible) {
        try {
            accessible.setAccessible(true);
        } catch (Exception ignored) {
        }
    }
}