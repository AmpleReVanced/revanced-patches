package app.revanced.extension.kakaotalk.packet;

import android.content.Context;
import android.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class HookInitializer implements B5.b<Void> {

    private static final String TAG = "HookInitializer";
    private static final int SERVER_PORT = 8080;

    @NotNull
    @Override
    public Void create(@NotNull Context context) {
        Serializer serializer = new Serializer();
        RemotePacketHandler remoteHandler = new RemotePacketHandler();

        PacketHook.initialize(serializer);
        startServer(remoteHandler);

        return null;
    }

    private void startServer(RemotePacketHandler handler) {
        PacketHandlerServer.setRequestHandler(handler);

        new Thread(() -> {
            try {
                new PacketHandlerServer(SERVER_PORT).run();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start server", e);
            }
        }).start();
    }

    @NotNull
    @Override
    public List<Class<? extends B5.b<?>>> dependencies() {
        return Collections.emptyList();
    }
}
