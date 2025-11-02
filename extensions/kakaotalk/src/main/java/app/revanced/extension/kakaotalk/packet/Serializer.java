package app.revanced.extension.kakaotalk.packet;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class Serializer {

    public JSONObject packetToJson(Jp.e packet) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("header", serializeHeader(packet.a));
        json.put("body", serializeBody(packet.b));
        return json;
    }

    public byte[] jsonToBytes(JSONObject json) {
        String jsonString = json.toString() + "\n";
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    private JSONObject serializeHeader(Jp.b header) throws JSONException {
        if (header == null) return null;

        JSONObject json = new JSONObject();
        json.put("packetId", header.a);
        json.put("status", header.b);
        json.put("method", header.c.name());
        json.put("bodyLength", header.d);
        return json;
    }

    private JSONObject serializeBody(Jp.a body) {
        if (body == null) return null;
        return new JSONObject(body.a().toMap());
    }
}
