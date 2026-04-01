package app.revanced.extension.kakaotalk.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.util.TypedValue;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.revanced.extension.kakaotalk.helper.ResourceHelper;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;

public final class SettingsActivity extends Activity {
    private static final String PREF_GHOST_MODE = "morphe_pref_ghost_mode";
    private static final String PREF_REMOVE_SHORT_FORM_TAB = "morphe_pref_remove_short_form_tab";
    private static final String PREF_DEFAULT_EXTERNAL_BROWSER = "morphe_pref_default_external_browser";
    private static final String PREF_ENABLE_RECORDING_PAUSE_RESUME = "morphe_pref_enable_recording_pause_resume";
    private static final String PREF_ENABLE_SEND_BIG_TEXT = "morphe_pref_enable_send_big_text";
    private static final String PREF_ENABLE_MARKDOWN = "morphe_pref_enable_markdown";
    private static final String PREF_PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM = "morphe_pref_play_youtube_player_in_chat_room";
    private static final String PREF_FORCE_DEBUG_MODE = "morphe_pref_force_debug_mode";
    private static final String PREF_DEBUG = "morphe_pref_debug";
    private static final String PREF_DEBUG_STACKTRACE = "morphe_pref_debug_stacktrace";
    private static final String PREF_DEBUG_TOAST = "morphe_pref_debug_toast";
    private static final String PREF_APP_VERSION = "morphe_pref_app_version";
    private static final String PREF_PATCHES_VERSION = "morphe_pref_patches_version";
    private static final String PREF_PACKAGE_NAME = "morphe_pref_package_name";
    private static final String PREF_RESET = "morphe_pref_reset";
    private static final String PREF_GITHUB = "morphe_pref_github";
    private static final String MESSAGE_RESTART_REQUIRED = "morphe_settings_restart_required";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);
        setTitle(resString("morphe_label_for_ample_settings", "Morphe Settings"));
        setContentView(requireResourceId("layout", "morphe_kakaotalk_settings"));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(resString("morphe_label_for_ample_settings", "Morphe Settings"));
        }

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(requireResourceId("id", "morphe_kakaotalk_settings_container"), new SettingsFragment())
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
        private static final Set<String> RESTART_SENSITIVE_PREFERENCES = new LinkedHashSet<>();

        static {
            RESTART_SENSITIVE_PREFERENCES.add(PREF_REMOVE_SHORT_FORM_TAB);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(requireResourceId("xml", "morphe_kakaotalk_settings_preferences"));

            bindSwitch(PREF_GHOST_MODE, Settings.GHOST_MODE);
            bindSwitch(PREF_REMOVE_SHORT_FORM_TAB, Settings.REMOVE_SHORT_FORM_TAB);
            bindSwitch(PREF_DEFAULT_EXTERNAL_BROWSER, Settings.DEFAULT_EXTERNAL_BROWSER);
            bindSwitch(PREF_ENABLE_RECORDING_PAUSE_RESUME, Settings.ENABLE_RECORDING_PAUSE_RESUME);
            bindSwitch(PREF_ENABLE_SEND_BIG_TEXT, Settings.ENABLE_SEND_BIG_TEXT);
            bindSwitch(PREF_ENABLE_MARKDOWN, Settings.ENABLE_MARKDOWN);
            bindSwitch(PREF_PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM, Settings.PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM);
            bindSwitch(PREF_FORCE_DEBUG_MODE, Settings.FORCE_DEBUG_MODE);
            bindSwitch(PREF_DEBUG, BaseSettings.DEBUG);
            bindSwitch(PREF_DEBUG_STACKTRACE, BaseSettings.DEBUG_STACKTRACE);
            bindSwitch(PREF_DEBUG_TOAST, BaseSettings.DEBUG_TOAST_ON_ERROR);

            bindInfoPreference(PREF_APP_VERSION, Utils.getAppVersionName());
            bindInfoPreference(PREF_PATCHES_VERSION, Utils.getPatchesReleaseVersion());
            bindInfoPreference(PREF_PACKAGE_NAME, requireActivity().getPackageName());
            bindResetPreference();
            bindLinkPreference(PREF_GITHUB, "https://github.com/MorpheApp/morphe-patches");

            refreshPreferences();
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
                maybeShowRestartRequiredNotice(key);
                refreshPreferences();
                return true;
            });
        }

        private void maybeShowRestartRequiredNotice(String key) {
            if (!RESTART_SENSITIVE_PREFERENCES.contains(key)) {
                return;
            }

            int resourceId = ResourceHelper.getResourceId("string", MESSAGE_RESTART_REQUIRED);
            String message = resourceId == 0
                    ? "Restart is required to apply this setting."
                    : requireActivity().getString(resourceId);
            Utils.showToastLong(message);
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

        private void bindLinkPreference(String key, String url) {
            Preference preference = requirePreference(key, Preference.class);
            preference.setOnPreferenceClickListener(pref -> {
                Utils.openLink(url);
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

            if (!isActionBarOverlayEnabled()) {
                return;
            }

            int actionBarHeight = getActionBarHeight();
            if (actionBarHeight <= 0) {
                return;
            }

            listView.setPadding(
                    listView.getPaddingLeft(),
                    listView.getPaddingTop() + actionBarHeight,
                    listView.getPaddingRight(),
                    listView.getPaddingBottom()
            );
        }

        private boolean isActionBarOverlayEnabled() {
            TypedValue typedValue = new TypedValue();
            return requireActivity().getTheme().resolveAttribute(android.R.attr.windowActionBarOverlay, typedValue, true)
                    && typedValue.data != 0;
        }

        private int getActionBarHeight() {
            TypedValue typedValue = new TypedValue();
            if (!requireActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                return 0;
            }

            return TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }

        private String normalizeSummary(String summary) {
            if (summary == null) {
                return "-";
            }

            String trimmed = summary.trim();
            return trimmed.isEmpty() ? "-" : trimmed;
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
