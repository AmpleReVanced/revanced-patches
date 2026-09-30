package app.revanced.extension.kakaotalk.chatlog.details;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.morphe.extension.shared.StringRef;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class MessageDetailsExtension {
    static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Morphe message details");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MessageDetailsExtension() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static String menuTitle() {
        return StringRef.str("morphe_kakaotalk_message_details_title");
    }

    public static Set<?> appendAction(Set<?> actions, Object message, Object chatRoom) {
        if (!Settings.SHOW_MESSAGE_DETAILS.get() || !(message instanceof MessageDetailsSource)) {
            return actions;
        }
        Set<Object> result = new LinkedHashSet<>(actions);
        result.add(new MessageDetailsAction(message, chatRoom));
        return result;
    }

    public static void open(Activity activity, MessageDetailsSource message) {
        WORKER.execute(() -> {
            try {
                String json = MessageDetailsJson.capture(message).serialize();
                String token = MessageDetailsStore.write(activity.getApplicationContext(), json);
                MAIN.post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        MessageDetailsActivity.start(activity, token);
                    }
                });
            } catch (Exception exception) {
                MAIN.post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        Toast.makeText(activity, StringRef.str("morphe_kakaotalk_message_details_error"), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}