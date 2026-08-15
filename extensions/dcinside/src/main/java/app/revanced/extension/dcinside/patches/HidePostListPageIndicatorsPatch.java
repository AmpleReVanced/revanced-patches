package app.revanced.extension.dcinside.patches;

import android.view.View;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class HidePostListPageIndicatorsPatch {
    private HidePostListPageIndicatorsPatch() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static int getPageIndicatorVisibility(int originalVisibility) {
        return Settings.hidePostListPageIndicators() ? View.GONE : originalVisibility;
    }
}