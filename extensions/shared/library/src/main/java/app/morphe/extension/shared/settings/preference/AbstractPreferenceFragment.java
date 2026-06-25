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
        if (preference instanceof PreferenceScreen) {
            wrapDialogListClickListener(((PreferenceScreen) preference).getDialog());
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    protected void removePreferences(String... keys) {
        for (String key : keys) {
            Preference preference = findPreference(key);
            PreferenceGroup parent = preference == null ? null : preference.getParent();
            if (parent != null) {
                parent.removePreference(preference);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new DebouncedListView(getActivity());
    }

    private static void wrapDialogListClickListener(Dialog dialog) {
        if (dialog == null) {
            return;
        }

        ListView listView = dialog.findViewById(android.R.id.list);
        if (listView == null) {
            return;
        }

        AdapterView.OnItemClickListener originalListener = listView.getOnItemClickListener();
        if (originalListener != null && !(originalListener instanceof DebouncedItemClickListener)) {
            listView.setOnItemClickListener(new DebouncedItemClickListener(originalListener));
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
