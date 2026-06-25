/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"deprecation", "unused"})
public class MorpheListPreference extends ListPreference {
    public MorpheListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MorpheListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MorpheListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MorpheListPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return MorphePreferenceStyle.createPreferenceView(
                getContext(),
                MorphePreferenceStyle.TRAILING_CHEVRON
        );
    }

    @Override
    protected void onBindView(View view) {
        MorphePreferenceStyle.bindText(this, view);
        MorphePreferenceStyle.bindChevron(this, view);
    }
}
