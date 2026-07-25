package app.revanced.extension.kakaotalk.packet;

import android.util.Log;
import org.json.JSONObject;

public class PacketHook {

    private static final String TAG = "PacketHook";
    private static volatile Serializer serializer = new Serializer();

    public static void initialize(Serializer serializer) {
        PacketHook.serializer = serializer;
    }

    public static void handlePacket(Object packet) {
        try {
            JSONObject packetJson = serializer.packetToJson(packet);
            PacketHandlerServer.sendToAll(serializer.jsonToBytes(packetJson));
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle packet", e);
        }
    }
}
