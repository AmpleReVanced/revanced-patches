package app.revanced.extension.kakaotalk.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.revanced.extension.kakaotalk.helper.ResourceHelper;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.settings.preference.SettingsActivityLayout;
import app.morphe.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.revanced.extension.kakaotalk.patches.BypassMoatCheckPatch;
import app.revanced.extension.kakaotalk.patches.DefaultExternalBrowserPatch;
import app.revanced.extension.kakaotalk.patches.DisableOpenChatRoomCommentPatch;
import app.revanced.extension.kakaotalk.patches.EnableMarkdownPatch;
import app.revanced.extension.kakaotalk.patches.EnableRecordingPauseResumePatch;
import app.revanced.extension.kakaotalk.patches.EnableSendBigTextPatch;
import app.revanced.extension.kakaotalk.patches.ForceEnableDebugModePatch;
import app.revanced.extension.kakaotalk.patches.GhostModePatch;
import app.revanced.extension.kakaotalk.patches.HideMoreTabComponentsPatch;
import app.revanced.extension.kakaotalk.patches.HideMoreTabGamePatch;
import app.revanced.extension.kakaotalk.patches.OverrideFeatureFlagPatch;
import app.revanced.extension.kakaotalk.patches.PlayYoutubePlayerInChatRoomPatch;
import app.revanced.extension.kakaotalk.patches.Remove99ClampPatch;
import app.revanced.extension.kakaotalk.patches.RemoveShortFormTabPatch;
import app.revanced.extension.kakaotalk.patches.ShowDeletedHiddenOrEditedMessagePatch;

public final class SettingsActivity extends Activity {
    private static final String PREF_GHOST_MODE = "morphe_pref_ghost_mode";
    private static final String PREF_SHOW_MODIFIED_MESSAGE_SENDER_PROFILE =
            "morphe_pref_show_modified_message_sender_profile";
    private static final String PREF_REMOVE_SHORT_FORM_TAB = "morphe_pref_remove_short_form_tab";
    private static final String PREF_HIDE_MORE_TAB_GAME = "morphe_pref_hide_more_tab_game";
    private static final String PREF_HIDE_MORE_TAB_KAKAO_PAY_SECTION = "morphe_pref_hide_more_tab_kakao_pay_section";
    private static final String PREF_HIDE_MORE_TAB_GAME_PLAY_SECTION = "morphe_pref_hide_more_tab_game_play_section";
    private static final String PREF_HIDE_MORE_TAB_KAKAO_NOW_SECTION = "morphe_pref_hide_more_tab_kakao_now_section";
    private static final String PREF_HIDE_MORE_TAB_WEATHER_SECTION = "morphe_pref_hide_more_tab_weather_section";
    private static final String PREF_HIDE_MORE_TAB_SERVICE_GROUP_SECTION = "morphe_pref_hide_more_tab_service_group_section";
    private static final String PREF_HIDE_MORE_TAB_LINE_SERVICE_SECTION = "morphe_pref_hide_more_tab_line_service_section";
    private static final String PREF_DISABLE_99_UNREAD_LIMIT = "morphe_pref_disable_99_unread_limit";
    private static final String PREF_DEFAULT_EXTERNAL_BROWSER = "morphe_pref_default_external_browser";
    private static final String PREF_ENABLE_RECORDING_PAUSE_RESUME = "morphe_pref_enable_recording_pause_resume";
    private static final String PREF_ENABLE_SEND_BIG_TEXT = "morphe_pref_enable_send_big_text";
    private static final String PREF_ENABLE_MARKDOWN = "morphe_pref_enable_markdown";
    private static final String PREF_PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM = "morphe_pref_play_youtube_player_in_chat_room";
    private static final String PREF_OPEN_CHAT_ROOM_COMMENT_DISABLED = "morphe_pref_open_chat_room_comment_disabled";
    private static final String PREF_BYPASS_MOAT_INTEGRITY_CHECK = "morphe_pref_bypass_moat_integrity_check";
    private static final String PREF_FEATURE_FLAG_OVERRIDES = "morphe_pref_feature_flag_overrides";
    private static final String PREF_FORCE_DEBUG_MODE = "morphe_pref_force_debug_mode";
    private static final String PREF_DEBUG = "morphe_pref_debug";
    private static final String PREF_DEBUG_STACKTRACE = "morphe_pref_debug_stacktrace";
    private static final String PREF_DEBUG_TOAST = "morphe_pref_debug_toast";
    private static final String PREF_APP_VERSION = "morphe_pref_app_version";
    private static final String PREF_PATCHES_VERSION = "morphe_pref_patches_version";
    private static final String PREF_PACKAGE_NAME = "morphe_pref_package_name";
    private static final String PREF_RESET = "morphe_pref_reset";
    private static final String MESSAGE_RESTART_REQUIRED_TITLE = "morphe_settings_restart_required_title";
    private static final String MESSAGE_RESTART_REQUIRED = "morphe_settings_restart_required";
    private static final String MESSAGE_RESTART_REQUIRED_RESTART = "morphe_settings_restart_required_restart";
    private static final String MESSAGE_BYPASS_MOAT_CONFIRM_TITLE = "morphe_settings_patch_bypass_moat_check_confirm_title";
    private static final String MESSAGE_BYPASS_MOAT_CONFIRM_MESSAGE = "morphe_settings_patch_bypass_moat_check_confirm_message";
    private static final String MESSAGE_BYPASS_MOAT_CONFIRM_ENABLE = "morphe_settings_patch_bypass_moat_check_confirm_enable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);
        int containerId = SettingsActivityLayout.setContentView(
                this,
                resString("morphe_label_for_ample_settings", "Morphe Settings")
        );

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(containerId, new SettingsFragment())
                    .commit();
        }
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

    public static final class SettingsFragment extends ToolbarPreferenceFragment {
        private final List<SwitchBinding> switchBindings = new ArrayList<>();
        private final List<TextBinding> textBindings = new ArrayList<>();
        private final Set<BooleanSetting> resettableSettings = new LinkedHashSet<>();
        private final Set<StringSetting> resettableTextSettings = new LinkedHashSet<>();
        private static final Set<String> RESTART_SENSITIVE_PREFERENCES = new LinkedHashSet<>();

        static {
            RESTART_SENSITIVE_PREFERENCES.add(PREF_REMOVE_SHORT_FORM_TAB);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_GAME);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_KAKAO_PAY_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_GAME_PLAY_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_KAKAO_NOW_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_WEATHER_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_SERVICE_GROUP_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_HIDE_MORE_TAB_LINE_SERVICE_SECTION);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_FORCE_DEBUG_MODE);
            RESTART_SENSITIVE_PREFERENCES.add(PREF_BYPASS_MOAT_INTEGRITY_CHECK);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(requireResourceId("xml", "morphe_kakaotalk_settings_preferences"));

            bindSwitchIfIncluded(PREF_GHOST_MODE, Settings.GHOST_MODE, GhostModePatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_REMOVE_SHORT_FORM_TAB, Settings.REMOVE_SHORT_FORM_TAB, RemoveShortFormTabPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_GAME, Settings.HIDE_MORE_TAB_GAME, HideMoreTabGamePatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_KAKAO_PAY_SECTION, Settings.HIDE_MORE_TAB_KAKAO_PAY_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_GAME_PLAY_SECTION, Settings.HIDE_MORE_TAB_GAME_PLAY_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_KAKAO_NOW_SECTION, Settings.HIDE_MORE_TAB_KAKAO_NOW_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_WEATHER_SECTION, Settings.HIDE_MORE_TAB_WEATHER_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_SERVICE_GROUP_SECTION, Settings.HIDE_MORE_TAB_SERVICE_GROUP_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_HIDE_MORE_TAB_LINE_SERVICE_SECTION, Settings.HIDE_MORE_TAB_LINE_SERVICE_SECTION, HideMoreTabComponentsPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_DISABLE_99_UNREAD_LIMIT, Settings.DISABLE_99_UNREAD_LIMIT, Remove99ClampPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_DEFAULT_EXTERNAL_BROWSER, Settings.DEFAULT_EXTERNAL_BROWSER, DefaultExternalBrowserPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_ENABLE_RECORDING_PAUSE_RESUME, Settings.ENABLE_RECORDING_PAUSE_RESUME, EnableRecordingPauseResumePatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_ENABLE_SEND_BIG_TEXT, Settings.ENABLE_SEND_BIG_TEXT, EnableSendBigTextPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_ENABLE_MARKDOWN, Settings.ENABLE_MARKDOWN, EnableMarkdownPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM, Settings.PLAY_YOUTUBE_PLAYER_IN_CHAT_ROOM, PlayYoutubePlayerInChatRoomPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_OPEN_CHAT_ROOM_COMMENT_DISABLED, Settings.OPEN_CHAT_ROOM_COMMENT_DISABLED, DisableOpenChatRoomCommentPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_SHOW_MODIFIED_MESSAGE_SENDER_PROFILE, Settings.SHOW_MODIFIED_MESSAGE_SENDER_PROFILE, ShowDeletedHiddenOrEditedMessagePatch.isPatchIncluded());
            bindRiskySwitchIfIncluded(PREF_BYPASS_MOAT_INTEGRITY_CHECK, Settings.BYPASS_MOAT_INTEGRITY_CHECK, BypassMoatCheckPatch.isPatchIncluded());
            bindTextIfIncluded(PREF_FEATURE_FLAG_OVERRIDES, Settings.FEATURE_FLAG_OVERRIDES, OverrideFeatureFlagPatch.isPatchIncluded());
            bindSwitchIfIncluded(PREF_FORCE_DEBUG_MODE, Settings.FORCE_DEBUG_MODE, ForceEnableDebugModePatch.isPatchIncluded());
            bindSwitch(PREF_DEBUG, BaseSettings.DEBUG);
            bindSwitch(PREF_DEBUG_STACKTRACE, BaseSettings.DEBUG_STACKTRACE);
            bindSwitch(PREF_DEBUG_TOAST, BaseSettings.DEBUG_TOAST_ON_ERROR);

            bindInfoPreference(PREF_APP_VERSION, Utils.getAppVersionName());
            bindInfoPreference(PREF_PATCHES_VERSION, Utils.getPatchesReleaseVersion());
            bindInfoPreference(PREF_PACKAGE_NAME, requireActivity().getPackageName());
            bindResetPreference();
            removeEmptyPreferenceGroups();
            setPreferenceScreenToolbar(getPreferenceScreen());
            refreshPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshPreferences();
        }

        private void bindSwitch(String key, BooleanSetting setting) {
            SwitchPreference preference = requirePreference(key, SwitchPreference.class);
            preference.setPersistent(false);
            switchBindings.add(new SwitchBinding(preference, setting));
            resettableSettings.add(setting);
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                setting.save((Boolean) newValue);
                maybeShowRestartRequiredNotice(key);
                refreshPreferences();
                return true;
            });
        }

        private void bindSwitchIfIncluded(String key, BooleanSetting setting, boolean included) {
            if (!included) {
                removePreference(key);
                return;
            }

            bindSwitch(key, setting);
        }

        private void bindRiskySwitch(String key, BooleanSetting setting) {
            SwitchPreference preference = requirePreference(key, SwitchPreference.class);
            preference.setPersistent(false);
            switchBindings.add(new SwitchBinding(preference, setting));
            resettableSettings.add(setting);
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (!enabled) {
                    setting.save(false);
                    maybeShowRestartRequiredNotice(key);
                    refreshPreferences();
                    return true;
                }

                showMoatBypassConfirmation(key, setting);
                return false;
            });
        }

        private void bindRiskySwitchIfIncluded(String key, BooleanSetting setting, boolean included) {
            if (!included) {
                removePreference(key);
                return;
            }

            bindRiskySwitch(key, setting);
        }

        private void bindText(String key, StringSetting setting) {
            EditTextPreference preference = requirePreference(key, EditTextPreference.class);
            preference.setPersistent(false);
            textBindings.add(new TextBinding(preference, setting, preference.getSummary()));
            resettableTextSettings.add(setting);
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                setting.save((String) newValue);
                refreshPreferences();
                return true;
            });
        }

        private void bindTextIfIncluded(String key, StringSetting setting, boolean included) {
            if (!included) {
                removePreference(key);
                return;
            }

            bindText(key, setting);
        }

        private void maybeShowRestartRequiredNotice(String key) {
            if (!RESTART_SENSITIVE_PREFERENCES.contains(key)) {
                return;
            }

            new AlertDialog.Builder(requireActivity())
                    .setTitle(resString(
                            MESSAGE_RESTART_REQUIRED_TITLE,
                            "Restart required"
                    ))
                    .setMessage(resString(
                            MESSAGE_RESTART_REQUIRED,
                            "Restart is required to apply this setting."
                    ))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(resString(
                            MESSAGE_RESTART_REQUIRED_RESTART,
                            "Restart"
                    ), (dialog, which) -> Utils.restartApp(requireActivity()))
                    .show();
        }

        private void showMoatBypassConfirmation(String key, BooleanSetting setting) {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(resString(
                            MESSAGE_BYPASS_MOAT_CONFIRM_TITLE,
                            "Enable Moat integrity detection bypass?"
                    ))
                    .setMessage(resString(
                            MESSAGE_BYPASS_MOAT_CONFIRM_MESSAGE,
                            "This bypass is required for some KakaoPay flows, but it disables an integrity detection path. Use at your own risk."
                    ))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(resString(
                            MESSAGE_BYPASS_MOAT_CONFIRM_ENABLE,
                            "Enable"
                    ), (dialog, which) -> {
                        setting.save(true);
                        maybeShowRestartRequiredNotice(key);
                        refreshPreferences();
                    })
                    .show();
        }

        private void bindInfoPreference(String key, String summary) {
            Preference preference = requirePreference(key, Preference.class);
            preference.setPersistent(false);
            preference.setSelectable(false);
            preference.setSummary(normalizeSummary(summary));
        }

        private void bindResetPreference() {
            Preference preference = requirePreference(PREF_RESET, Preference.class);
            preference.setPersistent(false);
            preference.setOnPreferenceClickListener(pref -> {
                for (BooleanSetting setting : resettableSettings) {
                    setting.resetToDefault();
                }
                for (StringSetting setting : resettableTextSettings) {
                    setting.resetToDefault();
                }
                refreshPreferences();
                return true;
            });
        }

        private void removeEmptyPreferenceGroups() {
            PreferenceGroup root = getPreferenceScreen();
            if (root == null) {
                return;
            }

            removeEmptyPreferenceGroups(root);
        }

        private boolean removeEmptyPreferenceGroups(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    PreferenceGroup child = (PreferenceGroup) preference;
                    if (removeEmptyPreferenceGroups(child) && child.getPreferenceCount() == 0) {
                        group.removePreference(child);
                    }
                }
            }

            return group != getPreferenceScreen();
        }

        private void removePreference(String key) {
            Preference preference = findPreference(key);
            PreferenceGroup root = getPreferenceScreen();
            if (preference == null || root == null) {
                return;
            }

            PreferenceGroup parent = findParentPreference(root, preference);
            if (parent != null) {
                parent.removePreference(preference);
            }
        }

        private PreferenceGroup findParentPreference(PreferenceGroup group, Preference preference) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference child = group.getPreference(i);
                if (child == preference) {
                    return group;
                }
                if (child instanceof PreferenceGroup) {
                    PreferenceGroup parent = findParentPreference((PreferenceGroup) child, preference);
                    if (parent != null) {
                        return parent;
                    }
                }
            }

            return null;
        }

        private void refreshPreferences() {
            for (SwitchBinding binding : switchBindings) {
                binding.preference.setChecked(binding.setting.get());
                binding.preference.setEnabled(binding.setting.isAvailable());
            }
            for (TextBinding binding : textBindings) {
                String value = binding.setting.get();
                binding.preference.setText(value);
                binding.preference.setSummary(value == null || value.trim().isEmpty()
                        ? binding.defaultSummary
                        : normalizeSummary(value));
                binding.preference.setEnabled(binding.setting.isAvailable());
            }
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

    private static final class TextBinding {
        private final EditTextPreference preference;
        private final StringSetting setting;
        private final CharSequence defaultSummary;

        private TextBinding(EditTextPreference preference, StringSetting setting, CharSequence defaultSummary) {
            this.preference = preference;
            this.setting = setting;
            this.defaultSummary = defaultSummary;
        }
    }
}
