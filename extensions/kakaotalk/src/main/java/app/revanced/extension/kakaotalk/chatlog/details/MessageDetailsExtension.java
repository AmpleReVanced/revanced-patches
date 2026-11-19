package app.revanced.extension.kakaotalk.chatlog.details;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
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
    private static final int MENU_ITEM_ID = View.generateViewId();

    private MessageDetailsExtension() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static String menuTitle() {
        return StringRef.str("morphe_kakaotalk_message_details_title");
    }

    public static List<?> prependAction(List<?> actions, Object message, Object chatRoom) {
        if (!canInspect(message)) {
            return actions;
        }
        List<Object> result = new ArrayList<>(actions.size() + 1);
        result.add(new MessageDetailsAction(message, chatRoom));
        result.addAll(actions);
        return result;
    }

    public static void bindMenu(View view, MessageDetailsViewHolder holder) {
        if (!Settings.SHOW_MESSAGE_DETAILS.get()) return;
        bindMenu(view, (menu, target, info) -> {
            Object message = holder.getMessageDetailsSource();
            Activity activity = findActivity(target.getContext());
            if (canInspect(message) && activity != null && menu.findItem(MENU_ITEM_ID) == null) {
                menu.add(Menu.NONE, MENU_ITEM_ID, 0, menuTitle()).setOnMenuItemClickListener(item -> {
                    open(activity, (MessageDetailsSource) message);
                    return true;
                });
            }
        });
    }

    private static void bindMenu(View view, View.OnCreateContextMenuListener listener) {
        view.setOnCreateContextMenuListener(listener);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                bindMenu(group.getChildAt(i), listener);
            }
        }
    }

    public static boolean showMenu(View view, MessageDetailsViewHolder holder) {
        return showMenu(view.getContext(), holder.getMessageDetailsSource());
    }

    public static boolean showMenu(Context context, Object message) {
        if (!canInspect(message)) return false;
        Activity activity = findActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        new AlertDialog.Builder(context)
                .setItems(new CharSequence[]{menuTitle()}, (dialog, which) ->
                        open(activity, (MessageDetailsSource) message))
                .show();
        return true;
    }

    private static boolean canInspect(Object message) {
        return message instanceof MessageDetailsSource && Settings.SHOW_MESSAGE_DETAILS.get();
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
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