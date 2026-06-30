/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

@SuppressWarnings("unused")
public class MorphePreferenceRowLayout extends FrameLayout {
    private View row;

    public MorphePreferenceRowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        row = MorphePreferenceStyle.createPreferenceViewWithIconSlot(
                context,
                MorphePreferenceStyle.TRAILING_CHEVRON
        );
        MorphePreferenceStyle.setTrailingVisible(row, false);
        addView(row, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (row != null) {
            row.setPressed(pressed);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (row != null) {
            row.setEnabled(enabled);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        MorphePreferenceStyle.syncPreferenceScreenRow(this);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MorphePreferenceStyle.syncPreferenceScreenRow(this);
    }
}
