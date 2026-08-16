package app.revanced.extension.samsungkeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import app.morphe.extension.shared.settings.preference.MorphePreferenceStyle;
import app.morphe.extension.shared.settings.preference.SettingsActivityLayout;

public final class FeedbackSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsActivityLayout.applyTheme(this);
        super.onCreate(savedInstanceState);

        SettingsStore.initialize(getApplicationContext());
        String title = text("morphe_samsung_keyboard_feedback_title", "Key feedback");
        int containerId = SettingsActivityLayout.setContentView(this, title);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(28));

        TextView summary = textView(
                text(
                        "morphe_samsung_keyboard_feedback_summary",
                        "Adjust keypress sound volume and vibration strength."
                ),
                14,
                MorphePreferenceStyle.secondaryTextColor(this)
        );
        summary.setLineSpacing(dp(1), 1.0f);
        summary.setPadding(dp(17), dp(8), dp(17), dp(14));
        content.addView(summary, matchWrap());

        content.addView(section(
                text("morphe_samsung_keyboard_feedback_sound", "Sound"),
                true
        ));
        Slider soundVolume = slider(
                text("morphe_samsung_keyboard_feedback_sound_volume", "Sound volume"),
                0,
                SettingsStore.getFeedbackSoundVolume(),
                SettingsStore::setFeedbackSoundVolume,
                this::previewSound
        );
        Toggle sound = toggle(
                text("morphe_samsung_keyboard_feedback_sound_enabled", "Use sound"),
                text(
                        "morphe_samsung_keyboard_feedback_sound_enabled_summary",
                        "Play a sound when a key is pressed."
                ),
                SettingsStore.isFeedbackSoundEnabled(getContentResolver()),
                enabled -> {
                    SettingsStore.setFeedbackSoundEnabled(getContentResolver(), enabled);
                    soundVolume.setEnabled(enabled);
                    if (enabled) previewSound();
                }
        );
        content.addView(sound.root, matchWrap());
        content.addView(soundVolume.root, matchWrap());
        soundVolume.setEnabled(sound.checked);

        content.addView(section(
                text("morphe_samsung_keyboard_feedback_vibration", "Vibration"),
                false
        ));
        Slider vibrationStrength = slider(
                text("morphe_samsung_keyboard_feedback_vibration_strength", "Vibration strength"),
                1,
                Math.max(1, SettingsStore.getFeedbackVibrationStrength()),
                SettingsStore::setFeedbackVibrationStrength,
                () -> FeedbackCompat.previewVibration(this)
        );
        Toggle vibration = toggle(
                text("morphe_samsung_keyboard_feedback_vibration_enabled", "Use vibration"),
                text(
                        "morphe_samsung_keyboard_feedback_vibration_enabled_summary",
                        "Vibrate when a key is pressed."
                ),
                SettingsStore.isFeedbackVibrationEnabled(getContentResolver()),
                enabled -> {
                    SettingsStore.setFeedbackVibrationEnabled(getContentResolver(), enabled);
                    vibrationStrength.setEnabled(enabled);
                    if (enabled) FeedbackCompat.previewVibration(this);
                }
        );
        content.addView(vibration.root, matchWrap());
        content.addView(vibrationStrength.root, matchWrap());
        vibrationStrength.setEnabled(vibration.checked);

        TextView hapticSummary = textView(
                text(
                        "morphe_samsung_keyboard_feedback_system_haptic",
                        "Vibration also follows the system touch feedback setting."
                ),
                14,
                MorphePreferenceStyle.secondaryTextColor(this)
        );
        hapticSummary.setLineSpacing(dp(1), 1.0f);
        hapticSummary.setPadding(dp(17), dp(8), dp(17), 0);
        content.addView(hapticSummary, matchWrap());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setBackgroundColor(MorphePreferenceStyle.backgroundColor(this));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        FrameLayout container = findViewById(containerId);
        container.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private TextView section(String title, boolean first) {
        TextView view = textView(title, 15, MorphePreferenceStyle.primaryTextColor(this));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(17), dp(first ? 12 : 28), dp(17), dp(10));
        return view;
    }

    private Toggle toggle(
            String title,
            String summary,
            boolean checked,
            Consumer<Boolean> onChange
    ) {
        View root = MorphePreferenceStyle.createPreferenceView(
                this,
                MorphePreferenceStyle.TRAILING_SWITCH
        );
        TextView titleView = root.findViewById(android.R.id.title);
        TextView summaryView = root.findViewById(android.R.id.summary);
        titleView.setText(title);
        summaryView.setText(summary);
        root.setClickable(true);
        root.setFocusable(true);
        return new Toggle(
                root,
                MorphePreferenceStyle.findSwitch(root),
                checked,
                onChange
        );
    }

    private Slider slider(
            String title,
            int minimum,
            int value,
            IntConsumer onChange,
            Runnable preview
    ) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setMinimumHeight(dp(96));
        root.setPadding(dp(17), dp(8), dp(17), dp(16));
        root.setBackgroundColor(MorphePreferenceStyle.backgroundColor(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = textView(title, 18, MorphePreferenceStyle.primaryTextColor(this));
        header.addView(label, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        TextView amount = textView(
                percentage(value),
                14,
                MorphePreferenceStyle.primaryTextColor(this)
        );
        amount.setGravity(Gravity.CENTER);
        amount.setIncludeFontPadding(false);
        amount.setMinWidth(dp(58));
        amount.setPadding(dp(10), dp(5), dp(10), dp(5));
        amount.setBackground(roundedBackground(
                MorphePreferenceStyle.pressedBackgroundColor(this),
                12
        ));
        header.addView(amount);
        LinearLayout.LayoutParams headerParams = matchWrap();
        headerParams.bottomMargin = dp(8);
        root.addView(header, headerParams);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMin(minimum);
        seekBar.setMax(100);
        seekBar.setProgress(value);
        seekBar.setSplitTrack(false);
        seekBar.setContentDescription(title);
        int foreground = MorphePreferenceStyle.primaryTextColor(this);
        seekBar.setProgressTintList(ColorStateList.valueOf(foreground));
        seekBar.setThumbTintList(ColorStateList.valueOf(foreground));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(
                MorphePreferenceStyle.pressedBackgroundColor(this)
        ));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                amount.setText(percentage(progress));
                if (fromUser) onChange.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                preview.run();
            }
        });
        root.addView(seekBar, matchWrap());
        return new Slider(root, label, amount, seekBar);
    }

    private void previewSound() {
        if (!SettingsStore.isFeedbackSoundEnabled(getContentResolver())) return;
        AudioManager manager = getSystemService(AudioManager.class);
        if (manager != null) {
            manager.playSoundEffect(
                    AudioManager.FX_KEY_CLICK,
                    SettingsStore.getFeedbackSoundVolume() / 100.0f
            );
        }
    }

    private TextView textView(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private GradientDrawable roundedBackground(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    @SuppressLint("DiscouragedApi")
    private String text(String name, String fallback) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return id == 0 ? fallback : getString(id);
    }

    private String percentage(int value) {
        return value + "%";
    }

    private int dp(int value) {
        return MorphePreferenceStyle.dp(this, value);
    }

    private static final class Toggle {
        private final View root;
        private final MorphePreferenceStyle.SwitchView switchView;
        private final Consumer<Boolean> onChange;
        private boolean checked;

        private Toggle(
                View root,
                MorphePreferenceStyle.SwitchView switchView,
                boolean checked,
                Consumer<Boolean> onChange
        ) {
            this.root = root;
            this.switchView = switchView;
            this.checked = checked;
            this.onChange = onChange;
            switchView.setChecked(checked, false);
            MorphePreferenceStyle.bindSwitchAccessibility(root, checked);
            root.setOnClickListener(view -> setChecked(!this.checked));
        }

        private void setChecked(boolean checked) {
            this.checked = checked;
            switchView.setChecked(checked, true);
            MorphePreferenceStyle.bindSwitchAccessibility(root, checked);
            onChange.accept(checked);
        }
    }

    private static final class Slider {
        private final LinearLayout root;
        private final TextView label;
        private final TextView amount;
        private final SeekBar seekBar;

        private Slider(LinearLayout root, TextView label, TextView amount, SeekBar seekBar) {
            this.root = root;
            this.label = label;
            this.amount = amount;
            this.seekBar = seekBar;
        }

        private void setEnabled(boolean enabled) {
            root.setAlpha(enabled ? 1.0f : 0.38f);
            label.setEnabled(enabled);
            amount.setEnabled(enabled);
            seekBar.setEnabled(enabled);
        }
    }
}