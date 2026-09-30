package app.revanced.extension.kakaotalk.chatlog.details;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

final class MessageDetailsStore {
    private static final long RETENTION_MS = 60 * 60 * 1000L;

    private MessageDetailsStore() {
    }

    static String write(Context context, String json) throws IOException {
        File directory = directory(context);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create message details directory");
        }
        File[] previous = directory.listFiles();
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        if (previous != null) {
            for (File file : previous) {
                if (file.getName().endsWith(".json") && file.lastModified() < cutoff) {
                    file.delete();
                }
            }
        }
        String token = UUID.randomUUID().toString();
        File file = file(context, token);
        try {
            Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            file.delete();
            throw exception;
        }
        return token;
    }

    static String read(Context context, String token) throws IOException {
        return new String(Files.readAllBytes(file(context, token).toPath()), StandardCharsets.UTF_8);
    }

    private static File file(Context context, String token) throws IOException {
        if (token == null || !token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IOException("Invalid message details token");
        }
        return new File(directory(context), token + ".json");
    }

    private static File directory(Context context) {
        return new File(context.getCacheDir(), "morphe-message-details");
    }
}