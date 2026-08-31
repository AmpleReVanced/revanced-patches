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

    public static void setPageIndicatorVisibility(View view, int originalVisibility) {
        view.setVisibility(
                Settings.hidePostListPageIndicators() ? View.GONE : originalVisibility
        );
    }
}