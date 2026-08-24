package app.revanced.extension.samsungkeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.lang.ref.WeakReference;

public final class WindowCompat {
    private static final String NAVIGATION_BAR_COLOR = "honey_navigation_bar_background_color";
    private static final boolean ONE_UI = detectOneUi();
    private static volatile WeakReference<InputMethodService> inputMethodService = new WeakReference<>(null);
    private static volatile WeakReference<View> inputView = new WeakReference<>(null);
    private static volatile WeakReference<EditText> settingsInput = new WeakReference<>(null);

    private WindowCompat() {
    }

    public static void initialize(InputMethodService service) {
        inputMethodService = new WeakReference<>(service);
    }

    public static void captureInputView(View view) {
        inputView = new WeakReference<>(view);
        if (!ONE_UI) updateNavigationBar(view);
    }

    public static void showSoftInput(Context context, int flags) {
        Activity activity = findActivity(context);
        if (activity == null) return;

        EditText input = settingsInput.get();
        if (input == null || input.getContext() != activity || !input.isAttachedToWindow()) {
            input = new EditText(activity);
            input.setAlpha(0f);
            input.setCursorVisible(false);
            input.setFocusableInTouchMode(true);
            input.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            activity.addContentView(input, new ViewGroup.LayoutParams(1, 1));
            settingsInput = new WeakReference<>(input);
        }

        EditText target = input;
        target.requestFocus();
        target.post(() -> {
            InputMethodManager manager = activity.getSystemService(InputMethodManager.class);
            if (manager != null) manager.showSoftInput(target, flags);
            WindowInsetsController controller = target.getWindowInsetsController();
            if (controller != null) controller.show(WindowInsets.Type.ime());
        });
    }

    public static boolean commitClipboard(ClipData data) {
        InputMethodService service = inputMethodService.get();
        if (service == null || data == null || data.getItemCount() == 0) return false;

        InputConnection connection = service.getCurrentInputConnection();
        if (connection == null) return false;
        CharSequence text = data.getItemAt(0).coerceToText(service);
        return text != null && connection.commitText(text, 1);
    }

    @SuppressWarnings("deprecation")
    public static void semOverridePendingTransition(Activity activity, int enterAnim, int exitAnim) {
        activity.overridePendingTransition(enterAnim, exitAnim);
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

        Window window = getInputMethodWindow();
        if (window == null) return null;

        View decorView = window.peekDecorView();
        return decorView == null ? null : decorView.getWindowToken();
    }

    @SuppressWarnings("deprecation")
    private static void updateNavigationBar(View view) {
        Window window = getInputMethodWindow();
        if (window == null) return;

        int resource = view.getResources().getIdentifier(
                NAVIGATION_BAR_COLOR,
                "color",
                view.getContext().getPackageName()
        );
        if (resource == 0) return;

        int color = view.getContext().getColor(resource);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setNavigationBarColor(color);
        window.setNavigationBarDividerColor(color);
        window.setNavigationBarContrastEnforced(false);

        int lightNavigationBar = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        int appearance = Color.luminance(color) > 0.5 ? lightNavigationBar : 0;
        view.post(() -> {
            WindowInsetsController controller = view.getWindowInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(appearance, lightNavigationBar);
        });
    }

    private static Window getInputMethodWindow() {
        InputMethodService service = inputMethodService.get();
        if (service == null) return null;

        Dialog dialog = service.getWindow();
        return dialog == null ? null : dialog.getWindow();
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) return null;
            context = base;
        }
        return null;
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
