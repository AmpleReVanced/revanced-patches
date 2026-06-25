package app.morphe.extension.shared.settings.preference;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toolbar;

@SuppressWarnings("unused")
public final class SettingsActivityLayout {
    private static final int SETTINGS_FRAGMENT_CONTAINER_ID = 0x00f00001;

    private SettingsActivityLayout() {
    }

    public static int setContentView(Activity activity, CharSequence title) {
        activity.setTitle(title);

        int backgroundColor = resolveBackgroundColor(activity);
        int foregroundColor = resolveForegroundColor(activity, backgroundColor);

        activity.getWindow().setStatusBarColor(backgroundColor);
        activity.getWindow().setNavigationBarColor(backgroundColor);
        applySystemBarIcons(activity, isLight(backgroundColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(true);
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor);
        root.setFitsSystemWindows(true);
        root.setTransitionGroup(true);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        applySystemWindowInsets(root);

        Toolbar toolbar = new Toolbar(activity);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(foregroundColor);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setNavigationIcon(createBackArrowDrawable(activity, foregroundColor));
        toolbar.setNavigationOnClickListener(view -> activity.finish());

        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resolveToolbarHeight(activity)
        ));

        FrameLayout container = new FrameLayout(activity);
        container.setId(SETTINGS_FRAGMENT_CONTAINER_ID);
        container.setBackgroundColor(backgroundColor);
        root.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        activity.setContentView(root);
        root.requestApplyInsets();
        return SETTINGS_FRAGMENT_CONTAINER_ID;
    }

    static int resolveToolbarHeight(Context context) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    value.data,
                    context.getResources().getDisplayMetrics()
            );
        }
        return dp(context, 56);
    }

    static int resolveBackgroundColor(Context context) {
        int defaultBackgroundColor = isNightMode(context) ? Color.BLACK : Color.WHITE;
        return resolveColor(context, android.R.attr.windowBackground, defaultBackgroundColor);
    }

    static int resolveForegroundColor(Context context, int backgroundColor) {
        return resolveColor(
                context,
                android.R.attr.textColorPrimary,
                isLight(backgroundColor) ? Color.BLACK : Color.WHITE
        );
    }

    static Drawable createBackArrowDrawable(Context context, int color) {
        return new BackArrowDrawable(context, color);
    }

    private static int resolveColor(Context context, int attribute, int fallback) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attribute});
        try {
            return typedArray.getColor(0, fallback);
        } finally {
            typedArray.recycle();
        }
    }

    private static void applySystemBarIcons(Activity activity, boolean light) {
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        flags &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        flags &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
        flags &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    static void applySystemWindowInsets(View view) {
        int paddingLeft = view.getPaddingLeft();
        int paddingTop = view.getPaddingTop();
        int paddingRight = view.getPaddingRight();
        int paddingBottom = view.getPaddingBottom();

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    paddingLeft + insets.getSystemWindowInsetLeft(),
                    paddingTop + insets.getSystemWindowInsetTop(),
                    paddingRight + insets.getSystemWindowInsetRight(),
                    paddingBottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        view.requestApplyInsets();
    }

    private static boolean isLight(int color) {
        double luminance = (0.299 * Color.red(color))
                + (0.587 * Color.green(color))
                + (0.114 * Color.blue(color));
        return luminance >= 186;
    }

    private static boolean isNightMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    private static final class BackArrowDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int size;

        private BackArrowDrawable(Context context, int color) {
            size = dp(context, 24);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(context, 2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(color);
        }

        @Override
        public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            float centerY = bounds.exactCenterY();
            float left = bounds.left + (bounds.width() * 0.30f);
            float right = bounds.right - (bounds.width() * 0.25f);
            float headX = bounds.left + (bounds.width() * 0.48f);
            float headOffset = bounds.height() * 0.24f;

            canvas.drawLine(left, centerY, right, centerY, paint);
            canvas.drawLine(left, centerY, headX, centerY - headOffset, paint);
            canvas.drawLine(left, centerY, headX, centerY + headOffset, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }
    }
}
