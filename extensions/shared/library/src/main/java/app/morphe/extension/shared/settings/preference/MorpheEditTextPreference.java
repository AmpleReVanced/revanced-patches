/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"deprecation", "unused"})
public class MorpheEditTextPreference extends EditTextPreference {
    public MorpheEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MorpheEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MorpheEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MorpheEditTextPreference(Context context) {
        super(context);
    }

    @Override
    @SuppressLint("MissingSuperCall")
    protected View onCreateView(ViewGroup parent) {
        return MorphePreferenceStyle.createPreferenceView(
                getContext(),
                MorphePreferenceStyle.TRAILING_CHEVRON
        );
    }

    @Override
    @SuppressLint("MissingSuperCall")
    protected void onBindView(View view) {
        MorphePreferenceStyle.bindText(this, view);
        MorphePreferenceStyle.bindChevron(this, view);
    }
}
