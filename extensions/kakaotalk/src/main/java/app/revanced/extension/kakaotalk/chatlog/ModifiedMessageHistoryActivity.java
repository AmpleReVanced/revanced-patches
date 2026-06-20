package app.revanced.extension.kakaotalk.chatlog;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
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
    private static final String EXTRA_DARK_MODE =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_DARK_MODE";
    private static final String EXTRA_PROFILE_NICKNAME =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_PROFILE_NICKNAME";
    private static final String EXTRA_PROFILE_IMAGE =
            "app.revanced.extension.kakaotalk.chatlog.EXTRA_PROFILE_IMAGE";
    private static final int LIGHT_CHAT_BACKGROUND = 0xFFABC1D1;
    private static final int DARK_CHAT_BACKGROUND = 0xFF080808;
    private static final int LIGHT_SURFACE = LIGHT_CHAT_BACKGROUND;
    private static final int DARK_SURFACE = 0xFF080808;
    private static final int LIGHT_TITLE = Color.BLACK;
    private static final int DARK_TITLE = Color.WHITE;
    private static final int LIGHT_SECONDARY_TEXT = 0xFF767676;
    private static final int DARK_SECONDARY_TEXT = 0xFFA6A6A6;
    private static final int LIGHT_NICKNAME_TEXT = 0xFF424242;
    private static final int DARK_NICKNAME_TEXT = 0xFFDFDFDF;
    private static final int LIGHT_MESSAGE_TEXT = 0xFF191919;
    private static final int DARK_MESSAGE_TEXT = 0xFFF2F2F2;
    private static final int MY_BUBBLE = 0xFFFEE500;
    private static final int LIGHT_OTHER_BUBBLE = Color.WHITE;
    private static final int DARK_OTHER_BUBBLE = 0xFF202020;
    private static final int LIGHT_PROFILE_FALLBACK = 0xFFCCCCCC;
    private static final int DARK_PROFILE_FALLBACK = 0xFF5D5D5D;

    private boolean darkMode;

    public static void start(
            Context context,
            String historyJson,
            String currentMessage,
            boolean isMine,
            boolean darkMode,
            String profileNickname,
            Bitmap profileImage
    ) {
        Intent intent = new Intent(context, ModifiedMessageHistoryActivity.class);
        intent.putExtra(EXTRA_HISTORY_JSON, historyJson);
        intent.putExtra(EXTRA_CURRENT_MESSAGE, currentMessage);
        intent.putExtra(EXTRA_IS_MINE, isMine);
        intent.putExtra(EXTRA_DARK_MODE, darkMode);
        intent.putExtra(EXTRA_PROFILE_NICKNAME, profileNickname);
        if (profileImage != null) {
            intent.putExtra(EXTRA_PROFILE_IMAGE, profileImage);
        }

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

        String historyJson = getIntent().getStringExtra(EXTRA_HISTORY_JSON);
        String currentMessage = getIntent().getStringExtra(EXTRA_CURRENT_MESSAGE);
        boolean isMine = getIntent().getBooleanExtra(EXTRA_IS_MINE, false);
        darkMode = getIntent().getBooleanExtra(EXTRA_DARK_MODE, isSystemDarkMode());
        String profileNickname = getIntent().getStringExtra(EXTRA_PROFILE_NICKNAME);
        Bitmap profileImage = getIntent().getParcelableExtra(EXTRA_PROFILE_IMAGE);
        applySystemBarColors();

        setContentView(createScreen(
                ModifiedMessageHistory.parse(historyJson),
                currentMessage,
                isMine,
                profileNickname,
                profileImage
        ));
    }

    private View createScreen(
            List<ModifiedMessageHistory.Message> messages,
            String currentMessage,
            boolean isMine,
            String profileNickname,
            Bitmap profileImage
    ) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(chatBackgroundColor());
        root.setPadding(0, getStatusBarHeight(), 0, 0);

        root.addView(createHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));
        root.addView(createContent(messages, currentMessage, isMine, profileNickname, profileImage), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));

        return root;
    }

    private void applySystemBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(surfaceColor());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (darkMode) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(surfaceColor());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            header.setElevation(dp(2));
        }

        TextView backButton = createText("\u2039", 34, titleColor(), Typeface.NORMAL);
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
                titleColor(),
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
            boolean isMine,
            String profileNickname,
            Bitmap profileImage
    ) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(chatBackgroundColor());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(14), dp(16), dp(24));

        if (messages.isEmpty() && currentMessage == null) {
            TextView emptyView = createText(
                    resString("morphe_kakaotalk_chatlog_modified_history_no_items", "No edit history"),
                    15,
                    secondaryTextColor(),
                    Typeface.NORMAL
            );
            emptyView.setGravity(Gravity.CENTER);
            container.addView(emptyView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        } else {
            for (int i = 0; i < messages.size(); i++) {
                container.addView(createMessageRow(messages.get(i), i, isMine, profileNickname, profileImage));
            }

            if (currentMessage != null) {
                container.addView(createCurrentMessageRow(
                        currentMessage,
                        messages.size(),
                        isMine,
                        profileNickname,
                        profileImage
                ));
            }
        }

        scrollView.addView(container, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    private View createCurrentMessageRow(
            String currentMessage,
            int index,
            boolean isMine,
            String profileNickname,
            Bitmap profileImage
    ) {
        return createMessageRow(
                str("morphe_kakaotalk_chatlog_modified_history_current"),
                currentMessage,
                index,
                isMine,
                profileNickname,
                profileImage
        );
    }

    private View createMessageRow(
            ModifiedMessageHistory.Message message,
            int index,
            boolean isMine,
            String profileNickname,
            Bitmap profileImage
    ) {
        return createMessageRow(
                String.format(
                        Locale.getDefault(),
                        str("morphe_kakaotalk_chatlog_modified_history_revision"),
                        message.revision
                ),
                message.message,
                index,
                isMine,
                profileNickname,
                profileImage
        );
    }

    private View createMessageRow(
            String labelText,
            String messageText,
            int index,
            boolean isMine,
            String profileNickname,
            Bitmap profileImage
    ) {
        if (!isMine && (hasText(profileNickname) || profileImage != null)) {
            return createOtherMessageRow(labelText, messageText, index, profileNickname, profileImage);
        }

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
                secondaryTextColor(),
                Typeface.NORMAL
        );
        label.setGravity(isMine ? Gravity.RIGHT : Gravity.LEFT);
        row.addView(label, bubbleLayoutParams(isMine, false));

        TextView bubble = createText(
                messageOrEmpty(messageText).length() == 0
                        ? str("morphe_kakaotalk_chatlog_modified_history_empty")
                        : messageOrEmpty(messageText),
                16,
                messageTextColor(isMine),
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

    private View createOtherMessageRow(
            String labelText,
            String messageText,
            int index,
            String profileNickname,
            Bitmap profileImage
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (index > 0) {
            rowParams.topMargin = dp(14);
        }
        row.setLayoutParams(rowParams);

        row.addView(createProfileImage(profileImage), profileLayoutParams());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        if (hasText(profileNickname)) {
            TextView nickname = createText(
                    profileNickname,
                    12,
                    nicknameTextColor(),
                    Typeface.NORMAL
            );
            content.addView(nickname, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }

        TextView label = createText(
                labelText,
                12,
                secondaryTextColor(),
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (hasText(profileNickname)) {
            labelParams.topMargin = dp(2);
        }
        content.addView(label, labelParams);

        TextView bubble = createText(
                messageOrEmpty(messageText).length() == 0
                        ? str("morphe_kakaotalk_chatlog_modified_history_empty")
                        : messageOrEmpty(messageText),
                16,
                messageTextColor(false),
                Typeface.NORMAL
        );
        bubble.setTextIsSelectable(true);
        bubble.setMaxWidth(resolveDimension(
                "bubble_width_text",
                getResources().getDisplayMetrics().widthPixels - dp(136)
        ));
        applyBubbleBackground(bubble, false);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        content.addView(bubble, bubbleLayoutParams(false, true));

        row.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return row;
    }

    private ImageView createProfileImage(Bitmap profileImage) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        if (profileImage != null) {
            imageView.setImageBitmap(profileImage);
        } else {
            imageView.setBackground(createFallbackProfileBackground());
        }
        return imageView;
    }

    private GradientDrawable createFallbackProfileBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(profileFallbackColor());
        return drawable;
    }

    private LinearLayout.LayoutParams profileLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        params.rightMargin = dp(8);
        return params;
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
        bubble.setBackground(createBubbleBackground(isMine));
    }

    private GradientDrawable createBubbleBackground(boolean isMine) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(isMine ? MY_BUBBLE : otherBubbleColor());
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

    private boolean hasText(String text) {
        return text != null && text.length() > 0;
    }

    private String messageOrEmpty(String message) {
        return message == null ? "" : message;
    }

    private int chatBackgroundColor() {
        return darkMode ? DARK_CHAT_BACKGROUND : LIGHT_CHAT_BACKGROUND;
    }

    private int surfaceColor() {
        return darkMode ? DARK_SURFACE : LIGHT_SURFACE;
    }

    private int titleColor() {
        return darkMode ? DARK_TITLE : LIGHT_TITLE;
    }

    private int secondaryTextColor() {
        return darkMode ? DARK_SECONDARY_TEXT : LIGHT_SECONDARY_TEXT;
    }

    private int nicknameTextColor() {
        return darkMode ? DARK_NICKNAME_TEXT : LIGHT_NICKNAME_TEXT;
    }

    private int messageTextColor(boolean isMine) {
        return isMine ? LIGHT_MESSAGE_TEXT : darkMode ? DARK_MESSAGE_TEXT : LIGHT_MESSAGE_TEXT;
    }

    private int otherBubbleColor() {
        return darkMode ? DARK_OTHER_BUBBLE : LIGHT_OTHER_BUBBLE;
    }

    private int profileFallbackColor() {
        return darkMode ? DARK_PROFILE_FALLBACK : LIGHT_PROFILE_FALLBACK;
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

    private boolean isSystemDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }
}
