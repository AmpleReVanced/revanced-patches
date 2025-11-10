package app.revanced.extension.kakaotalk.spoofer;

import android.app.AppComponentFactory;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.yc.pm.PackageManagerStub;
import com.yc.pm.Reflect;
import com.yc.pm.WebViewUpdateServiceStub;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class RevancedAppComponentFactory extends AppComponentFactory {
    static {
        HiddenApiBypass.addHiddenApiExemptions("");

        PackageManagerStub.replaceService();
        WebViewUpdateServiceStub.replaceService();
    }

    @Override
    public ClassLoader instantiateClassLoader(ClassLoader cl, ApplicationInfo aInfo) {
        try (InputStream s = cl.getResource("app.revanced.sig.orig").openStream()) {
            try (BufferedReader b = new BufferedReader(new InputStreamReader(s))) {
                Spoofer.SIGNATURE_HEX = b.readLine();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return super.instantiateClassLoader(cl, aInfo);
    }
}
