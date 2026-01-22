package app.revanced.extension.kakaotalk.chatlog;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.kakao.talk.widget.chatlog.ChatInfoView;
import com.kakao.talk.widget.chatlog.MyChatInfoView;
import com.kakao.talk.widget.chatlog.OthersChatInfoView;

import static app.revanced.extension.shared.StringRef.str;

public class ChatInfoExtension {

    // Related to a deleted message
    private boolean isDeleted = false;
    private Layout deletedLayout = null;
    private Rect deletedRect = null;
    private TextPaint deletedPaint = null;

    // Related to a hidden message
    private boolean isHidden = false;
    private Layout hiddenLayout = null;
    private Rect hiddenRect = null;
    private TextPaint hiddenPaint = null;

    private final ChatInfoView view;

    public ChatInfoExtension(ChatInfoView view) {
        this.view = view;
        initializePaints();
    }

    private void initializePaints() {
        deletedPaint = new TextPaint(1);
        deletedPaint.setColor(getDeletedColor());

        hiddenPaint = new TextPaint(1);
        hiddenPaint.setColor(getHiddenColor());
    }

    private static int getDeletedColor() {
        return 0xFFFF4444; // Modified during patching.
    }

    private static int getHiddenColor() {
        return 0xFF999999; // Modified during patching.
    }

    private void ensurePaintsInitialized() {
        TextPaint sourcePaint = null;

        Layout modifyLayout = view.getModifyLayout();
        if (modifyLayout != null && modifyLayout.getPaint() != null) {
            sourcePaint = (TextPaint) modifyLayout.getPaint();
        }

        if (sourcePaint != null) {
            deletedPaint.setTextSize(sourcePaint.getTextSize());
            deletedPaint.setTypeface(sourcePaint.getTypeface());

            hiddenPaint.setTextSize(sourcePaint.getTextSize());
            hiddenPaint.setTypeface(sourcePaint.getTypeface());
        } else {
            try {
                int textSizeResId = view.getResources().getIdentifier(
                        "font_level_small", "dimen", view.getContext().getPackageName()
                );
                if (textSizeResId != 0) {
                    float textSize = view.getResources().getDimension(textSizeResId);
                    deletedPaint.setTextSize(textSize);
                    hiddenPaint.setTextSize(textSize);
                } else {
                    float density = view.getResources().getDisplayMetrics().density;
                    float defaultTextSize = 10 * density;
                    deletedPaint.setTextSize(defaultTextSize);
                    hiddenPaint.setTextSize(defaultTextSize);
                }
            } catch (Exception e) {
                float density = view.getResources().getDisplayMetrics().density;
                float defaultTextSize = 10 * density;
                deletedPaint.setTextSize(defaultTextSize);
                hiddenPaint.setTextSize(defaultTextSize);
            }
        }
    }

    public void setDeleted(boolean deleted) {
        if (this.isDeleted != deleted) {
            this.isDeleted = deleted;
            if (deleted) {
                ensurePaintsInitialized();
                createDeletedLayout();
            } else {
                deletedLayout = null;
                deletedRect = null;
            }
            view.requestLayout();
            view.invalidate();
        }
    }

    public void setHidden(boolean hidden) {
        if (this.isHidden != hidden) {
            this.isHidden = hidden;
            if (hidden) {
                ensurePaintsInitialized();
                createHiddenLayout();
            } else {
                hiddenLayout = null;
                hiddenRect = null;
            }
            view.requestLayout();
            view.invalidate();
        }
    }

    private void createDeletedLayout() {
        if (deletedPaint == null) return;

        String text = str("revanced_kakaotalk_chatlog_deleted");
        BoringLayout.Metrics metrics = BoringLayout.isBoring(text, deletedPaint);

        if (metrics == null) {
            deletedLayout = new StaticLayout(
                    text,
                    deletedPaint,
                    (int) deletedPaint.measureText(text),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    false
            );
        } else {
            deletedLayout = BoringLayout.make(
                    text,
                    deletedPaint,
                    (int) deletedPaint.measureText(text),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    metrics,
                    false
            );
        }
    }

    private void createHiddenLayout() {
        if (hiddenPaint == null) return;

        String text = str("revanced_kakaotalk_chatlog_hidden");
        BoringLayout.Metrics metrics = BoringLayout.isBoring(text, hiddenPaint);

        if (metrics == null) {
            hiddenLayout = new StaticLayout(
                    text,
                    hiddenPaint,
                    (int) hiddenPaint.measureText(text),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    false
            );
        } else {
            hiddenLayout = BoringLayout.make(
                    text,
                    hiddenPaint,
                    (int) hiddenPaint.measureText(text),
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    metrics,
                    false
            );
        }
    }

    public int getAdditionalWidth() {
        int deletedWidth = (isDeleted && deletedLayout != null) ? deletedLayout.getWidth() : 0;
        int hiddenWidth = (isHidden && hiddenLayout != null) ? hiddenLayout.getWidth() : 0;
        return Math.max(deletedWidth, hiddenWidth);
    }

    public int getAdditionalHeight() {
        int extensionHeight = 0;

        if (isDeleted && deletedLayout != null) {
            extensionHeight = deletedLayout.getHeight();
        } else if (isHidden && hiddenLayout != null) {
            extensionHeight = hiddenLayout.getHeight();
        }

        try {
            Bitmap chatThreadIcon = view.getChatThreadShareIcon();

            if (chatThreadIcon != null) {
                Layout unreadLayout = view.getUnreadLayout();
                int chatThreadHeight = chatThreadIcon.getHeight();

                int spacing = 0;
                try {
                    spacing = view.getResources().getDimensionPixelSize(
                            view.getResources().getIdentifier(
                                    "chat_info_spacing_2", "dimen", view.getContext().getPackageName()
                            )
                    );
                } catch (Exception e) {
                    float density = view.getResources().getDisplayMetrics().density;
                    spacing = (int) (2 * density);
                }

                if (unreadLayout != null) {
                    int unreadHeight = unreadLayout.getHeight();
                    int correction = (chatThreadHeight + spacing) - unreadHeight;
                    if (correction > 0) {
                        extensionHeight += correction;
                    }
                } else {
                    extensionHeight += spacing;
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return extensionHeight;
    }

    public int calculateRect(int paddingLeft, int totalWidth, int currentTop) {
        if (!isDeleted && !isHidden) {
            return currentTop;
        }

        Layout targetLayout = isDeleted ? deletedLayout : hiddenLayout;
        if (targetLayout == null) {
            return currentTop;
        }

        int bottom = currentTop + targetLayout.getHeight();

        if (isDeleted) {
            if (view instanceof MyChatInfoView) {
                int actualWidth = totalWidth - view.getPaddingLeft() - view.getPaddingRight();
                deletedRect = new Rect(
                        view.getPaddingLeft() + actualWidth - deletedLayout.getWidth(),
                        currentTop,
                        view.getPaddingLeft() + actualWidth,
                        bottom
                );
            } else if (view instanceof OthersChatInfoView) {
                deletedRect = new Rect(
                        paddingLeft,
                        currentTop,
                        paddingLeft + deletedLayout.getWidth(),
                        bottom
                );
            }
        } else if (isHidden) {
            if (view instanceof MyChatInfoView) {
                int actualWidth = totalWidth - view.getPaddingLeft() - view.getPaddingRight();
                hiddenRect = new Rect(
                        view.getPaddingLeft() + actualWidth - hiddenLayout.getWidth(),
                        currentTop,
                        view.getPaddingLeft() + actualWidth,
                        bottom
                );
            } else if (view instanceof OthersChatInfoView) {
                hiddenRect = new Rect(
                        paddingLeft,
                        currentTop,
                        paddingLeft + hiddenLayout.getWidth(),
                        bottom
                );
            }
        }

        return bottom;
    }

    public void draw(android.graphics.Canvas canvas) {
        if (isDeleted && deletedLayout != null && deletedRect != null) {
            canvas.save();
            canvas.translate(deletedRect.left, deletedRect.top);
            deletedLayout.draw(canvas);
            canvas.restore();

        } else if (isHidden && hiddenLayout != null && hiddenRect != null) {
            canvas.save();
            canvas.translate(hiddenRect.left, hiddenRect.top);
            hiddenLayout.draw(canvas);
            canvas.restore();
        }
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public Layout getDeletedLayout() {
        return deletedLayout;
    }

    public Layout getHiddenLayout() {
        return hiddenLayout;
    }

    public Rect getDeletedRect() {
        return deletedRect;
    }

    public Rect getHiddenRect() {
        return hiddenRect;
    }

}