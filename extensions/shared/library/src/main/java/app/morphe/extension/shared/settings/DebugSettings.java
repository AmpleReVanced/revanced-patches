package app.morphe.extension.shared.settings;

import org.json.JSONException;
import org.json.JSONObject;

import app.morphe.extension.shared.settings.preference.SharedPrefCategory;

public final class DebugSettings {
    private static final String LEGACY_PREFERENCE = "morphe_debug";
    private static final String LEGACY_BACKUP_KEY = "debug";
    private static final String BACKUP_KEY = "debug_level";

    private DebugSettings() {
    }

    public static synchronized void migrateLegacyPreference() {
        SharedPrefCategory preferences = Setting.preferences;
        if (!preferences.preferences.contains(LEGACY_PREFERENCE)) {
            return;
        }
        if (!preferences.preferences.contains(BaseSettings.DEBUG_LEVEL.key)
                && preferences.getBoolean(LEGACY_PREFERENCE, false)) {
            BaseSettings.DEBUG_LEVEL.save(DebugLevel.DEBUG);
        }
        preferences.removeKey(LEGACY_PREFERENCE);
    }

    public static String migrateLegacyBackup(String backup) {
        String json = backup.trim();
        if (json.endsWith(",")) {
            json = json.substring(0, json.length() - 1);
        }
        if (!json.startsWith("{")) {
            json = "{" + json + "}";
        }

        try {
            JSONObject settings = new JSONObject(json);
            Object legacyDebug = settings.opt(LEGACY_BACKUP_KEY);
            if (!settings.has(BACKUP_KEY) && legacyDebug instanceof Boolean) {
                DebugLevel level = (Boolean) legacyDebug ? DebugLevel.DEBUG : DebugLevel.DISABLED;
                settings.put(BACKUP_KEY, level.name());
                return settings.toString();
            }
        } catch (JSONException ignored) {
            // Let the settings importer report malformed backups.
        }
        return backup;
    }
}