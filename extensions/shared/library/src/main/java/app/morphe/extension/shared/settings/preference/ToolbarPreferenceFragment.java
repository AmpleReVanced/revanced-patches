package app.morphe.extension.shared.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

@SuppressWarnings({"deprecation", "unused"})
public abstract class ToolbarPreferenceFragment extends AbstractPreferenceFragment {
    private static final String TOOLBAR_ROOT_TAG = "morphe_preference_screen_toolbar_root";

    protected void setPreferenceScreenToolbar(PreferenceScreen parentScreen) {
        if (parentScreen == null) {
            return;
        }

        for (int i = 0, count = parentScreen.getPreferenceCount(); i < count; i++) {
            Preference childPreference = parentScreen.getPreference(i);
            if (childPreference instanceof PreferenceScreen) {
                PreferenceScreen childScreen = (PreferenceScreen) childPreference;
                setPreferenceScreenToolbar(childScreen);
                childScreen.setOnPreferenceClickListener(preference -> {
                    View view = getView();
                    if (view != null) {
                        view.post(() -> addPreferenceScreenToolbar(childScreen));
                    } else {
                        addPreferenceScreenToolbar(childScreen);
                    }
                    return false;
                });
            }
        }
    }

    private static void addPreferenceScreenToolbar(PreferenceScreen childScreen) {
        Dialog dialog = childScreen.getDialog();
        if (dialog == null) {
            return;
        }

        FrameLayout content = dialog.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag(TOOLBAR_ROOT_TAG) != null) {
            return;
        }

        Context context = childScreen.getContext();
        int backgroundColor = SettingsActivityLayout.resolveBackgroundColor(context);
        int foregroundColor = SettingsActivityLayout.resolveForegroundColor(context, backgroundColor);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setStatusBarColor(backgroundColor);
            window.setNavigationBarColor(backgroundColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(true);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(true);
            }
        }

        LinearLayout root = new LinearLayout(context);
        root.setTag(TOOLBAR_ROOT_TAG);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor);
        root.setFitsSystemWindows(true);
        root.setTransitionGroup(true);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        SettingsActivityLayout.applySystemWindowInsets(root);

        Toolbar toolbar = new Toolbar(context);
        toolbar.setTitle(childScreen.getTitle());
        toolbar.setTitleTextColor(foregroundColor);
        toolbar.setTitleMargin(dp(context, 16), 0, dp(context, 16), 0);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setNavigationIcon(SettingsActivityLayout.createBackArrowDrawable(context, foregroundColor));
        toolbar.setNavigationOnClickListener(view -> dialog.dismiss());

        TextView toolbarTextView = findTextView(toolbar);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(foregroundColor);
            toolbarTextView.setTextSize(20);
        }

        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                SettingsActivityLayout.resolveToolbarHeight(context)
        ));

        while (content.getChildCount() > 0) {
            View child = content.getChildAt(0);
            content.removeViewAt(0);
            root.addView(child, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
            ));
        }

        content.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.requestApplyInsets();
    }

    private static TextView findTextView(View view) {
        if (view instanceof TextView) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
            TextView textView = findTextView(viewGroup.getChildAt(i));
            if (textView != null) {
                return textView;
            }
        }

        return null;
    }

    private static int dp(Context context, float value) {
        return Math.round(android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }
}
