package app.revanced.extension.kakaotalk.spoofer;

import android.app.AppComponentFactory;
import android.content.pm.ApplicationInfo;

import com.yc.pm.PackageManagerStub;
import com.yc.pm.WebViewUpdateServiceStub;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RevancedAppComponentFactory extends AppComponentFactory {
    static {
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
