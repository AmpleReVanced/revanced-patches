package app.revanced.extension.kakaotalk.packet;

import org.json.JSONObject;

public interface PacketInterceptor {
    void onPacketReceived(JSONObject packet) throws Exception;
}
