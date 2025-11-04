package app.revanced.extension.kakaotalk.packet;

import android.content.Context;
import android.util.Log;
import org.jetbrains.annotations.NotNull;
import top.canyie.pine.Pine;

import java.util.Collections;
import java.util.List;

public class HookInitializer implements B5.b<Void> {

    private static final String TAG = "HookInitializer";
    private static final int SERVER_PORT = 8080;

    @NotNull
    @Override
    public Void create(@NotNull Context context) {
        Pine.disableHiddenApiPolicy(true, true);

        Serializer serializer = new Serializer();
        RemotePacketHandler remoteHandler = new RemotePacketHandler();

        startServer(remoteHandler);
        installHook(serializer);

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

    private void installHook(Serializer serializer) {
        PacketHook hook = new PacketHook(serializer);
        hook.install();
    }

    @NotNull
    @Override
    public List<Class<? extends B5.b<?>>> dependencies() {
        return Collections.emptyList();
    }
}
