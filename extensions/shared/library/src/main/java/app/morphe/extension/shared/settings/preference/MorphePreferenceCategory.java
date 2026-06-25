/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressWarnings({"deprecation", "unused"})
public class MorphePreferenceCategory extends PreferenceCategory {
    public MorphePreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MorphePreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MorphePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MorphePreferenceCategory(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        TextView title = new TextView(getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        title.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        applyCategoryStyle(title);
        return title;
    }

    @Override
    protected void onBindView(View view) {
        TextView title = (TextView) view;
        title.setText(getTitle());
        title.setTextColor(MorphePreferenceStyle.primaryTextColor(getContext()));
        title.setEnabled(isEnabled());
        applyCategoryStyle(title);
    }

    private void applyCategoryStyle(TextView title) {
        int topPadding = isFirstCategory() ? 20 : 30;
        title.setPadding(
                MorphePreferenceStyle.dp(getContext(), 17),
                MorphePreferenceStyle.dp(getContext(), topPadding),
                MorphePreferenceStyle.dp(getContext(), 24),
                MorphePreferenceStyle.dp(getContext(), 10)
        );
        title.setBackgroundColor(MorphePreferenceStyle.backgroundColor(getContext()));
    }

    private boolean isFirstCategory() {
        PreferenceGroup parent = getParent();
        return parent == null || parent.getPreferenceCount() == 0 || parent.getPreference(0) == this;
    }
}
