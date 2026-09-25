package app.revanced.extension.kakaotalk.chatlog.details;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MessageDetailsSyntax {
    private static final Pattern TOKEN = Pattern.compile(
            "(\"(?:\\\\.|[^\"\\\\])*+\")|\\b(true|false|null)\\b|(-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");

    private MessageDetailsSyntax() {
    }

    static CharSequence highlight(String json, boolean dark, float spaceWidth) {
        SpannableString text = new SpannableString(json);
        Matcher matcher = TOKEN.matcher(json);
        while (matcher.find()) {
            int color;
            if (matcher.group(1) != null) {
                int next = matcher.end();
                while (next < json.length() && Character.isWhitespace(json.charAt(next))) next++;
                boolean key = next < json.length() && json.charAt(next) == ':';
                color = key ? (dark ? 0xFF9BC9FF : 0xFF245CA6) : (dark ? 0xFFB0D89A : 0xFF28744A);
            } else if (matcher.group(2) != null) {
                color = dark ? 0xFFD5B7FF : 0xFF8350AA;
            } else {
                color = dark ? 0xFFF2C38B : 0xFFA95C18;
            }
            text.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int start = 0;
        while (start < json.length()) {
            int end = json.indexOf('\n', start);
            if (end < 0) end = json.length();
            int indent = 0;
            while (start + indent < end && json.charAt(start + indent) == ' ') indent++;
            if (end > start) {
                text.setSpan(new LeadingMarginSpan.Standard(0, Math.round(Math.min(indent + 2, 8) * spaceWidth)),
                        start, Math.min(end + 1, json.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            start = end + 1;
        }
        return text;
    }
}