/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"deprecation", "unused"})
public class MorpheSwitchPreference extends SwitchPreference {
    private View rowView;
    private MorphePreferenceStyle.SwitchView switchView;
    private boolean hasPendingAnimation;
    private boolean pendingFromChecked;
    private boolean pendingToChecked;

    public MorpheSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MorpheSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MorpheSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MorpheSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = MorphePreferenceStyle.createPreferenceView(
                getContext(),
                MorphePreferenceStyle.TRAILING_SWITCH
        );
        rowView = view;
        switchView = MorphePreferenceStyle.findSwitch(view);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        rowView = view;
        Object previousKey = view.getTag();
        boolean samePreference = getKey() != null && getKey().equals(previousKey);
        view.setTag(getKey());

        MorphePreferenceStyle.bindText(this, view);
        MorphePreferenceStyle.bindSwitchAccessibility(view, isChecked());

        MorphePreferenceStyle.SwitchView currentSwitch = MorphePreferenceStyle.findSwitch(view);
        if (currentSwitch == null) {
            return;
        }

        currentSwitch.setEnabled(isEnabled());
        if (hasPendingAnimation && isChecked() == pendingToChecked) {
            currentSwitch.setChecked(pendingFromChecked, false);
            currentSwitch.setChecked(pendingToChecked, true);
            hasPendingAnimation = false;
        } else if (!currentSwitch.isAnimating() || !samePreference || currentSwitch.isChecked() != isChecked()) {
            currentSwitch.setChecked(isChecked(), false);
        } else {
            currentSwitch.setEnabled(isEnabled());
        }
        switchView = currentSwitch;
    }

    @Override
    protected void onClick() {
        if (!MorphePreferenceStyle.consumeSwitchClickAllowed(rowView)) {
            hasPendingAnimation = false;
            return;
        }

        boolean wasChecked = isChecked();
        hasPendingAnimation = true;
        pendingFromChecked = wasChecked;
        pendingToChecked = !wasChecked;
        super.onClick();

        if (wasChecked == isChecked()) {
            hasPendingAnimation = false;
            return;
        }

        if (rowView != null) {
            MorphePreferenceStyle.bindSwitchAccessibility(rowView, isChecked());
        }

        if (hasPendingAnimation && switchView != null) {
            switchView.setChecked(wasChecked, false);
            switchView.setChecked(isChecked(), true);
            hasPendingAnimation = false;
        }
    }
}
