package app.revanced.extension.kakaotalk.chatlog.details;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Insets;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
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
    private static final WeakHashMap<View, Boolean> originalLongClickable = new WeakHashMap<>();
    private static final Set<View> systemBackGestures = Collections.newSetFromMap(new WeakHashMap<>());

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

    public static void bindFallbackMenu(View row, MessageDetailsViewHolder holder) {
        if (!canInspect(holder.getMessageDetailsSource())) {
            Boolean previous = originalLongClickable.remove(row);
            if (previous != null) {
                row.setOnCreateContextMenuListener(null);
                row.setLongClickable(previous);
            }
            return;
        }
        if (originalLongClickable.putIfAbsent(row, row.isLongClickable()) != null) return;
        row.setOnCreateContextMenuListener((menu, target, info) -> {
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

    public static void trackSystemBackGesture(View list, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && startsAtBackEdge(list, event)) {
            systemBackGestures.add(list);
        } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            systemBackGestures.remove(list);
        }
    }

    public static boolean isSystemBackGesture(View list) {
        return systemBackGestures.contains(list);
    }

    private static boolean startsAtBackEdge(View list, MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)) return false;
        WindowInsets insets = list.getRootWindowInsets();
        if (insets == null) return false;
        Insets gestures = insets.getSystemGestureInsets();
        if (gestures.left == 0 && gestures.right == 0) return false;
        int[] location = new int[2];
        list.getLocationInWindow(location);
        float x = location[0] + event.getX();
        return (gestures.left > 0 && x < gestures.left)
                || (gestures.right > 0 && x >= list.getRootView().getWidth() - gestures.right);
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