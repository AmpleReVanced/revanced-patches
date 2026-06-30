package app.revanced.extension.kakaotalk.packet;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RemotePacketHandler implements PacketHandlerServer.PacketRequestHandler {

    private static final String TAG = "RemotePacketHandler";

    private static volatile String locoMethodClassName;
    private static volatile String locoReqBuilderClassName;
    private static volatile Object locoConnection;

    public static void initializeLoco(String methodClassDescriptor, String reqBuilderClassDescriptor, Object connection) {
        locoMethodClassName = descriptorToClassName(methodClassDescriptor);
        locoReqBuilderClassName = descriptorToClassName(reqBuilderClassDescriptor);
        locoConnection = connection;
    }

    @Override
    public JSONObject handleRequest(JSONObject packet) {
        try {
            Object request = createLocoRequest(packet);
            Object client = findActiveClient(request);
            Method sendMethod = findSendMethod(client.getClass(), request.getClass());
            if (sendMethod == null) {
                throw new IllegalStateException("Could not find Loco request send method");
            }

            sendMethod.setAccessible(true);
            sendMethod.invoke(client, request);

            return createSuccessResponse();
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle remote request", e);
            return createErrorResponse(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        }
    }

    private Object createLocoRequest(JSONObject packet) throws Exception {
        String methodName = getMethodName(packet);
        int packetId = getPacketId(packet);
        short status = (short) getStatus(packet);
        JSONObject body = packet.optJSONObject("body");

        Object method = getLocoMethod(methodName);
        Class<?> builderClass = getClass(locoReqBuilderClassName, "LocoReq.Builder");
        Constructor<?> constructor = findBuilderConstructor(builderClass, method.getClass());
        Method addBodyValue = findBuilderAddMethod(builderClass);

        Object builder = constructor.newInstance(method, packetId, status);
        if (body != null) {
            Iterator<String> keys = body.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                addBodyValue.invoke(builder, key, toLocoValue(body.get(key)));
            }
        }

        return buildRequest(builder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object getLocoMethod(String methodName) throws Exception {
        Class<?> methodClass = getClass(locoMethodClassName, "LocoMethod");
        return Enum.valueOf((Class) methodClass.asSubclass(Enum.class), methodName.toUpperCase(Locale.ROOT));
    }

    private Constructor<?> findBuilderConstructor(Class<?> builderClass, Class<?> methodClass) throws NoSuchMethodException {
        for (Constructor<?> constructor : builderClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 3 &&
                    parameterTypes[0].isAssignableFrom(methodClass) &&
                    parameterTypes[1] == Integer.TYPE &&
                    parameterTypes[2] == Short.TYPE) {
                constructor.setAccessible(true);
                return constructor;
            }
        }

        throw new NoSuchMethodException("Could not find LocoReq.Builder constructor");
    }

    private Method findBuilderAddMethod(Class<?> builderClass) throws NoSuchMethodException {
        for (Method method : getInstanceMethods(builderClass)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 &&
                    parameterTypes[0] == String.class &&
                    parameterTypes[1] == Object.class &&
                    builderClass.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }

        throw new NoSuchMethodException("Could not find LocoReq.Builder body add method");
    }

    private Object buildRequest(Object builder) throws Exception {
        for (Method method : getInstanceMethods(builder.getClass())) {
            if (method.getParameterTypes().length != 0 ||
                    method.getReturnType() == Void.TYPE ||
                    method.getReturnType().isPrimitive() ||
                    method.getReturnType() == builder.getClass()) {
                continue;
            }

            method.setAccessible(true);
            Object request = method.invoke(builder);
            if (request != null && hasByteArrayMethod(request.getClass())) {
                return request;
            }
        }

        throw new NoSuchMethodException("Could not build LocoReq");
    }

    private Object findActiveClient(Object request) {
        Object connection = locoConnection;
        if (connection == null) {
            throw new IllegalStateException("Loco connection is not initialized");
        }

        for (Field field : getInstanceFields(connection.getClass())) {
            Object value = getFieldValue(field, connection);
            if (!(value instanceof Map<?, ?>)) {
                continue;
            }

            for (Object strategy : ((Map<?, ?>) value).values()) {
                Object client = findClientFromStrategy(strategy, request);
                if (client != null) {
                    return client;
                }
            }
        }

        throw new IllegalStateException("Loco client is not connected");
    }

    private Object findClientFromStrategy(Object strategy, Object request) {
        if (strategy == null) {
            return null;
        }

        for (Method method : getInstanceMethods(strategy.getClass())) {
            if (method.getParameterTypes().length != 0 ||
                    method.getReturnType() == Void.TYPE ||
                    method.getReturnType().isPrimitive()) {
                continue;
            }

            Object value = invokeMethodQuietly(method, strategy);
            if (value != null && findSendMethod(value.getClass(), request.getClass()) != null) {
                return value;
            }
        }

        return null;
    }

    private Method findSendMethod(Class<?> clientClass, Class<?> requestClass) {
        for (Method method : getInstanceMethods(clientClass)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 &&
                    method.getReturnType() == Void.TYPE &&
                    parameterTypes[0].isAssignableFrom(requestClass) &&
                    hasByteArrayMethod(parameterTypes[0])) {
                return method;
            }
        }

        return null;
    }

    private boolean hasByteArrayMethod(Class<?> type) {
        for (Method method : getInstanceMethods(type)) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == byte[].class) {
                return true;
            }
        }

        return false;
    }

    private String getMethodName(JSONObject packet) {
        String methodName = packet.optString("method", null);
        if (methodName != null && !methodName.isEmpty()) {
            return methodName;
        }

        JSONObject header = packet.optJSONObject("header");
        if (header != null) {
            methodName = header.optString("method", null);
            if (methodName != null && !methodName.isEmpty()) {
                return methodName;
            }
        }

        throw new IllegalArgumentException("Missing Loco method");
    }

    private int getPacketId(JSONObject packet) {
        if (packet.has("packetId") && !packet.isNull("packetId")) {
            return packet.optInt("packetId");
        }

        JSONObject header = packet.optJSONObject("header");
        if (header != null && header.has("packetId") && !header.isNull("packetId")) {
            return header.optInt("packetId");
        }

        return (int) (System.currentTimeMillis() & 0xFFFFFFFFL);
    }

    private int getStatus(JSONObject packet) {
        if (packet.has("status") && !packet.isNull("status")) {
            return packet.optInt("status");
        }

        JSONObject header = packet.optJSONObject("header");
        if (header != null && header.has("status") && !header.isNull("status")) {
            return header.optInt("status");
        }

        return 0;
    }

    private Object toLocoValue(Object value) throws Exception {
        if (value == JSONObject.NULL) {
            return null;
        }
        if (value instanceof JSONObject) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<String> keys = ((JSONObject) value).keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, toLocoValue(((JSONObject) value).get(key)));
            }
            return map;
        }
        if (value instanceof JSONArray) {
            List<Object> list = new ArrayList<>();
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                list.add(toLocoValue(array.get(i)));
            }
            return list;
        }
        return value;
    }

    private Class<?> getClass(String className, String label) throws ClassNotFoundException {
        if (className == null || className.isEmpty()) {
            throw new IllegalStateException(label + " is not initialized");
        }

        ClassLoader loader = RemotePacketHandler.class.getClassLoader();
        if (loader != null) {
            return Class.forName(className, false, loader);
        }
        return Class.forName(className);
    }

    private static String descriptorToClassName(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return null;
        }
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            descriptor = descriptor.substring(1, descriptor.length() - 1);
        }
        return descriptor.replace('/', '.');
    }

    private List<Field> getInstanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private List<Method> getInstanceMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (!Modifier.isStatic(modifiers) && !method.isSynthetic()) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private Object getFieldValue(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeMethodQuietly(Method method, Object instance) {
        try {
            method.setAccessible(true);
            return method.invoke(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject createSuccessResponse() {
        JSONObject response = new JSONObject();
        try {
            response.put("status", "success");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create success response", e);
        }
        return response;
    }

    private JSONObject createErrorResponse(String error) {
        JSONObject response = new JSONObject();
        try {
            response.put("status", "error");
            response.put("error", error);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create error response", e);
        }
        return response;
    }
}
