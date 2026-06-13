package app.revanced.extension.kakaotalk.chatroom;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Enumeration;

import dalvik.system.DexFile;

public final class ChannelIdInfoExtension {
    private static final String LABEL_KEY = "morphe_kakaotalk_channel_id_label";
    private static final String COPIED_KEY = "morphe_kakaotalk_channel_id_copied";

    private static WeakReference<Context> contextReference;
    private static long currentChannelId;
    private static Method clickableModifierMethod;
    private static Class<?> function0Class;
    private static Object unitInstance;

    private ChannelIdInfoExtension() {
    }

    public static void setCurrentChannelId(Activity activity, long channelId) {
        currentChannelId = channelId;
        if (activity != null) {
            contextReference = new WeakReference<Context>(activity);
        }
    }

    public static Object makeCopyableTitleModifier(Object modifier) {
        if (modifier == null || currentChannelId <= 0L) return modifier;

        try {
            Method method = resolveClickableModifierMethod(modifier);
            if (method == null) return modifier;

            Class<?> function0 = resolveFunction0Class();
            Object onClick = Proxy.newProxyInstance(
                    function0.getClassLoader(),
                    new Class<?>[]{function0},
                    new CopyCurrentChannelIdInvocationHandler()
            );
            return method.invoke(null, modifier, null, null, true, null, null, onClick, 0x1c, null);
        } catch (Throwable ignored) {
            return modifier;
        }
    }

    public static void bind(View root, long channelId) {
        if (root == null || channelId <= 0L) return;

        TextView title = findTextView(root, "profile_title");
        if (title == null) return;

        String label = stringOrDefault(LABEL_KEY, "Channel ID");
        String currentTitle = removePreviousChannelIdLine(title.getText(), label);
        String channelLine = label + " " + channelId;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(currentTitle);
        builder.append('\n');

        int channelLineStart = builder.length();
        builder.append(channelLine);
        builder.setSpan(new RelativeSizeSpan(0.72f), channelLineStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new StyleSpan(Typeface.NORMAL), channelLineStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(resolveSubTextColor(title)), channelLineStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        title.setSingleLine(false);
        title.setMaxLines(3);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setText(builder);
        title.setContentDescription(currentTitle + ", " + channelLine);
        title.setOnClickListener(new CopyClickListener(channelId));
        title.setClickable(true);
    }

    private static TextView findTextView(View root, String name) {
        Context context = root.getContext();
        int id = root.getResources().getIdentifier(name, "id", context.getPackageName());
        if (id == 0) return null;

        View view = root.findViewById(id);
        return view instanceof TextView ? (TextView) view : null;
    }

    private static String removePreviousChannelIdLine(CharSequence text, String label) {
        if (text == null) return "";

        String value = text.toString();
        String marker = "\n" + label + " ";
        int markerIndex = value.indexOf(marker);
        return markerIndex >= 0 ? value.substring(0, markerIndex) : value;
    }

    private static int resolveSubTextColor(TextView view) {
        Context context = view.getContext();
        int colorId = view.getResources().getIdentifier("daynight_gray550s", "color", context.getPackageName());
        if (colorId != 0) {
            try {
                return context.getColor(colorId);
            } catch (Throwable ignored) {
            }
        }
        return Color.GRAY;
    }

    private static Method resolveClickableModifierMethod(Object modifier) {
        if (clickableModifierMethod != null) return clickableModifierMethod;

        Method method = findClickableModifierMethodByName("androidx.compose.foundation.ClickableKt", modifier);
        if (method == null) {
            method = findClickableModifierMethodFromDex(modifier);
        }
        if (method != null) {
            method.setAccessible(true);
            clickableModifierMethod = method;
        }
        return method;
    }

    private static Method findClickableModifierMethodByName(String className, Object modifier) {
        try {
            return findClickableModifierMethod(Class.forName(className, false, modifier.getClass().getClassLoader()), modifier);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findClickableModifierMethodFromDex(Object modifier) {
        Context context = contextReference != null ? contextReference.get() : null;
        if (context == null) return null;

        DexFile dexFile = null;
        try {
            dexFile = new DexFile(context.getApplicationInfo().sourceDir);
            Enumeration<String> entries = dexFile.entries();
            ClassLoader classLoader = modifier.getClass().getClassLoader();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                if (!className.startsWith("androidx.compose.foundation.")) continue;

                Method method = findClickableModifierMethodByName(className, modifier);
                if (method != null && method.getDeclaringClass().getClassLoader() == classLoader) {
                    return method;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Method findClickableModifierMethod(Class<?> clazz, Object modifier) throws ClassNotFoundException {
        Class<?> function0 = resolveFunction0Class();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 9) continue;
            if (!parameters[0].isInstance(modifier)) continue;
            if (parameters[3] != Boolean.TYPE) continue;
            if (!function0.isAssignableFrom(parameters[6])) continue;
            if (parameters[7] != Integer.TYPE) continue;
            if (parameters[8] != Object.class) continue;
            if (!parameters[0].isAssignableFrom(method.getReturnType())) continue;

            return method;
        }
        return null;
    }

    private static Class<?> resolveFunction0Class() throws ClassNotFoundException {
        if (function0Class == null) {
            function0Class = Class.forName("kotlin.jvm.functions.Function0");
        }
        return function0Class;
    }

    private static Object unitInstance() {
        if (unitInstance != null) return unitInstance;

        try {
            Class<?> unitClass = Class.forName("kotlin.Unit");
            for (Field field : unitClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == unitClass) {
                    field.setAccessible(true);
                    unitInstance = field.get(null);
                    return unitInstance;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void copyCurrentChannelId() {
        Context context = contextReference != null ? contextReference.get() : null;
        copyChannelId(context, currentChannelId);
    }

    private static void copyChannelId(Context context, long channelId) {
        if (context == null || channelId <= 0L) return;

        String channelIdText = Long.toString(channelId);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(stringOrDefault(LABEL_KEY, "Channel ID"), channelIdText));
        }
        Toast.makeText(context, stringOrDefault(COPIED_KEY, "Channel ID copied"), Toast.LENGTH_SHORT).show();
    }

    private static String stringOrDefault(String key, String fallback) {
        try {
            String value = str(key);
            if (value != null && value.length() > 0) return value;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static final class CopyCurrentChannelIdInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("invoke".equals(name)) {
                copyCurrentChannelId();
                return unitInstance();
            }
            if ("toString".equals(name)) return "CopyChannelId";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return args != null && args.length > 0 && proxy == args[0];
            return null;
        }
    }

    private static final class CopyClickListener implements View.OnClickListener {
        private final long channelId;

        private CopyClickListener(long channelId) {
            this.channelId = channelId;
        }

        @Override
        public void onClick(View view) {
            copyChannelId(view.getContext(), channelId);
        }
    }
}