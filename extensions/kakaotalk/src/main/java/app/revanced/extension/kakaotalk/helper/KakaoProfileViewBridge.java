package app.revanced.extension.kakaotalk.helper;

import android.content.Context;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflective access to the app's ProfileView, so a signature change breaks one place, not each screen.
 */
public final class KakaoProfileViewBridge {
    private static Constructor<?> constructor;
    private static Method loadMethod;
    private static boolean resolved;

    private KakaoProfileViewBridge() {
    }

    public static View create(Context context) {
        resolve();
        if (constructor == null) return null;

        try {
            Object instance = constructor.newInstance(context);
            return instance instanceof View ? (View) instance : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean load(View profileView, long userId, String imageUrl, int imageType) {
        resolve();
        if (loadMethod == null || profileView == null || userId <= 0L) return false;

        try {
            loadMethod.invoke(profileView, userId, imageUrl, imageType);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;

        try {
            Class<?> type = Class.forName("com.kakao.talk.widget.ProfileView");
            constructor = type.getConstructor(Context.class);
            loadMethod = type.getMethod("load", long.class, String.class, int.class);
        } catch (Throwable ignored) {
            constructor = null;
            loadMethod = null;
        }
    }
}