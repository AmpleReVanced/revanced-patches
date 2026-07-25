/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings({"deprecation", "unused"})
public class MorpheNoTitlePreferenceCategory extends PreferenceCategory {
    public MorpheNoTitlePreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MorpheNoTitlePreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MorpheNoTitlePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MorpheNoTitlePreferenceCategory(Context context) {
        super(context);
    }

    @Override
    @SuppressLint("MissingSuperCall")
    protected View onCreateView(ViewGroup parent) {
        return new View(getContext());
    }

    @Override
    public CharSequence getTitle() {
        if (getPreferenceCount() > 0) {
            return getPreference(0).getTitle();
        }
        return super.getTitle();
    }

    @Override
    public int getTitleRes() {
        if (getPreferenceCount() > 0) {
            return getPreference(0).getTitleRes();
        }
        return super.getTitleRes();
    }
}
