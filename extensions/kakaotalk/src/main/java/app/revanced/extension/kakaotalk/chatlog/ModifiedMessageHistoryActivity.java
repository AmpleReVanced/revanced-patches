package app.revanced.extension.kakaotalk.chatlog;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;

public final class ModifiedMessageHistoryActivity extends Activity {
    private static final String EXTRA_HISTORY_JSON =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_HISTORY_JSON";
    private static final String EXTRA_CURRENT_MESSAGE =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_CURRENT_MESSAGE";
    private static final String EXTRA_IS_MINE =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_IS_MINE";

    public static void start(Context context, String historyJson, String currentMessage, boolean isMine) {
        Intent intent = new Intent(context, ModifiedMessageHistoryActivity.class);
        intent.putExtra(EXTRA_HISTORY_JSON, historyJson);
        intent.putExtra(EXTRA_CURRENT_MESSAGE, currentMessage);
        intent.putExtra(EXTRA_IS_MINE, isMine);

        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);
        setTitle(resString("morphe_kakaotalk_chatlog_modified_history_title", "Edit history"));
        applyStatusBarColor();

        String historyJson = getIntent().getStringExtra(EXTRA_HISTORY_JSON);
        String currentMessage = getIntent().getStringExtra(EXTRA_CURRENT_MESSAGE);
        boolean isMine = getIntent().getBooleanExtra(EXTRA_IS_MINE, false);
        setContentView(createScreen(ModifiedMessageHistory.parse(historyJson), currentMessage, isMine));
    }

    private View createScreen(
            List<ModifiedMessageHistory.Message> messages,
            String currentMessage,
            boolean isMine
    ) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(resolveColor("theme_chatroom_background_color", 0xFFABC1D1));
        root.setPadding(0, getStatusBarHeight(), 0, 0);

        root.addView(createHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));
        root.addView(createContent(messages, currentMessage, isMine), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));

        return root;
    }

    private void applyStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(resolveColor("daynight_white000s", Color.WHITE));
        }
    }

    private View createHeader() {
        int titleColor = resolveColor("daynight_gray990s", Color.BLACK);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(resolveColor("daynight_white000s", Color.WHITE));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            header.setElevation(dp(2));
        }

        TextView backButton = createText("\u2039", 34, titleColor, Typeface.NORMAL);
        backButton.setGravity(Gravity.CENTER);
        backButton.setContentDescription("Back");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        header.addView(backButton, new LinearLayout.LayoutParams(
                dp(52),
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        TextView title = createText(
                resString("morphe_kakaotalk_chatlog_modified_history_title", "Edit history"),
                18,
                titleColor,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        ));

        header.addView(new View(this), new LinearLayout.LayoutParams(
                dp(52),
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        return header;
    }

    private View createContent(
            List<ModifiedMessageHistory.Message> messages,
            String currentMessage,
            boolean isMine
    ) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(resolveColor("theme_chatroom_background_color", 0xFFABC1D1));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(14), dp(16), dp(24));

        if (messages.isEmpty() && currentMessage == null) {
            TextView emptyView = createText(
                    resString("morphe_kakaotalk_chatlog_modified_history_no_items", "No edit history"),
                    15,
                    resolveColor("daynight_gray550s", 0xFF777777),
                    Typeface.NORMAL
            );
            emptyView.setGravity(Gravity.CENTER);
            container.addView(emptyView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        } else {
            for (int i = 0; i < messages.size(); i++) {
                container.addView(createMessageRow(messages.get(i), i, isMine));
            }

            if (currentMessage != null) {
                container.addView(createCurrentMessageRow(currentMessage, messages.size(), isMine));
            }
        }

        scrollView.addView(container, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createCurrentMessageRow(String currentMessage, int index, boolean isMine) {
        return createMessageRow(
                str("morphe_kakaotalk_chatlog_modified_history_current"),
                currentMessage,
                index,
                isMine
        );
    }

    private View createMessageRow(ModifiedMessageHistory.Message message, int index, boolean isMine) {
        return createMessageRow(
                String.format(
                        Locale.getDefault(),
                        str("morphe_kakaotalk_chatlog_modified_history_revision"),
                        message.revision
                ),
                message.message,
                index,
                isMine
        );
    }

    private View createMessageRow(String labelText, String messageText, int index, boolean isMine) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(isMine ? Gravity.RIGHT : Gravity.LEFT);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (index > 0) {
            rowParams.topMargin = dp(14);
        }
        row.setLayoutParams(rowParams);

        TextView label = createText(
                labelText,
                12,
                resolveColor("daynight_gray550s", 0xFF777777),
                Typeface.NORMAL
        );
        label.setGravity(isMine ? Gravity.RIGHT : Gravity.LEFT);
        row.addView(label, bubbleLayoutParams(isMine, false));

        TextView bubble = createText(
                messageText.length() == 0
                        ? str("morphe_kakaotalk_chatlog_modified_history_empty")
                        : messageText,
                16,
                resolveColor(
                        isMine ? "theme_chatroom_bubble_me_color" : "theme_chatroom_bubble_you_color",
                        Color.BLACK
                ),
                Typeface.NORMAL
        );
        bubble.setTextIsSelectable(true);
        bubble.setMaxWidth(resolveDimension(
                "bubble_width_text",
                getResources().getDisplayMetrics().widthPixels - dp(112)
        ));
        applyBubbleBackground(bubble, isMine);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        row.addView(bubble, bubbleLayoutParams(isMine, true));

        return row;
    }

    private LinearLayout.LayoutParams bubbleLayoutParams(boolean isMine, boolean body) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = isMine ? Gravity.RIGHT : Gravity.LEFT;
        params.leftMargin = isMine ? dp(56) : 0;
        params.rightMargin = isMine ? 0 : dp(56);
        if (body) {
            params.topMargin = dp(4);
        }
        return params;
    }

    private void applyBubbleBackground(TextView bubble, boolean isMine) {
        int drawableId = ResourceHelper.getResourceId(
                "drawable",
                isMine ? "chatroom_message_bubble_me_no_tail_bg_n" : "chatroom_message_bubble_you_no_tail_bg_n"
        );
        if (drawableId != 0) {
            bubble.setBackgroundResource(drawableId);
            return;
        }

        bubble.setBackground(createFallbackBubbleBackground(isMine));
    }

    private GradientDrawable createFallbackBubbleBackground(boolean isMine) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(isMine
                ? resolveColor("theme_chatroom_input_bar_send_button_color", 0xFFFEE500)
                : Color.WHITE);
        drawable.setCornerRadius(dp(12));
        return drawable;
    }

    private TextView createText(String text, int sp, int color, int typefaceStyle) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, typefaceStyle);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    private int resolveColor(String name, int fallback) {
        int colorId = ResourceHelper.getResourceId("color", name);
        if (colorId != 0) {
            try {
                return getColor(colorId);
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private int resolveDimension(String name, int fallback) {
        int dimenId = ResourceHelper.getResourceId("dimen", name);
        if (dimenId != 0) {
            try {
                return getResources().getDimensionPixelSize(dimenId);
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private String resString(String name, String fallback) {
        int resourceId = ResourceHelper.getResourceId("string", name);
        return resourceId == 0 ? fallback : getString(resourceId);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId == 0 ? 0 : getResources().getDimensionPixelSize(resourceId);
    }
}