package app.revanced.extension.kakaotalk.packet;

import Gp.d;
import Jp.c;
import android.util.Log;
import app.revanced.extension.kakaotalk.util.JsonUtils;
import com.kakao.talk.core.loco.a;
import dw0.k;
import org.json.JSONObject;

public class RemotePacketHandler implements PacketHandlerServer.PacketRequestHandler {

    private static final String TAG = "RemotePacketHandler";

    @Override
    public JSONObject handleRequest(JSONObject packet) {
        try {
            String method = packet.getString("method");
            JSONObject body = packet.getJSONObject("body");

            int packetId = (int) (System.currentTimeMillis() & 0xFFFFFFFFL);

            sendLocoRequest(packetId, MethodHelper.fromString(method), body);

            return createSuccessResponse();
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle remote request", e);
            return createErrorResponse(e.getMessage());
        }
    }

    private void sendLocoRequest(int packetId, c method, JSONObject body) throws Exception {
        Jp.j request = LocoRequest.makeRequest(
            packetId,
            (short) 0,
            method,
            new Jp.a(new k(JsonUtils.toMap(body)))
        );

        a.g().h.get(kotlin.jvm.internal.U.c(d.b.class)).n().v(request);
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
