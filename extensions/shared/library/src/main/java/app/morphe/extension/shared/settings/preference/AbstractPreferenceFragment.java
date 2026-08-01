package app.morphe.extension.shared.settings.preference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.Setting;

@SuppressWarnings({"deprecation", "unused"})
public abstract class AbstractPreferenceFragment extends PreferenceFragment {
    private static final class DebouncedListView extends ListView {
        private DebouncedListView(android.content.Context context) {
            super(context);
            setId(android.R.id.list);
            MorphePreferenceStyle.applyListStyle(this);
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        @Override
        public boolean performItemClick(View view, int position, long id) {
            Object item = getAdapter().getItem(position);

            if (item instanceof TwoStatePreference) {
                view.performHapticFeedback(toggleFeedbackConstant((TwoStatePreference) item));
                return super.performItemClick(view, position, id);
            }

            if (Utils.isFastClick()) {
                return true;
            }
            return super.performItemClick(view, position, id);
        }
    }

    private static final class DebouncedItemClickListener implements AdapterView.OnItemClickListener {
        private final AdapterView.OnItemClickListener originalListener;

        private DebouncedItemClickListener(AdapterView.OnItemClickListener originalListener) {
            this.originalListener = originalListener;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object item = parent.getAdapter().getItem(position);

            if (item instanceof TwoStatePreference) {
                view.performHapticFeedback(toggleFeedbackConstant((TwoStatePreference) item));
                originalListener.onItemClick(parent, view, position, id);
                return;
            }

            if (Utils.isFastClick()) {
                return;
            }
            originalListener.onItemClick(parent, view, position, id);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean handled = super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference instanceof PreferenceScreen) {
            PreferenceScreen childScreen = (PreferenceScreen) preference;
            stylePreferenceScreenDialog(childScreen);

            View view = getView();
            if (view != null) {
                view.post(() -> stylePreferenceScreenDialog(childScreen));
            }
        }
        return handled;
    }

    protected void removePreferences(String... keys) {
        for (String key : keys) {
            removePreference(key);
        }
    }

    protected void removePreference(String key) {
        Preference preference = findPreference(key);
        PreferenceGroup root = getPreferenceScreen();
        if (preference == null || root == null) {
            return;
        }

        PreferenceGroup parent = findParentPreference(root, preference);
        if (parent != null) {
            parent.removePreference(preference);
        }
    }

    protected void removeEmptyPreferenceGroups() {
        PreferenceGroup root = getPreferenceScreen();
        if (root == null) {
            return;
        }

        removeEmptyPreferenceGroups(root, root);
    }

    protected <T extends Preference> T requirePreference(String key, Class<T> type) {
        Preference preference = findPreference(key);
        if (preference == null) {
            throw new IllegalStateException("Missing preference: " + key);
        }
        return type.cast(preference);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new DebouncedListView(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View rootView = getView();
        if (rootView == null) {
            return;
        }

        rootView.setBackgroundColor(MorphePreferenceStyle.backgroundColor(rootView.getContext()));
        ListView listView = findListView(rootView);
        if (listView != null) {
            MorphePreferenceStyle.applyListStyle(listView);
        }
    }

    static void stylePreferenceScreenDialog(PreferenceScreen preferenceScreen) {
        if (preferenceScreen == null) {
            return;
        }

        styleDialogList(preferenceScreen.getDialog());
    }

    static void styleDialogList(Dialog dialog) {
        if (dialog == null) {
            return;
        }

        ListView listView = dialog.findViewById(android.R.id.list);
        if (listView == null) {
            return;
        }

        MorphePreferenceStyle.applyListStyle(listView);

        AdapterView.OnItemClickListener originalListener = listView.getOnItemClickListener();
        if (originalListener != null && !(originalListener instanceof DebouncedItemClickListener)) {
            listView.setOnItemClickListener(new DebouncedItemClickListener(originalListener));
        }
    }

    static ListView findListView(View view) {
        if (view instanceof ListView) {
            return (ListView) view;
        }

        View list = view.findViewById(android.R.id.list);
        if (list instanceof ListView) {
            return (ListView) list;
        }

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0, count = viewGroup.getChildCount(); i < count; i++) {
            ListView listView = findListView(viewGroup.getChildAt(i));
            if (listView != null) {
                return listView;
            }
        }

        return null;
    }

    private static boolean removeEmptyPreferenceGroups(PreferenceGroup root, PreferenceGroup group) {
        for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
            Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup child = (PreferenceGroup) preference;
                if (removeEmptyPreferenceGroups(root, child) && child.getPreferenceCount() == 0) {
                    group.removePreference(child);
                }
            }
        }

        return group != root;
    }

    private static PreferenceGroup findParentPreference(PreferenceGroup group, Preference preference) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference child = group.getPreference(i);
            if (child == preference) {
                return group;
            }
            if (child instanceof PreferenceGroup) {
                PreferenceGroup parent = findParentPreference((PreferenceGroup) child, preference);
                if (parent != null) {
                    return parent;
                }
            }
        }

        return null;
    }

    private static final int EXPORT_REQUEST_CODE = 0xE7;
    private static final int IMPORT_REQUEST_CODE = 0xE8;

    /**
     * Offered by every app, so that a setting which only takes effect on a fresh start says so
     * instead of looking broken.
     */
    public static void showRestartDialog(Context context, CharSequence title, CharSequence message,
                                         CharSequence restartText) {
        Utils.verifyOnMainThread();

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(restartText, (dialog, which) -> Utils.restartApp(context))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Settings are stored per install, so they are lost whenever the app is patched again.
     */
    protected void exportSettings() {
        try {
            String fileName = Utils.getApplicationName().replaceAll("\\s+", "_")
                    + "_settings_"
                    + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
                    + ".json";

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, EXPORT_REQUEST_CODE);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not start the settings export", ex);
        }
    }

    protected void importSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, IMPORT_REQUEST_CODE);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not start the settings import", ex);
        }
    }

    /**
     * Called after settings were imported, so that the app can offer a restart.
     */
    protected void onSettingsImported(boolean restartNeeded) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        if (requestCode == EXPORT_REQUEST_CODE) {
            writeSettings(data.getData());
        } else if (requestCode == IMPORT_REQUEST_CODE) {
            readSettings(data.getData());
        }
    }

    private void writeSettings(Uri uri) {
        Activity activity = getActivity();
        try (OutputStream output = activity.getContentResolver().openOutputStream(uri, "rwt")) {
            output.write(settingsJson(activity).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not export the settings", ex);
        }
    }

    /**
     * The export is a fragment meant to be pasted into a text box: it carries no outer braces and
     * ends with a comma, so a file gets the object around it back.
     */
    private static String settingsJson(Activity activity) {
        String export = Setting.exportToJson(activity).trim();

        if (export.endsWith(",")) {
            export = export.substring(0, export.length() - 1);
        }
        if (export.startsWith("{")) {
            return export;
        }

        return "{\n" + export + "\n}\n";
    }

    private void readSettings(Uri uri) {
        Activity activity = getActivity();
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int count;
            while ((count = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, count);
            }

            boolean restartNeeded = Setting.importFromJSON(
                    activity, buffer.toString(StandardCharsets.UTF_8.name()));
            onSettingsImported(restartNeeded);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not import the settings", ex);
        }
    }

    private static int toggleFeedbackConstant(TwoStatePreference preference) {
        if (Build.VERSION.SDK_INT >= 34) {
            return preference.isChecked()
                    ? HapticFeedbackConstants.TOGGLE_OFF
                    : HapticFeedbackConstants.TOGGLE_ON;
        }
        return HapticFeedbackConstants.CLOCK_TICK;
    }
}
