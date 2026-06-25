/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(backgroundColor);
        int toolbarPadding = dp(activity, 15);
        toolbar.setPadding(toolbarPadding, dp(activity, 10), toolbarPadding, dp(activity, 8));

        toolbar.addView(createBackArrowView(activity, view -> activity.finish()));
        toolbar.addView(createToolbarTitle(activity, title, foregroundColor));

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
        return dp(context, 70);
    }

    static int resolveBackgroundColor(Context context) {
        return MorphePreferenceStyle.backgroundColor(context);
    }

    static int resolveForegroundColor(Context context, int backgroundColor) {
        return MorphePreferenceStyle.primaryTextColor(context);
    }

    static View createBackArrowView(Context context, View.OnClickListener onClickListener) {
        BackArrowView back = new BackArrowView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44));
        params.gravity = Gravity.CENTER_VERTICAL;
        back.setLayoutParams(params);
        back.setOnClickListener(onClickListener);
        return back;
    }

    static TextView createToolbarTitle(Context context, CharSequence title, int color) {
        TextView titleTextView = new TextView(context);
        titleTextView.setText(title);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        titleTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        titleTextView.setIncludeFontPadding(false);
        titleTextView.setTextColor(color);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = Gravity.CENTER_VERTICAL;
        titleParams.leftMargin = dp(context, 7);
        titleTextView.setLayoutParams(titleParams);
        return titleTextView;
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

    private static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    private static final class BackArrowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private BackArrowView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float centerY = getHeight() / 2f;
            float tipX = dp(getContext(), 2);
            float endX = dp(getContext(), 21);
            float headEndX = dp(getContext(), 10);
            float headOffset = dp(getContext(), 7);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 1.9f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(MorphePreferenceStyle.primaryTextColor(getContext()));

            canvas.drawLine(tipX, centerY, endX, centerY, paint);
            canvas.drawLine(tipX, centerY, headEndX, centerY - headOffset, paint);
            canvas.drawLine(tipX, centerY, headEndX, centerY + headOffset, paint);
        }
    }
}
