package app.revanced.extension.unicorn.core;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ConfigNative {
    private static final String[] ROOT_SECTIONS = {
            "api",
            "app",
            "contentBlock",
            "dpiBypass",
            "filter",
            "license",
            "localServer",
            "networkTraffic",
            "ssl",
            "vpn",
            "vpnClient",
    };

    private ConfigNative() {
    }

    static boolean handles(String owner) {
        return NativeOwners.CONFIG.equals(owner) || owner.startsWith(NativeOwners.CONFIG_PREFIX);
    }

    static Object call(String owner, String method, String returnType, Object[] args) {
        if (NativeOwners.CONFIG.equals(owner)) {
            return callRoot(method, args);
        }
        if (NativeOwners.CONFIG_FILE.equals(owner)) {
            return fileValue(method);
        }
        return callConfigObject(owner, method, returnType, args);
    }

    private static Object callRoot(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return NativeRuntime.handle(rootConfig());
        }

        long handle = NativeRuntime.longValue(args[0]);
        ConfigObject root = NativeRuntime.get(handle, ConfigObject.class);

        switch (method) {
            case "native_Reset":
                root.clear();
                return null;
            case "native_delete":
                NativeRuntime.delete(handle);
                return null;
            default:
                if (method.startsWith("native_Get")) {
                    return Long.valueOf(root.getLong(NativeRuntime.propertyName(method, "native_Get"), 0L));
                }
                return Long.valueOf(0L);
        }
    }

    private static ConfigObject rootConfig() {
        ConfigObject root = new ConfigObject("root");
        for (String section : ROOT_SECTIONS) {
            root.set(section, NativeRuntime.handle(new ConfigObject(section)));
        }
        return root;
    }

    private static Object callConfigObject(String owner, String method, String returnType, Object[] args) {
        String component = NativeRuntime.componentName(owner);
        if ("native_new".equals(method)) {
            return NativeRuntime.handle(new ConfigObject(component));
        }
        if ("native_delete".equals(method)) {
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }
        if (method.startsWith("native_New")) {
            return NativeRuntime.handle(new ConfigObject(component + "." + method.substring("native_New".length())));
        }

        if (args.length == 0) {
            return NativeRuntime.defaultValue(returnType);
        }

        ConfigObject object = NativeRuntime.get(NativeRuntime.longValue(args[0]), ConfigObject.class);
        if (method.startsWith("native_Get")) {
            String key = NativeRuntime.propertyName(method, "native_Get");
            if ("Z".equals(returnType)) return Boolean.valueOf(object.getBoolean(key, false));
            if ("I".equals(returnType)) return Integer.valueOf(object.getInt(key, 0));
            if ("J".equals(returnType)) return Long.valueOf(object.getLong(key, 0L));
            if ("Ljava/lang/String;".equals(returnType)) return object.getString(key, "");
            return NativeRuntime.defaultValue(returnType);
        }
        if (method.startsWith("native_Set")) {
            String key = NativeRuntime.propertyName(method, "native_Set");
            if (args.length > 1) {
                object.set(key, args[1]);
            }
            return null;
        }

        return NativeRuntime.defaultValue(returnType);
    }

    private static String fileValue(String method) {
        if (!method.startsWith("native_")) {
            return "";
        }
        String name = method.substring("native_".length());
        if (name.endsWith("Directory")) {
            return name.substring(0, name.length() - "Directory".length()).toLowerCase(Locale.US);
        }
        return name.toLowerCase(Locale.US);
    }

    static final class ConfigObject {
        private final String component;
        private final Map<String, Object> values = new ConcurrentHashMap<>();

        ConfigObject(String component) {
            this.component = component;
        }

        boolean getBoolean(String key, boolean defaultValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defaultValue;
        }

        int getInt(String key, int defaultValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).intValue() : defaultValue;
        }

        long getLong(String key, long defaultValue) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).longValue() : defaultValue;
        }

        String getString(String key, String defaultValue) {
            Object value = values.get(key);
            return value == null ? defaultValue : String.valueOf(value);
        }

        void set(String key, Object value) {
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
        }

        void clear() {
            values.clear();
        }

        @Override
        public String toString() {
            return "ConfigObject{" + component + '}';
        }
    }
}
