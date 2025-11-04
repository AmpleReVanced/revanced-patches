package app.revanced.extension.kakaotalk.packet;

import dw0.j;

public class LocoRequest {

    public static Jp.j makeRequest(
            int packetId,
            short status,
            Jp.c method,
            Jp.a body
    ) {
        return new Jp.j(
                new Jp.b(packetId, status, method, new j().a(body.a).length),
                body
        );
    }

}
