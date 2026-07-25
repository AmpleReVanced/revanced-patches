package app.revanced.extension.kakaotalk.chatlog;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;

public final class OpenChatFeedProfileClickableSpan extends ClickableSpan {
    private static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    private static final String OPEN_PROFILE_METHOD = "revanced_openOpenChatFeedProfile";

    private final Object holder;
    private final Object member;

    public OpenChatFeedProfileClickableSpan(Object holder, Object member) {
        this.holder = holder;
        this.member = member;
    }

    public static void bind(TextView textView, String nickname, Object holder, Object member) {
        if (textView == null || nickname == null || nickname.isEmpty()) {
            return;
        }

        CharSequence text = textView.getText();
        if (text == null) {
            return;
        }

        int start = text.toString().indexOf(nickname);
        if (start < 0) {
            return;
        }

        SpannableString spannable = new SpannableString(text);
        for (OpenChatFeedProfileClickableSpan staleSpan :
                spannable.getSpans(0, spannable.length(), OpenChatFeedProfileClickableSpan.class)) {
            spannable.removeSpan(staleSpan);
        }
        spannable.setSpan(
                new OpenChatFeedProfileClickableSpan(holder, member),
                start,
                start + nickname.length(),
                SPAN_FLAGS
        );
        textView.setText(spannable);
    }

    @Override
    public void onClick(View view) {
        try {
            Method method = holder.getClass().getMethod(OPEN_PROFILE_METHOD, Object.class, View.class);
            method.invoke(holder, member, view);
        } catch (Exception ignored) {
        }
    }
}
