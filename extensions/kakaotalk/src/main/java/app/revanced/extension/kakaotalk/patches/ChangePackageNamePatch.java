package app.revanced.extension.kakaotalk.patches;

import android.content.Context;
import android.content.ContextWrapper;

import app.revanced.extension.kakaotalk.spoofer.Spoofer;

public final class ChangePackageNamePatch extends ContextWrapper {
    private ChangePackageNamePatch(Context base) {
        super(base);
    }

    public static Context wrapEmoticonContext(Context context) {
        return new ChangePackageNamePatch(context);
    }

    @Override
    public String getPackageName() {
        return Spoofer.PACKAGE_NAME;
    }
}