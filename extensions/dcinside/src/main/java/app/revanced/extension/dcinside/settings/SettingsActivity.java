package app.revanced.extension.dcinside.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.revanced.extension.dcinside.helper.ResourceHelper;

public final class SettingsActivity extends Activity {
    private static final String SETTINGS_SHORTCUT_ID = "morphe_dcinside_settings";
    private static final String PREF_APPLY_USER_MEMO_PRESET = "morphe_pref_apply_user_memo_preset";
    private static final String PREF_CLEAR_USER_MEMOS = "morphe_pref_clear_user_memos";
    private static final String PREF_DEBUG = "morphe_pref_debug";
    private static final String PREF_DEBUG_STACKTRACE = "morphe_pref_debug_stacktrace";
    private static final String PREF_DEBUG_TOAST = "morphe_pref_debug_toast";
    private static final String PREF_APP_VERSION = "morphe_pref_app_version";
    private static final String PREF_PATCHES_VERSION = "morphe_pref_patches_version";
    private static final String PREF_PACKAGE_NAME = "morphe_pref_package_name";
    private static final String PREF_RESET = "morphe_pref_reset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);
        setTitle(resString("morphe_label_for_ample_settings", "Morphe Settings"));
        setContentView(requireResourceId("layout", "morphe_dcinside_settings"));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(resString("morphe_label_for_ample_settings", "Morphe Settings"));
        }

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(requireResourceId("id", "morphe_dcinside_settings_container"), new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void bindSettingsShortcut(View rootView) {
        if (rootView == null) {
            return;
        }

        Context context = rootView.getContext();
        int shortcutId = context.getResources().getIdentifier(
                SETTINGS_SHORTCUT_ID,
                "id",
                context.getPackageName()
        );
        if (shortcutId == 0) {
            return;
        }

        View shortcut = rootView.findViewById(shortcutId);
        if (shortcut == null) {
            return;
        }

        shortcut.setOnClickListener(view ->
                view.getContext().startActivity(new Intent(view.getContext(), SettingsActivity.class))
        );
    }

    private String resString(String name, String fallback) {
        int resourceId = ResourceHelper.getResourceId("string", name);
        return resourceId == 0 ? fallback : getString(resourceId);
    }

    private static int requireResourceId(String defType, String name) {
        int resourceId = ResourceHelper.getResourceId(defType, name);
        if (resourceId == 0) {
            throw new IllegalStateException("Missing resource: " + defType + "/" + name);
        }
        return resourceId;
    }

    public static final class SettingsFragment extends PreferenceFragment {
        private final List<SwitchBinding> switchBindings = new ArrayList<>();
        private final Set<BooleanSetting> resettableSettings = new LinkedHashSet<>();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(requireResourceId("xml", "morphe_dcinside_settings_preferences"));

            bindUserMemoPresetPreference();
            bindSwitch(PREF_DEBUG, BaseSettings.DEBUG);
            bindSwitch(PREF_DEBUG_STACKTRACE, BaseSettings.DEBUG_STACKTRACE);
            bindSwitch(PREF_DEBUG_TOAST, BaseSettings.DEBUG_TOAST_ON_ERROR);

            bindInfoPreference(PREF_APP_VERSION, Utils.getAppVersionName());
            bindInfoPreference(PREF_PATCHES_VERSION, Utils.getPatchesReleaseVersion());
            bindInfoPreference(PREF_PACKAGE_NAME, requireActivity().getPackageName());
            bindClearUserMemosPreference();
            bindResetPreference();

            refreshPreferences();
        }

        private void bindUserMemoPresetPreference() {
            Preference preference = requirePreference(PREF_APPLY_USER_MEMO_PRESET, Preference.class);
            preference.setOnPreferenceClickListener(pref -> {
                showUserMemoPresetSelectionDialog();
                return true;
            });
        }

        private void showUserMemoPresetSelectionDialog() {
            Activity activity = requireActivity();
            UserMemoPatch.Preset[] presets = UserMemoPatch.getPresets();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder
                    .setTitle(resString(
                            "morphe_settings_user_memo_preset_select_title",
                            "Select user memo preset"
                    ))
                    .setAdapter(new PresetAdapter(builder.getContext(), presets),
                            (dialog, which) -> showUserMemoPresetConfirmDialog(presets[which]))
                    .setNegativeButton(resString(
                            "morphe_settings_user_memo_preset_dialog_negative",
                            "Cancel"
                    ), null)
                    .show();
        }

        private void showUserMemoPresetConfirmDialog(UserMemoPatch.Preset preset) {
            Activity activity = requireActivity();
            String title = preset.getTitle(activity);
            String description = preset.getDescription(activity);
            new AlertDialog.Builder(activity)
                    .setTitle(resString(
                            "morphe_settings_user_memo_preset_dialog_title",
                            "Register user memo preset?"
                    ))
                    .setMessage(resString(
                            "morphe_settings_user_memo_preset_dialog_message",
                            "%1$s\n%2$s\n\nRegister it to global user memo now?",
                            title,
                            description
                    ))
                    .setPositiveButton(resString(
                            "morphe_settings_user_memo_preset_dialog_positive",
                            "Use"
                    ), (dialog, which) -> UserMemoPatch.registerUserMemoPreset(activity, preset))
                    .setNegativeButton(resString(
                            "morphe_settings_user_memo_preset_dialog_negative",
                            "Cancel"
                    ), null)
                    .show();
        }

        private void bindClearUserMemosPreference() {
            Preference preference = requirePreference(PREF_CLEAR_USER_MEMOS, Preference.class);
            preference.setOnPreferenceClickListener(pref -> {
                Activity activity = requireActivity();
                new AlertDialog.Builder(activity)
                        .setTitle(resString(
                                "morphe_settings_user_memo_clear_dialog_title",
                                "Clear user memos?"
                        ))
                        .setMessage(resString(
                                "morphe_settings_user_memo_clear_dialog_message",
                                "All user memos stored in the app will be deleted. Continue?"
                        ))
                        .setPositiveButton(resString(
                                "morphe_settings_user_memo_clear_dialog_positive",
                                "Clear"
                        ), (dialog, which) -> UserMemoPatch.clearUserMemos(activity))
                        .setNegativeButton(resString(
                                "morphe_settings_user_memo_preset_dialog_negative",
                                "Cancel"
                        ), null)
                        .show();
                return true;
            });
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            applyTopInsetWorkaround();
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshPreferences();
        }

        private void bindSwitch(String key, BooleanSetting setting) {
            SwitchPreference preference = requirePreference(key, SwitchPreference.class);
            switchBindings.add(new SwitchBinding(preference, setting));
            resettableSettings.add(setting);
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                setting.save((Boolean) newValue);
                refreshPreferences();
                return true;
            });
        }

        private void bindInfoPreference(String key, String summary) {
            Preference preference = requirePreference(key, Preference.class);
            preference.setSelectable(false);
            preference.setSummary(normalizeSummary(summary));
        }

        private void bindResetPreference() {
            Preference preference = requirePreference(PREF_RESET, Preference.class);
            preference.setOnPreferenceClickListener(pref -> {
                for (BooleanSetting setting : resettableSettings) {
                    setting.resetToDefault();
                }
                refreshPreferences();
                return true;
            });
        }

        private void refreshPreferences() {
            for (SwitchBinding binding : switchBindings) {
                binding.preference.setChecked(binding.setting.get());
                binding.preference.setEnabled(binding.setting.isAvailable());
            }
        }

        private void applyTopInsetWorkaround() {
            ListView listView = requireActivity().findViewById(android.R.id.list);
            if (listView == null) {
                return;
            }

            listView.setClipToPadding(false);
        }

        private String normalizeSummary(String summary) {
            if (summary == null) {
                return "-";
            }

            String trimmed = summary.trim();
            return trimmed.isEmpty() ? "-" : trimmed;
        }

        private String resString(String name, String fallback) {
            int resourceId = ResourceHelper.getResourceId("string", name);
            return resourceId == 0 ? fallback : requireActivity().getString(resourceId);
        }

        private String resString(String name, String fallback, Object... formatArgs) {
            int resourceId = ResourceHelper.getResourceId("string", name);
            return resourceId == 0
                    ? String.format(fallback, formatArgs)
                    : requireActivity().getString(resourceId, formatArgs);
        }

        private static int dp(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
        }

        private static ColorStateList resolveTextColor(Context context, int attr) {
            TypedArray array = context.obtainStyledAttributes(new int[]{attr});
            try {
                return array.getColorStateList(0);
            } finally {
                array.recycle();
            }
        }

        private static void setTextColor(TextView view, ColorStateList color) {
            if (color != null) {
                view.setTextColor(color);
            }
        }

        private Activity requireActivity() {
            Activity activity = getActivity();
            if (activity == null) {
                throw new IllegalStateException("Settings fragment is not attached");
            }
            return activity;
        }

        private <T extends Preference> T requirePreference(String key, Class<T> type) {
            Preference preference = findPreference(key);
            if (preference == null) {
                throw new IllegalStateException("Missing preference: " + key);
            }
            return type.cast(preference);
        }

        private static final class PresetAdapter extends BaseAdapter {
            private final Context context;
            private final ColorStateList primaryTextColor;
            private final ColorStateList secondaryTextColor;
            private final UserMemoPatch.Preset[] presets;

            PresetAdapter(Context context, UserMemoPatch.Preset[] presets) {
                this.context = context;
                this.presets = presets;
                primaryTextColor = resolveTextColor(context, android.R.attr.textColorPrimary);
                secondaryTextColor = resolveTextColor(context, android.R.attr.textColorSecondary);
            }

            @Override
            public int getCount() {
                return presets.length;
            }

            @Override
            public UserMemoPatch.Preset getItem(int position) {
                return presets[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView instanceof LinearLayout && convertView.getTag() instanceof ViewHolder) {
                    holder = (ViewHolder) convertView.getTag();
                } else {
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setLayoutParams(new AbsListView.LayoutParams(
                            AbsListView.LayoutParams.MATCH_PARENT,
                            AbsListView.LayoutParams.WRAP_CONTENT
                    ));
                    layout.setMinimumHeight(dp(context, 64));
                    layout.setPadding(dp(context, 24), dp(context, 12), dp(context, 24), dp(context, 12));

                    TextView title = new TextView(context);
                    title.setTypeface(Typeface.DEFAULT_BOLD);
                    title.setTextSize(16);
                    title.setSingleLine(false);
                    setTextColor(title, primaryTextColor);

                    TextView description = new TextView(context);
                    description.setTextSize(13);
                    description.setSingleLine(false);
                    description.setLineSpacing(0, 1.1f);
                    description.setPadding(0, dp(context, 4), 0, 0);
                    setTextColor(description, secondaryTextColor);

                    layout.addView(title, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    layout.addView(description, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    holder = new ViewHolder(title, description);
                    layout.setTag(holder);
                    convertView = layout;
                }

                UserMemoPatch.Preset preset = getItem(position);
                holder.title.setText(preset.getTitle(context));
                holder.description.setText(preset.getDescription(context));
                return convertView;
            }
        }

        private static final class ViewHolder {
            private final TextView title;
            private final TextView description;

            ViewHolder(TextView title, TextView description) {
                this.title = title;
                this.description = description;
            }
        }
    }

    private static final class SwitchBinding {
        private final SwitchPreference preference;
        private final BooleanSetting setting;

        private SwitchBinding(SwitchPreference preference, BooleanSetting setting) {
            this.preference = preference;
            this.setting = setting;
        }
    }
}
