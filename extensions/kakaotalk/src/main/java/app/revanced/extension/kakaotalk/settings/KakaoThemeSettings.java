package app.revanced.extension.kakaotalk.settings;

@SuppressWarnings("unused")
public final class KakaoThemeSettings {
    private static final int MODE_NIGHT_UNSPECIFIED = Integer.MIN_VALUE;
    private static final int KAKAO_MODE_NIGHT_YES = 1;
    private static final int KAKAO_MODE_NIGHT_NO = 2;

    private static volatile int nightMode = MODE_NIGHT_UNSPECIFIED;

    private KakaoThemeSettings() {
    }

    public static void setNightMode(int nightMode) {
        KakaoThemeSettings.nightMode = nightMode;
    }

    public static Boolean isDarkMode() {
        if (nightMode == KAKAO_MODE_NIGHT_YES) {
            return true;
        }
        if (nightMode == KAKAO_MODE_NIGHT_NO) {
            return false;
        }
        return null;
    }
}
