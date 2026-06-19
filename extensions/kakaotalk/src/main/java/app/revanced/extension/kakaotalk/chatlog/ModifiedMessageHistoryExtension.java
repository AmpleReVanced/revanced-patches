package app.revanced.extension.kakaotalk.chatlog;

import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.lang.reflect.Field;

public final class ModifiedMessageHistoryExtension {
    private static int modifiedLabelId;
    private static int bubbleId;
    private static int messageId;

    private ModifiedMessageHistoryExtension() {
    }

    public static String mergeModifiedHistory(String currentHistory, String message, int revision) {
        return ModifiedMessageHistory.merge(currentHistory, message, revision);
    }

    public static void bindModifiedLabel(
            Object viewHolder,
            String historyJson,
            String currentMessage,
            boolean isMine
    ) {
        View itemView = getItemView(viewHolder);
        TextView label = findModifiedLabel(itemView);
        if (label == null) return;

        String safeHistoryJson = historyJson == null ? "" : historyJson;
        boolean hasHistory = !ModifiedMessageHistory.parse(safeHistoryJson).isEmpty();

        label.setOnClickListener(null);
        label.setOnLongClickListener(null);
        label.setClickable(hasHistory);
        label.setFocusable(hasHistory);
        label.setLongClickable(false);
        label.setPaintFlags(hasHistory
                ? label.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
                : label.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        label.setAlpha(hasHistory ? 0.85f : 0.6f);
        label.setImportantForAccessibility(hasHistory
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        if (!hasHistory) return;

        label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ModifiedMessageHistoryActivity.start(
                        view.getContext(),
                        safeHistoryJson,
                        getCurrentMessage(itemView, currentMessage),
                        isMine
                );
            }
        });
        label.setLongClickable(true);
        label.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return performBubbleLongClick(itemView, view);
            }
        });
    }

    private static TextView findModifiedLabel(View itemView) {
        if (itemView == null) return null;

        int labelId = getModifiedLabelId(itemView);
        if (labelId == 0) return null;

        View label = itemView.findViewById(labelId);
        return label instanceof TextView ? (TextView) label : null;
    }

    private static String getCurrentMessage(View itemView, String fallbackMessage) {
        if (fallbackMessage != null && fallbackMessage.length() > 0) return fallbackMessage;
        if (itemView == null) return null;

        int currentMessageId = getMessageId(itemView);
        if (currentMessageId == 0) return null;

        View message = itemView.findViewById(currentMessageId);
        TextView messageTextView = findTextView(message);
        if (messageTextView == null) return null;

        CharSequence text = messageTextView.getText();
        return text == null ? "" : text.toString();
    }

    private static TextView findTextView(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (!(view instanceof ViewGroup)) return null;

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            TextView textView = findTextView(viewGroup.getChildAt(i));
            if (textView != null) return textView;
        }

        return null;
    }

    private static boolean performBubbleLongClick(View itemView, View source) {
        if (performLongClick(findBubble(itemView), source)) return true;
        if (performLongClick(findMessage(itemView), source)) return true;

        ViewParent parent = source.getParent();
        while (parent instanceof View) {
            View parentView = (View) parent;
            if (performLongClick(parentView, source)) return true;
            parent = parentView.getParent();
        }

        return performLongClick(itemView, source);
    }

    private static boolean performLongClick(View target, View source) {
        return target != null && target != source && target.performLongClick();
    }

    private static View findBubble(View itemView) {
        if (itemView == null) return null;

        int id = getBubbleId(itemView);
        return id == 0 ? null : itemView.findViewById(id);
    }

    private static View findMessage(View itemView) {
        if (itemView == null) return null;

        int id = getMessageId(itemView);
        return id == 0 ? null : itemView.findViewById(id);
    }

    private static View getItemView(Object viewHolder) {
        if (viewHolder == null) return null;

        try {
            Field itemViewField = viewHolder.getClass().getField("itemView");
            Object itemView = itemViewField.get(viewHolder);
            return itemView instanceof View ? (View) itemView : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int getModifiedLabelId(View view) {
        if (modifiedLabelId == 0) {
            modifiedLabelId = view.getResources().getIdentifier(
                    "modified_label",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return modifiedLabelId;
    }

    private static int getBubbleId(View view) {
        if (bubbleId == 0) {
            bubbleId = view.getResources().getIdentifier(
                    "bubble",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return bubbleId;
    }

    private static int getMessageId(View view) {
        if (messageId == 0) {
            messageId = view.getResources().getIdentifier(
                    "message",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return messageId;
    }
}