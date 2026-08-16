package app.revanced.extension.samsungkeyboard;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public final class WindowCompat {
    private static final boolean ONE_UI = detectOneUi();
    private static volatile WeakReference<InputMethodService> inputMethodService = new WeakReference<>(null);
    private static volatile WeakReference<View> inputView = new WeakReference<>(null);

    private WindowCompat() {
    }

    public static void initialize(InputMethodService service) {
        inputMethodService = new WeakReference<>(service);
    }

    public static void captureInputView(View view) {
        inputView = new WeakReference<>(view);
    }

    public static void setFlags(Window window, int flags, int mask) {
        if (ONE_UI) {
            window.setFlags(flags, mask);
            return;
        }

        int safeMask = mask & ~WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (safeMask != 0) window.setFlags(flags, safeMask);
        if ((mask & WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS) != 0) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    public static void setType(Window window, int type) {
        if (ONE_UI || type != WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG) {
            window.setType(type);
            return;
        }

        WindowManager.LayoutParams attributes = window.getAttributes();
        if (useAttachedDialog(attributes)) {
            window.setAttributes(attributes);
        } else {
            window.setType(type);
        }
    }

    public static void show(Dialog dialog) {
        if (!ONE_UI) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                if (convertInputMethodDialog(attributes)) {
                    window.setAttributes(attributes);
                }
            }
        }
        dialog.show();
    }

    public static void addView(ViewManager manager, View view, ViewGroup.LayoutParams params) {
        if (!ONE_UI && params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams attributes = (WindowManager.LayoutParams) params;
            convertInputMethodDialog(attributes);
        }
        manager.addView(view, params);
    }

    private static boolean convertInputMethodDialog(WindowManager.LayoutParams attributes) {
        return attributes.type == WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG &&
                useAttachedDialog(attributes);
    }

    private static boolean useAttachedDialog(WindowManager.LayoutParams attributes) {
        IBinder token = getInputMethodWindowToken();
        if (token == null) return false;

        attributes.token = token;
        attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        return true;
    }

    private static IBinder getInputMethodWindowToken() {
        View view = inputView.get();
        IBinder token = view == null ? null : view.getWindowToken();
        if (token != null) return token;

        InputMethodService service = inputMethodService.get();
        if (service == null) return null;

        Dialog dialog = service.getWindow();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window == null) return null;

        View decorView = window.peekDecorView();
        return decorView == null ? null : decorView.getWindowToken();
    }

    @SuppressLint("PrivateApi")
    private static boolean detectOneUi() {
        if (!"samsung".equalsIgnoreCase(Build.MANUFACTURER)) return false;
        try {
            Class.forName("android.os.SemSystemProperties");
            return true;
        } catch (ClassNotFoundException | SecurityException ignored) {
            return false;
        }
    }
}