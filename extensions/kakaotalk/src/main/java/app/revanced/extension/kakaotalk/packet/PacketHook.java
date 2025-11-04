package app.revanced.extension.kakaotalk.packet;

import android.util.Log;
import org.json.JSONObject;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

import java.lang.reflect.Method;

public class PacketHook {

    private static final String TAG = "PacketHook";
    private static final String TARGET_CLASS_1 = "Jp.p"; // for response without request
    private static final String TARGET_METHOD_1 = "S";
    private static final String TARGET_CLASS_2 = "Gp.g$b"; // for response with request
    private static final String TARGET_METHOD_2 = "b";

    private final Serializer serializer;

    public PacketHook(Serializer serializer) {
        this.serializer = serializer;
    }

    public void install() {
        try {
            Class<?> targetClass1 = Class.forName(TARGET_CLASS_1);
            Method targetMethod1 = findMethod(targetClass1, TARGET_METHOD_1);

            if (targetMethod1 == null) {
                Log.e(TAG, "Method not found: " + TARGET_METHOD_1);
                return;
            }

            Pine.hook(targetMethod1, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    handlePacket((Jp.k) callFrame.args[1]);
                }
            });

            Class<?> targetClass2 = Class.forName(TARGET_CLASS_2);
            Method targetMethod2 = findMethod(targetClass2, TARGET_METHOD_2);

            if (targetMethod2 == null) {
                Log.e(TAG, "Method not found: " + TARGET_METHOD_2);
                return;
            }

            Pine.hook(targetMethod2, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    handlePacket((Jp.k) callFrame.args[0]);
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
