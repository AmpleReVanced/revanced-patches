package app.revanced.extension.kakaotalk.chatlog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.lang.reflect.Field;

import app.revanced.extension.kakaotalk.settings.Settings;

public final class ModifiedMessageHistoryExtension {
    private static int modifiedLabelId;
    private static int bubbleId;
    private static int messageId;
    private static int nicknameId;
    private static int profileId;
    private static int profileLayoutId;
    private static ProfileInfo cachedProfileInfo;

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
                ProfileInfo profileInfo = isMine || !Settings.showModifiedMessageSenderProfile()
                        ? null
                        : findProfileInfo(itemView);
                ModifiedMessageHistoryActivity.start(
                        view.getContext(),
                        safeHistoryJson,
                        getCurrentMessage(itemView, currentMessage),
                        isMine,
                        profileInfo == null ? null : profileInfo.nickname,
                        profileInfo == null ? null : profileInfo.profileImage
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

    private static ProfileInfo findProfileInfo(View itemView) {
        ProfileInfo profileInfo = getProfileInfo(itemView);
        if (profileInfo != null) return profileInfo;

        View current = itemView;
        ViewParent parent = current == null ? null : current.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup parentGroup = (ViewGroup) parent;
            int index = parentGroup.indexOfChild(current);
            if (index >= 0) {
                for (int i = index - 1; i >= 0; i--) {
                    profileInfo = getProfileInfo(parentGroup.getChildAt(i));
                    if (profileInfo != null) return profileInfo;
                }
            }

            current = parentGroup;
            parent = parentGroup.getParent();
        }

        return cachedProfileInfo;
    }

    private static ProfileInfo getProfileInfo(View root) {
        if (root == null) return null;

        String nickname = getNickname(root);
        Bitmap profileImage = getProfileImage(root);
        if ((nickname == null || nickname.length() == 0) && profileImage == null) return null;

        ProfileInfo profileInfo = new ProfileInfo(nickname, profileImage);
        cachedProfileInfo = profileInfo;
        return profileInfo;
    }

    private static String getNickname(View root) {
        int id = getNicknameId(root);
        if (id == 0) return null;

        View nicknameView = root.findViewById(id);
        if (!(nicknameView instanceof TextView) || !isActuallyVisible(nicknameView)) return null;

        CharSequence text = ((TextView) nicknameView).getText();
        if (text == null) return null;

        String nickname = text.toString();
        return nickname.length() == 0 ? null : nickname;
    }

    private static Bitmap getProfileImage(View root) {
        View profileView = findProfileView(root);
        if (profileView == null || !isActuallyVisible(profileView)) return null;

        return captureView(profileView);
    }

    private static View findProfileView(View root) {
        int profileLayoutId = getProfileLayoutId(root);
        if (profileLayoutId != 0) {
            View profileLayout = root.findViewById(profileLayoutId);
            if (profileLayout != null) return profileLayout;
        }

        int profileId = getProfileId(root);
        return profileId == 0 ? null : root.findViewById(profileId);
    }

    private static Bitmap captureView(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) return null;

        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);

            int maxSize = (int) (48 * view.getResources().getDisplayMetrics().density);
            int largestSide = Math.max(width, height);
            if (largestSide <= maxSize) return bitmap;

            float scale = (float) maxSize / largestSide;
            Bitmap scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    Math.max(1, (int) (width * scale)),
                    Math.max(1, (int) (height * scale)),
                    true
            );
            bitmap.recycle();
            return scaled;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isActuallyVisible(View view) {
        if (view.getVisibility() != View.VISIBLE) return false;

        ViewParent parent = view.getParent();
        while (parent instanceof View) {
            View parentView = (View) parent;
            if (parentView.getVisibility() != View.VISIBLE) return false;
            parent = parentView.getParent();
        }

        return true;
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

    private static int getNicknameId(View view) {
        if (nicknameId == 0) {
            nicknameId = view.getResources().getIdentifier(
                    "nickname",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return nicknameId;
    }

    private static int getProfileId(View view) {
        if (profileId == 0) {
            profileId = view.getResources().getIdentifier(
                    "profile",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return profileId;
    }

    private static int getProfileLayoutId(View view) {
        if (profileLayoutId == 0) {
            profileLayoutId = view.getResources().getIdentifier(
                    "profile_layout",
                    "id",
                    view.getContext().getPackageName()
            );
        }

        return profileLayoutId;
    }

    private static final class ProfileInfo {
        private final String nickname;
        private final Bitmap profileImage;

        private ProfileInfo(String nickname, Bitmap profileImage) {
            this.nickname = nickname;
            this.profileImage = profileImage;
        }
    }
}
