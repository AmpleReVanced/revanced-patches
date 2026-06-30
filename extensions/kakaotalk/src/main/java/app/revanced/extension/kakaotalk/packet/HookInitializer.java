package app.revanced.extension.kakaotalk.packet;

import android.content.Context;
import android.util.Log;

public final class HookInitializer {

    private static final String TAG = "HookInitializer";
    private static final int SERVER_PORT = 8080;
    private static boolean initialized;

    private HookInitializer() {
    }

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        initialized = true;

        Serializer serializer = new Serializer();
        RemotePacketHandler remoteHandler = new RemotePacketHandler();

        PacketHook.initialize(serializer);
        startServer(remoteHandler);
    }

    private static void startServer(RemotePacketHandler handler) {
        PacketHandlerServer.setRequestHandler(handler);

        new Thread(() -> {
            try {
                new PacketHandlerServer(SERVER_PORT).run();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start server", e);
            }
        }).start();
    }
}
