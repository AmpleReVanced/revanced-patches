package app.morphe.extension.shared.settings.preference;

import android.app.Dialog;
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

import app.morphe.extension.shared.Utils;

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

    private static ListView findListView(View view) {
        if (view instanceof ListView) {
            return (ListView) view;
        }

        View list = view.findViewById(android.R.id.list);
        if (list instanceof ListView) {
            return (ListView) list;
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

    private static int toggleFeedbackConstant(TwoStatePreference preference) {
        if (Build.VERSION.SDK_INT >= 34) {
            return preference.isChecked()
                    ? HapticFeedbackConstants.TOGGLE_OFF
                    : HapticFeedbackConstants.TOGGLE_ON;
        }
        return HapticFeedbackConstants.CLOCK_TICK;
    }
}
