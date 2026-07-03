package app.revanced.extension.all.misc.signature.spoof;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Replaces the original {@code android:appComponentFactory} of the patched app to bootstrap the
 * runtime signature spoof before any application, activity, receiver, service or provider
 * instance is created.
 *
 * <p>The original factory, if any, is loaded from {@link #ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE}
 * or, failing that, from a {@code <meta-data>} value with name
 * {@link #ORIGINAL_APP_COMPONENT_FACTORY_META_DATA} (the latter is added when the original factory
 * declaration is relative and could not be resolved from the resource at patch time). All
 * {@code instantiate*} calls are then delegated to the loaded original factory so the host app
 * keeps its own component creation logic intact.</p>
 */
public final class SignatureSpoofAppComponentFactory extends AppComponentFactory {

    private static final String TAG = "SignatureSpoof";

    static final String ORIGINAL_APP_COMPONENT_FACTORY_META_DATA =
            "app.revanced.extension.all.misc.signature.spoof.ORIGINAL_APP_COMPONENT_FACTORY";

    static final String ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE =
            "app.revanced.signature_spoof.app_component_factory";

    private AppComponentFactory originalFactory;
    private String originalFactoryName;

    static {
        // Install the PackageManager / WebViewUpdateService hooks as soon as this class is
        // loaded. The PackageManager hooks are safe to install before resources are loaded; they
        // gracefully no-op until initialize() populates the original signature / package name.
        SignatureSpoof.installHooks();
    }

    @Override
    public ClassLoader instantiateClassLoader(ClassLoader cl, ApplicationInfo aInfo) {
        loadOriginalFactoryName(cl, aInfo);
        SignatureSpoof.initialize(cl, aInfo.packageName);
        AppComponentFactory original = getOriginalFactory(cl);
        ClassLoader result = original != null
                ? original.instantiateClassLoader(cl, aInfo)
                : super.instantiateClassLoader(cl, aInfo);
        // The original factory may return a different classloader; install the spoof with the one
        // that will actually load the patched app classes so the spoof resources are visible.
        if (result != cl) {
            SignatureSpoof.initialize(result, aInfo.packageName);
        }
        return result;
    }

    @Override
    public Application instantiateApplication(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        SignatureSpoof.initialize(cl, null);
        AppComponentFactory original = getOriginalFactory(cl);
        return original != null
                ? original.instantiateApplication(cl, className)
                : super.instantiateApplication(cl, className);
    }

    @Override
    public Activity instantiateActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        SignatureSpoof.initialize(cl, null);
        AppComponentFactory original = getOriginalFactory(cl);
        return original != null
                ? original.instantiateActivity(cl, className, intent)
                : super.instantiateActivity(cl, className, intent);
    }

    @Override
    public BroadcastReceiver instantiateReceiver(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        SignatureSpoof.initialize(cl, null);
        AppComponentFactory original = getOriginalFactory(cl);
        return original != null
                ? original.instantiateReceiver(cl, className, intent)
                : super.instantiateReceiver(cl, className, intent);
    }

    @Override
    public android.app.Service instantiateService(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        SignatureSpoof.initialize(cl, null);
        AppComponentFactory original = getOriginalFactory(cl);
        return original != null
                ? original.instantiateService(cl, className, intent)
                : super.instantiateService(cl, className, intent);
    }

    @Override
    public ContentProvider instantiateProvider(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        SignatureSpoof.initialize(cl, null);
        AppComponentFactory original = getOriginalFactory(cl);
        return original != null
                ? original.instantiateProvider(cl, className)
                : super.instantiateProvider(cl, className);
    }

    private void loadOriginalFactoryName(ClassLoader cl, ApplicationInfo aInfo) {
        if (originalFactoryName != null) return;

        originalFactoryName = readFirstLine(cl, ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE);
        if (originalFactoryName != null && !originalFactoryName.isEmpty()) return;

        if (aInfo == null) return;
        Bundle metaData = aInfo.metaData;
        if (metaData == null) return;
        originalFactoryName = metaData.getString(ORIGINAL_APP_COMPONENT_FACTORY_META_DATA);
    }

    private AppComponentFactory getOriginalFactory(ClassLoader cl) {
        if (originalFactory != null) return originalFactory;

        if (originalFactoryName == null) {
            originalFactoryName = readFirstLine(cl, ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE);
        }
        if (originalFactoryName == null || originalFactoryName.isEmpty()
                || getClass().getName().equals(originalFactoryName)) {
            return null;
        }

        try {
            Class<?> factoryClass = Class.forName(originalFactoryName, false, cl);
            Object instance = factoryClass.getDeclaredConstructor().newInstance();
            if (instance instanceof AppComponentFactory) {
                originalFactory = (AppComponentFactory) instance;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to instantiate original AppComponentFactory: " + originalFactoryName, t);
        }
        return originalFactory;
    }

    private static String readFirstLine(ClassLoader cl, String resource) {
        try (InputStream input = cl.getResourceAsStream(resource)) {
            if (input == null) return null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line != null ? line.trim() : null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read " + resource, e);
            return null;
        }
    }
}