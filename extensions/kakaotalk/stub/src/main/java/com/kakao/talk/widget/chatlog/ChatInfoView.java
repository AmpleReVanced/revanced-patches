package com.kakao.talk.widget.chatlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Stub class for ChatInfoView
 * This class provides a public interface to interact with ChatInfoView from external modules
 */
public abstract class ChatInfoView extends View {

    public ChatInfoView(Context context) {
        super(context);
    }

    public ChatInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Abstract methods that must be implemented by subclasses
    public abstract int getTotalWidth();
    public abstract void makeRect();

    // Date related methods
    public abstract String getDate();
    public abstract void setDate(String date);
    public abstract int getDateTextWidth(String date);
    public abstract int getDateTextColor();
    public abstract void setDateTextColor(int color);
    public abstract void hideDate(boolean hide);
    public abstract boolean isDateHidden();
    public abstract void setDateHidden(boolean hidden);

    // Layout getters and setters
    public abstract Layout getDateLayout();
    public abstract void setDateLayout(Layout layout);
    public abstract Layout getUnreadLayout();
    public abstract void setUnreadLayout(Layout layout);
    public abstract Layout getModifyLayout();
    public abstract void setModifyLayout(Layout layout);

    // Rect getters and setters
    public abstract Rect getDateRect();
    public abstract void setDateRect(Rect rect);
    public abstract Rect getUnreadRect();
    public abstract void setUnreadRect(Rect rect);
    public abstract Rect getBookmarkRect();
    public abstract void setBookmarkRect(Rect rect);
    public abstract Rect getModifyRect();
    public abstract void setModifyRect(Rect rect);
    public abstract Rect getChatThreadShareRect();
    public abstract void setChatThreadShareRect(Rect rect);

    // Bitmap getters and setters
    public abstract Bitmap getBookmarkIcon();
    public abstract void setBookmarkIcon(Bitmap bitmap);
    public abstract Bitmap getChatThreadShareIcon();
    public abstract void setChatThreadShareIcon(Bitmap bitmap);

    // Bookmark related methods
    public abstract boolean isBookmarking();
    public abstract void setBookmarking(boolean bookmarking);

    // Unread count methods
    public abstract int getUnreadCount();
    public abstract void setUnreadCount(int count);
    public abstract int getUnreadTextColor();
    public abstract void setUnreadTextColor(int color);
    public abstract void enableUnreadShadow(boolean enable);

    // Modify related methods
    public abstract boolean isModify();
    public abstract void setModify(boolean modify);
    public abstract int getModifyTextColor();
    public abstract void setModifyTextColor(int color);

    // Chat thread share methods
    public abstract boolean isSharedToChatRoomFromChatThread();
    public abstract void setSharedToChatRoomFromChatThread(boolean shared);

    // Chat log ID methods
    public abstract void setChatLogId(long chatLogId);

    // State checking methods
    public abstract boolean isHidden();
    public abstract boolean isAnimating();

    // Spacing
    public abstract int getBookmarkAndDateSpace();
}