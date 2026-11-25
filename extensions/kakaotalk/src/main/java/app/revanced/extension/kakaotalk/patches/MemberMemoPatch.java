package app.revanced.extension.kakaotalk.patches;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.UpdateAppearance;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

@SuppressWarnings("unused")
public final class MemberMemoPatch {
    private static final String PREFERENCES = "morphe_kakaotalk_member_memo";
    private static final int MAX_LENGTH = 100;
    private static final Map<TextView, Nickname> NICKNAMES = Collections.synchronizedMap(new WeakHashMap<>());

    private MemberMemoPatch() {
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static void bindNickname(TextView view, long userId) {
        Nickname nickname = new Nickname(userId, view.getText());
        NICKNAMES.put(view, nickname);
        render(view, nickname);
    }

    public static void attachProfileMemo(Activity activity) {
        activity.getWindow().getDecorView().post(() -> attach(activity));
    }

    private static void attach(Activity activity) {
        try {
            long userId = profileUserId(activity.getIntent());
            if (userId <= 0L) return;

            TextView chip = createChip(activity, userId);
            ViewGroup appBar = appBarRow(activity);
            if (appBar != null) {
                appBar.addView(chip, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 32), Gravity.CENTER));
                return;
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 32), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            chip.setOnApplyWindowInsetsListener((view, insets) -> {
                params.topMargin = insets.getSystemWindowInsetTop() + dp(activity, 12);
                view.setLayoutParams(params);
                return insets;
            });
            ((ViewGroup) activity.findViewById(android.R.id.content)).addView(chip, params);
            chip.requestApplyInsets();
        } catch (Exception exception) {
            Logger.printException(() -> "Could not add the member memo to the profile", exception);
        }
    }

    private static long profileUserId(Intent intent) throws Exception {
        long userId = intent.getLongExtra("user_id", 0L);
        if (userId > 0L) return userId;
        Parcelable member = intent.getParcelableExtra("member");
        return member == null ? 0L : (Long) member.getClass().getMethod("getId").invoke(member);
    }

    private static ViewGroup appBarRow(Activity activity) {
        int id = activity.getResources().getIdentifier("navigation_button_container", "id", "com.kakao.talk.openlink");
        View container = id == 0 ? null : activity.findViewById(id);
        if (!(container instanceof ViewGroup) || ((ViewGroup) container).getChildCount() == 0) return null;
        View row = ((ViewGroup) container).getChildAt(0);
        return row instanceof FrameLayout ? (ViewGroup) row : null;
    }

    private static TextView createChip(Activity activity, long userId) {
        TextView chip = new TextView(activity);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x99000000);
        background.setCornerRadius(dp(activity, 16));
        chip.setBackground(background);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setMaxWidth(dp(activity, 200));
        chip.setPadding(dp(activity, 12), 0, dp(activity, 14), 0);
        chip.setCompoundDrawablePadding(dp(activity, 6));
        Drawable icon = activity.getDrawable(android.R.drawable.ic_menu_edit);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(Color.WHITE);
            icon.setBounds(0, 0, dp(activity, 16), dp(activity, 16));
            chip.setCompoundDrawablesRelative(icon, null, null, null);
        }
        renderChip(chip, userId);
        chip.setOnClickListener(view -> editMemo(activity, userId, chip));
        return chip;
    }

    private static void editMemo(Activity activity, long userId, TextView chip) {
        String memo = getMemo(userId);
        EditText input = new EditText(activity);
        input.setHint(str("morphe_kakaotalk_member_memo_hint"));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LENGTH)});
        if (memo != null) {
            input.setText(memo);
            input.setSelection(memo.length());
        }
        FrameLayout container = new FrameLayout(activity);
        container.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), 0);
        container.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(str("morphe_kakaotalk_member_memo_title"))
                .setView(container)
                .setPositiveButton(str("morphe_kakaotalk_member_memo_save"),
                        (dialog, which) -> saveMemo(userId, input.getText().toString(), chip))
                .setNegativeButton(android.R.string.cancel, null);
        if (memo != null) {
            builder.setNeutralButton(str("morphe_kakaotalk_member_memo_delete"),
                    (dialog, which) -> saveMemo(userId, null, chip));
        }
        builder.show();
    }

    private static void saveMemo(long userId, String memo, TextView chip) {
        String key = Long.toString(userId);
        String value = memo == null ? "" : memo.trim();
        SharedPreferences.Editor editor = preferences().edit();
        if (value.isEmpty()) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.apply();

        renderChip(chip, userId);
        for (Map.Entry<TextView, Nickname> entry : snapshot()) {
            if (entry.getValue().userId == userId) render(entry.getKey(), entry.getValue());
        }
    }

    private static void renderChip(TextView chip, long userId) {
        String memo = getMemo(userId);
        chip.setText(memo == null ? str("morphe_kakaotalk_member_memo_add") : memo);
    }

    private static void render(TextView view, Nickname nickname) {
        String memo = Settings.MEMBER_MEMO_NICKNAME.get() ? getMemo(nickname.userId) : null;
        if (memo == null) {
            if (!TextUtils.equals(view.getText(), nickname.name)) view.setText(nickname.name);
            return;
        }

        SpannableStringBuilder text = new SpannableStringBuilder(nickname.name).append("  ");
        int start = text.length();
        text.append(memo.replace('\n', ' '));
        text.setSpan(new DimSpan(), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(0.9f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(text);
    }

    private static String getMemo(long userId) {
        String memo = preferences().getString(Long.toString(userId), null);
        return memo == null || memo.isEmpty() ? null : memo;
    }

    private static SharedPreferences preferences() {
        return Utils.getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static ArrayList<Map.Entry<TextView, Nickname>> snapshot() {
        synchronized (NICKNAMES) {
            return new ArrayList<>(NICKNAMES.entrySet());
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Nickname {
        final long userId;
        final CharSequence name;

        Nickname(long userId, CharSequence name) {
            this.userId = userId;
            this.name = name;
        }
    }

    private static final class DimSpan extends CharacterStyle implements UpdateAppearance {
        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setAlpha(paint.getAlpha() * 6 / 10);
        }
    }
}
