package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.TRUE;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;

@SuppressWarnings("unused")
public final class Settings extends BaseSettings {
    private Settings() {
    }

    public static final BooleanSetting HIDE_OFFICIAL_NOTICES =
            new BooleanSetting("dcinside_hide_official_notices", TRUE);

    public static boolean hideOfficialNotices() {
        return HIDE_OFFICIAL_NOTICES.get();
    }
}
