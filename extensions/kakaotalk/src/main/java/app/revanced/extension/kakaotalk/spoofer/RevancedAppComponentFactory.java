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

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Context context = Reflect.on("android.app.ActivityThread").call("currentApplication").get();

                Log.i("PATCHER", "currentApplication: " + context);
                Log.i("PATCHER", "currentPackageName: " + context.getApplicationContext().getPackageName());
                Log.i("PATCHER", "installPackageName (LEGACY): " + context.getApplicationContext().getPackageManager().getInstallerPackageName(context.getPackageName()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        Log.i("PATCHER", "installPackageName (MODERN): " + context.getApplicationContext().getPackageManager().getInstallSourceInfo(context.getPackageName()).getInitiatingPackageName());
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("PATCHER", "Failed to get install source info", e);
                    }
                }
            }
        }, 5000);
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
