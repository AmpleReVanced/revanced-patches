package app.revanced.extension.dcinside.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

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
            bindResetPreference();

            refreshPreferences();
        }

        private void bindUserMemoPresetPreference() {
            Preference preference = requirePreference(PREF_APPLY_USER_MEMO_PRESET, Preference.class);
            preference.setOnPreferenceClickListener(pref -> {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(resString(
                                "morphe_settings_user_memo_preset_dialog_title",
                                "Register user memo preset?"
                        ))
                        .setMessage(resString(
                                "morphe_settings_user_memo_preset_dialog_message",
                                "The bundled preset will be registered immediately. Continue?"
                        ))
                        .setPositiveButton(resString(
                                "morphe_settings_user_memo_preset_dialog_positive",
                                "Use"
                        ), (dialog, which) -> UserMemoPresetPatch.registerUserMemoPreset(requireActivity()))
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
