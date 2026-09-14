package app.revanced.extension.kakaotalk.chatlog.details;

import static app.morphe.extension.shared.StringRef.str;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.MorphePreferenceStyle;
import app.morphe.extension.shared.settings.preference.SettingsActivityLayout;
import app.revanced.extension.kakaotalk.settings.MorpheSettingsIconDynamicDrawable;

public final class MessageDetailsActivity extends Activity {
    private static final String EXTRA_TOKEN = "app.revanced.extension.kakaotalk.message.details.TOKEN";
    private static final String STATE_ORIGINAL = "message_details_original";
    private static final String STATE_PAGES = "message_details_pages";
    private static final String STATE_SCROLLS = "message_details_scrolls";
    private static final String STATE_SAVE_ORIGINAL = "message_details_save_original";
    private static final int PAGE_LENGTH = 30000;
    private static final int SAVE_JSON = 1;
    private final int[] pages = new int[2];
    private final int[] scrolls = new int[2];
    private boolean darkMode;
    private boolean original;
    private boolean saveOriginal;
    private MessageDetailsSnapshot snapshot;
    private TextView typeView;
    private TextView summaryView;
    private TextView descriptionView;
    private TextView jsonView;
    private Button formattedTab;
    private Button originalTab;
    private Button copyButton;
    private Button saveButton;
    private Button previousButton;
    private Button nextButton;
    private TextView pageView;
    private LinearLayout pageBar;
    private ScrollView scrollView;
    private ProgressBar progressView;

    static void start(Activity activity, String token) {
        activity.startActivity(new Intent(activity, MessageDetailsActivity.class).putExtra(EXTRA_TOKEN, token));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());
        MorphePreferenceStyle.setThemeModeProvider(MorpheSettingsIconDynamicDrawable::isAppDarkMode);
        SettingsActivityLayout.applyTheme(this);
        super.onCreate(savedInstanceState);
        darkMode = MorphePreferenceStyle.isDark(this);
        if (savedInstanceState != null) {
            original = savedInstanceState.getBoolean(STATE_ORIGINAL);
            saveOriginal = savedInstanceState.getBoolean(STATE_SAVE_ORIGINAL);
            restorePositions(savedInstanceState, STATE_PAGES, pages);
            restorePositions(savedInstanceState, STATE_SCROLLS, scrolls);
        }
        int containerId = SettingsActivityLayout.setContentView(this, str("morphe_kakaotalk_message_details_title"));
        FrameLayout container = findViewById(containerId);
        container.addView(createContent(), new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        String token = getIntent().getStringExtra(EXTRA_TOKEN);
        MessageDetailsExtension.WORKER.execute(() -> {
            try {
                MessageDetailsSnapshot loaded = MessageDetailsSnapshot.parse(MessageDetailsStore.read(getApplicationContext(), token));
                String time = loaded.localTime;
                if (!time.isEmpty()) {
                    time = OffsetDateTime.parse(time).format(DateTimeFormatter.ofPattern("yyyy.MM.dd  HH:mm:ss XXX", Locale.getDefault()));
                }
                String summary = str("morphe_kakaotalk_message_details_id", loaded.messageId);
                if (!time.isEmpty()) summary += "\n" + time;
                String loadedSummary = summary;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    snapshot = loaded;
                    typeView.setText(loaded.messageType);
                    summaryView.setText(loadedSummary);
                    progressView.setVisibility(View.GONE);
                    copyButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    showPage(true);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    summaryView.setText(str("morphe_kakaotalk_message_details_error"));
                    progressView.setVisibility(View.GONE);
                });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        scrolls[tabIndex()] = scrollView.getScrollY();
        state.putBoolean(STATE_ORIGINAL, original);
        state.putBoolean(STATE_SAVE_ORIGINAL, saveOriginal);
        state.putIntArray(STATE_PAGES, pages);
        state.putIntArray(STATE_SCROLLS, scrolls);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MorphePreferenceStyle.isDark(this) != darkMode) recreate();
    }

    private View createContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(12));
        content.addView(createSummary(), matchWrap());
        content.addView(createTabs(), spaced(dp(14), 0));
        descriptionView = text("", 12, secondaryColor(), false);
        descriptionView.setPadding(dp(4), dp(10), dp(4), dp(12));
        content.addView(descriptionView, matchWrap());

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        content.addView(divider(), new LinearLayout.LayoutParams(MATCH_PARENT, dp(1)));
        content.addView(scrollView, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f));
        jsonView = text("", 13, primaryColor(), false);
        jsonView.setTypeface(Typeface.MONOSPACE);
        jsonView.setGravity(Gravity.TOP | Gravity.START);
        jsonView.setTextDirection(View.TEXT_DIRECTION_LTR);
        jsonView.setPadding(dp(4), dp(12), dp(4), dp(16));
        jsonView.setLineSpacing(dp(3), 1f);
        jsonView.setTextIsSelectable(true);
        jsonView.setHighlightColor(darkMode ? 0xFF354965 : 0xFFDCE9FF);
        jsonView.setHorizontallyScrolling(false);
        scrollView.addView(jsonView, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        pageBar = new LinearLayout(this);
        pageBar.setGravity(Gravity.CENTER_VERTICAL);
        pageBar.setPadding(0, dp(4), 0, 0);
        pageBar.setVisibility(View.GONE);
        previousButton = button("morphe_kakaotalk_message_details_previous");
        previousButton.setOnClickListener(view -> movePage(-1));
        nextButton = button("morphe_kakaotalk_message_details_next");
        nextButton.setOnClickListener(view -> movePage(1));
        pageView = text("", 12, secondaryColor(), true);
        pageView.setGravity(Gravity.CENTER);
        pageBar.addView(previousButton, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(48)));
        pageBar.addView(pageView, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
        pageBar.addView(nextButton, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(48)));
        content.addView(pageBar, matchWrap());
        updateTabs();
        return content;
    }

    private View createSummary() {
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.addView(createActions(), new LinearLayout.LayoutParams(MATCH_PARENT, dp(48)));
        summaryView = text(str("morphe_kakaotalk_message_details_loading"), 12, secondaryColor(), false);
        summaryView.setTextIsSelectable(true);
        summaryView.setLineSpacing(dp(3), 1f);
        summary.addView(summaryView, matchWrap());
        return summary;
    }

    private View createTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabs.setBackground(rounded(MorphePreferenceStyle.pressedBackgroundColor(this), 12));
        formattedTab = button("morphe_kakaotalk_message_details_formatted");
        originalTab = button("morphe_kakaotalk_message_details_original");
        formattedTab.setTextSize(14);
        originalTab.setTextSize(14);
        formattedTab.setOnClickListener(view -> selectTab(false));
        originalTab.setOnClickListener(view -> selectTab(true));
        tabs.addView(formattedTab, new LinearLayout.LayoutParams(0, dp(40), 1f));
        tabs.addView(originalTab, new LinearLayout.LayoutParams(0, dp(40), 1f));
        return tabs;
    }

    private View createActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        typeView = text("ChatLog", 14, primaryColor(), true);
        actions.addView(typeView, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));
        progressView = new ProgressBar(this);
        progressView.getIndeterminateDrawable().setTint(secondaryColor());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        progressParams.setMarginEnd(dp(12));
        actions.addView(progressView, progressParams);
        copyButton = button("morphe_kakaotalk_message_details_copy_short");
        copyButton.setContentDescription(str("morphe_kakaotalk_message_details_copy"));
        copyButton.setEnabled(false);
        copyButton.setOnClickListener(view -> copyJson());
        actions.addView(copyButton, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(48)));
        saveButton = button("morphe_kakaotalk_message_details_save_short");
        saveButton.setContentDescription(str("morphe_kakaotalk_message_details_save"));
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(view -> saveJson());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(WRAP_CONTENT, dp(48));
        saveParams.setMarginStart(dp(8));
        actions.addView(saveButton, saveParams);
        return actions;
    }

    private void selectTab(boolean selectedOriginal) {
        if (original == selectedOriginal) return;
        scrolls[tabIndex()] = scrollView.getScrollY();
        original = selectedOriginal;
        updateTabs();
        showPage(true);
    }

    private void updateTabs() {
        styleTab(formattedTab, !original);
        styleTab(originalTab, original);
        descriptionView.setText(str(original ? "morphe_kakaotalk_message_details_original_description"
                : "morphe_kakaotalk_message_details_formatted_description"));
    }

    private void styleTab(Button tab, boolean selected) {
        tab.setSelected(selected);
        tab.setTextColor(selected ? primaryColor() : secondaryColor());
        tab.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        tab.setBackground(ripple(selected ? (darkMode ? 0xFF3A3A3E : Color.WHITE) : Color.TRANSPARENT, 9));
    }

    private void movePage(int offset) {
        pages[tabIndex()] += offset;
        scrolls[tabIndex()] = 0;
        showPage(false);
    }

    private void showPage(boolean restoreScroll) {
        if (snapshot == null) return;
        String json = snapshot.json(original);
        int tab = tabIndex();
        int total = Math.max(1, (json.length() + PAGE_LENGTH - 1) / PAGE_LENGTH);
        int page = pages[tab] = Math.max(0, Math.min(pages[tab], total - 1));
        String part = json.substring(pageBoundary(json, page), pageBoundary(json, page + 1));
        jsonView.setText(MessageDetailsSyntax.highlight(part, darkMode, jsonView.getPaint().measureText(" ")));
        pageBar.setVisibility(total > 1 ? View.VISIBLE : View.GONE);
        pageView.setText(str("morphe_kakaotalk_message_details_page", page + 1, total));
        previousButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < total);
        int scroll = restoreScroll ? scrolls[tab] : 0;
        scrollView.post(() -> {
            if (tabIndex() == tab && pages[tab] == page) scrollView.scrollTo(0, scroll);
        });
    }

    private void saveJson() {
        if (snapshot == null) return;
        saveOriginal = original;
        try {
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE,
                            saveOriginal ? "message-raw.json" : "message.json"), SAVE_JSON);
        } catch (RuntimeException exception) {
            Toast.makeText(this, str("morphe_kakaotalk_message_details_save_error"), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_JSON || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        boolean selectedOriginal = saveOriginal;
        String contents = snapshot == null ? null : snapshot.json(selectedOriginal);
        String token = getIntent().getStringExtra(EXTRA_TOKEN);
        MessageDetailsExtension.WORKER.execute(() -> {
            String result;
            try {
                String json = contents == null ? MessageDetailsSnapshot.parse(
                        MessageDetailsStore.read(getApplicationContext(), token)).json(selectedOriginal) : contents;
                try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "wt")) {
                    if (output == null) throw new IllegalStateException("Missing document output stream");
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                }
                result = "morphe_kakaotalk_message_details_saved";
            } catch (Exception exception) {
                result = "morphe_kakaotalk_message_details_save_error";
            }
            String resultKey = result;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) Toast.makeText(this, str(resultKey), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void copyJson() {
        if (snapshot == null) return;
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(ClipData.newPlainText("message.json", snapshot.json(original)));
            Toast.makeText(this, str("morphe_kakaotalk_message_details_copied"), Toast.LENGTH_SHORT).show();
        } catch (RuntimeException exception) {
            Toast.makeText(this, str("morphe_kakaotalk_message_details_copy_error"), Toast.LENGTH_LONG).show();
        }
    }

    private Button button(String key) {
        Button button = new Button(this, null, 0);
        button.setText(str(key));
        button.setTextSize(13);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setMinWidth(dp(64));
        button.setMinimumWidth(dp(64));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setStateListAnimator(null);
        button.setBackgroundTintList(null);
        button.setBackground(new InsetDrawable(
                ripple(MorphePreferenceStyle.pressedBackgroundColor(this), 9), 0, dp(8), 0, dp(8)));
        button.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{
                MorphePreferenceStyle.disabledTextColor(this), primaryColor()}));
        return button;
    }

    private TextView text(String value, int size, int color, boolean medium) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return view;
    }

    private RippleDrawable ripple(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(darkMode ? 0x24FFFFFF : 0x14000000),
                rounded(color, radius), rounded(Color.WHITE, radius));
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(borderColor());
        return view;
    }

    private int borderColor() {
        return darkMode ? 0xFF303034 : 0xFFECECEF;
    }
    private int primaryColor() {
        return MorphePreferenceStyle.primaryTextColor(this);
    }
    private int secondaryColor() {
        return MorphePreferenceStyle.secondaryTextColor(this);
    }
    private int dp(float value) {
        return MorphePreferenceStyle.dp(this, value);
    }
    private int tabIndex() {
        return original ? 1 : 0;
    }

    private static void restorePositions(Bundle state, String key, int[] target) {
        int[] saved = state.getIntArray(key);
        if (saved != null && saved.length == target.length) System.arraycopy(saved, 0, target, 0, target.length);
    }

    private static int pageBoundary(String json, int index) {
        int end = Math.min(index * PAGE_LENGTH, json.length());
        if (end > 0 && end < json.length() && Character.isHighSurrogate(json.charAt(end - 1))) end--;
        return end;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams spaced(int top, int bottom) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, top, 0, bottom);
        return params;
    }
}