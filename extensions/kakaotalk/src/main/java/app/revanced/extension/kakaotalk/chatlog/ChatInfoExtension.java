package app.revanced.extension.kakaotalk.chatlog;

import android.graphics.Rect;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.kakao.talk.widget.chatlog.ChatInfoView;
import com.kakao.talk.widget.chatlog.MyChatInfoView;
import com.kakao.talk.widget.chatlog.OthersChatInfoView;

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
        deletedPaint.setColor(0xFFFF4444); // Color.RED (for deleted)

        hiddenPaint = new TextPaint(1);
        hiddenPaint.setColor(0xFF999999); // Color.GRAY (for hidden)
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
        }
    }

    private void createDeletedLayout() {
        if (deletedPaint == null) return;

        String text = "삭제됨";
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

        String text = "가려짐";
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
        if (isDeleted && deletedLayout != null) {
            return deletedLayout.getHeight();
        } else if (isHidden && hiddenLayout != null) {
            return hiddenLayout.getHeight();
        }
        return 0;
    }

    public int calculateRect(int paddingLeft, int totalWidth, int currentTop) {
        int nextTop = currentTop;

        if (isDeleted && deletedLayout != null) {
            int height = deletedLayout.getHeight() + currentTop;

            if (view instanceof MyChatInfoView) {
                int actualWidth = totalWidth - view.getPaddingLeft() - view.getPaddingRight();
                deletedRect = new Rect(
                        view.getPaddingLeft() + actualWidth - deletedLayout.getWidth(),
                        currentTop,
                        view.getPaddingLeft() + actualWidth,
                        height
                );
            } else if (view instanceof OthersChatInfoView) {
                deletedRect = new Rect(
                        paddingLeft,
                        currentTop,
                        paddingLeft + deletedLayout.getWidth(),
                        height
                );
            }
            nextTop = height;

        } else if (isHidden && hiddenLayout != null) {
            int height = hiddenLayout.getHeight() + currentTop;

            if (view instanceof MyChatInfoView) {
                int actualWidth = totalWidth - view.getPaddingLeft() - view.getPaddingRight();
                hiddenRect = new Rect(
                        view.getPaddingLeft() + actualWidth - hiddenLayout.getWidth(),
                        currentTop,
                        view.getPaddingLeft() + actualWidth,
                        height
                );
            } else if (view instanceof OthersChatInfoView) {
                hiddenRect = new Rect(
                        paddingLeft,
                        currentTop,
                        paddingLeft + hiddenLayout.getWidth(),
                        height
                );
            }
            nextTop = height;
        }

        return nextTop;
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
