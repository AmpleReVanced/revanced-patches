package app.revanced.extension.kakaotalk.patches;

import android.content.Context;
import android.content.ContextWrapper;

import org.json.JSONObject;

import app.revanced.extension.kakaotalk.spoofer.Spoofer;

public final class ChangePackageNamePatch extends ContextWrapper {
    private ChangePackageNamePatch(Context base) {
        super(base);
    }

    public static Context wrapContext(Context context) {
        return new ChangePackageNamePatch(context);
    }

    public static void sanitizeFidoProperties(JSONObject properties) {
        properties.remove("lab");
        properties.remove("dip");
    }

    @Override
    public String getPackageName() {
        return Spoofer.PACKAGE_NAME;
    }
}