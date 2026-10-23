package app.revanced.extension.kakaotalk.patches;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class OpenChatMemberActions {
    private static final String BLOCK_BUTTON_TAG = "morphe_open_chat_block_button";

    public static TextView getBlockButton(TextView original) {
        ViewGroup parent = (ViewGroup) original.getParent();
        TextView button = parent.findViewWithTag(BLOCK_BUTTON_TAG);
        if (button == null) {
            button = new TextView(original.getContext());
            button.setTag(BLOCK_BUTTON_TAG);
            button.setGravity(original.getGravity());
            button.setTypeface(original.getTypeface());
            button.setMinWidth(original.getMinWidth());
            button.setMinHeight(original.getMinHeight());
            button.setPadding(original.getPaddingLeft(), original.getPaddingTop(),
                    original.getPaddingRight(), original.getPaddingBottom());
            button.setCompoundDrawablePadding(original.getCompoundDrawablePadding());
            parent.addView(button, new LinearLayout.LayoutParams(original.getLayoutParams()));
        }
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, original.getTextSize());
        button.setTextColor(original.getTextColors());
        setEqualButtonWidths(parent, true);
        return button;
    }

    public static void hideBlockButton(Activity activity) {
        View button = activity.getWindow().getDecorView().findViewWithTag(BLOCK_BUTTON_TAG);
        if (button != null) {
            button.setVisibility(View.GONE);
            setEqualButtonWidths((ViewGroup) button.getParent(), false);
        }
    }

    private static void setEqualButtonWidths(ViewGroup parent, boolean enabled) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = enabled ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
            params.weight = enabled ? 1 : 0;
            child.setLayoutParams(params);
        }
    }
}