package app.revanced.extension.kakaotalk.packet;

import android.util.Log;
import org.json.JSONObject;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

import java.lang.reflect.Method;

public class PacketHook {

    private static final String TAG = "PacketHook";
    private static final String TARGET_CLASS = "Jp.p";
    private static final String TARGET_METHOD = "S";

    private final Serializer serializer;

    public PacketHook(Serializer serializer) {
        this.serializer = serializer;
    }

    public void install() {
        try {
            Class<?> targetClass = Class.forName(TARGET_CLASS);
            Method targetMethod = findMethod(targetClass, TARGET_METHOD);

            if (targetMethod == null) {
                Log.e(TAG, "Method not found: " + TARGET_METHOD);
                return;
            }

            Pine.hook(targetMethod, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    handlePacket((Jp.k) callFrame.args[1]);
                }
            });

            Log.i(TAG, "Packet hook installed successfully");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Target class not found", e);
        }
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private void handlePacket(Jp.k packet) {
        try {
            JSONObject packetJson = serializer.packetToJson(packet);
            PacketHandlerServer.sendToAll(serializer.jsonToBytes(packetJson));
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle packet", e);
        }
    }
}
