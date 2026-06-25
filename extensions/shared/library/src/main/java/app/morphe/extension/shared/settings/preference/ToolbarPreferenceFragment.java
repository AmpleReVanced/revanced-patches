/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

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
import android.widget.ListView;

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
                        postAddPreferenceScreenToolbar(childScreen, view, 0);
                    } else {
                        addPreferenceScreenToolbar(childScreen);
                    }
                    return false;
                });
            }
        }
    }

    private static void postAddPreferenceScreenToolbar(PreferenceScreen childScreen, View anchor, int attempt) {
        anchor.post(() -> {
            AbstractPreferenceFragment.stylePreferenceScreenDialog(childScreen);
            if (addPreferenceScreenToolbar(childScreen) || attempt >= 8) {
                return;
            }

            anchor.postDelayed(
                    () -> postAddPreferenceScreenToolbar(childScreen, anchor, attempt + 1),
                    16L
            );
        });
    }

    private static boolean addPreferenceScreenToolbar(PreferenceScreen childScreen) {
        Dialog dialog = childScreen.getDialog();
        if (dialog == null) {
            return false;
        }

        AbstractPreferenceFragment.styleDialogList(dialog);

        FrameLayout content = dialog.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag(TOOLBAR_ROOT_TAG) != null) {
            return content != null;
        }

        Context context = childScreen.getContext();
        int backgroundColor = SettingsActivityLayout.resolveBackgroundColor(context);
        int foregroundColor = SettingsActivityLayout.resolveForegroundColor(context, backgroundColor);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setStatusBarColor(backgroundColor);
            window.setNavigationBarColor(backgroundColor);
            SettingsActivityLayout.applySystemBarIcons(window, SettingsActivityLayout.isLight(backgroundColor));
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

        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(backgroundColor);
        int toolbarPadding = dp(context, 15);
        toolbar.setPadding(toolbarPadding, dp(context, 10), toolbarPadding, dp(context, 8));

        toolbar.addView(SettingsActivityLayout.createBackArrowView(context, view -> dialog.dismiss()));
        toolbar.addView(SettingsActivityLayout.createToolbarTitle(context, childScreen.getTitle(), foregroundColor));

        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                SettingsActivityLayout.resolveToolbarHeight(context)
        ));

        while (content.getChildCount() > 0) {
            View child = content.getChildAt(0);
            content.removeViewAt(0);
            ListView listView = findListView(child);
            if (listView != null) {
                MorphePreferenceStyle.applyListStyle(listView);
            }
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
        return true;
    }

    private static ListView findListView(View view) {
        if (view instanceof ListView) {
            return (ListView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
            ListView listView = findListView(viewGroup.getChildAt(i));
            if (listView != null) {
                return listView;
            }
        }

        return null;
    }

    private static int dp(Context context, float value) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }
}
