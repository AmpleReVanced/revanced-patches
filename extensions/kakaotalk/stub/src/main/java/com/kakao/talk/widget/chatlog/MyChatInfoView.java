package com.kakao.talk.widget.chatlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;

/**
 * Stub implementation of MyChatInfoView.
 * This class keeps method/constructor signatures for compile-time only.
 */
public final class MyChatInfoView extends ChatInfoView {

    public MyChatInfoView(Context context) {
        super(context);
    }

    public MyChatInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyChatInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getTotalWidth() {
        return 0;
    }

    @Override
    public void makeRect() {

    }

    @Override
    public String getDate() {
        return "";
    }

    @Override
    public void setDate(String date) {

    }

    @Override
    public int getDateTextWidth(String date) {
        return 0;
    }

    @Override
    public int getDateTextColor() {
        return 0;
    }

    @Override
    public void setDateTextColor(int color) {

    }

    @Override
    public void hideDate(boolean hide) {

    }

    @Override
    public boolean isDateHidden() {
        return false;
    }

    @Override
    public void setDateHidden(boolean hidden) {

    }

    @Override
    public Layout getDateLayout() {
        return null;
    }

    @Override
    public void setDateLayout(Layout layout) {

    }

    @Override
    public Layout getUnreadLayout() {
        return null;
    }

    @Override
    public void setUnreadLayout(Layout layout) {

    }

    @Override
    public Layout getModifyLayout() {
        return null;
    }

    @Override
    public void setModifyLayout(Layout layout) {

    }

    @Override
    public Rect getDateRect() {
        return null;
    }

    @Override
    public void setDateRect(Rect rect) {

    }

    @Override
    public Rect getUnreadRect() {
        return null;
    }

    @Override
    public void setUnreadRect(Rect rect) {

    }

    @Override
    public Rect getBookmarkRect() {
        return null;
    }

    @Override
    public void setBookmarkRect(Rect rect) {

    }

    @Override
    public Rect getModifyRect() {
        return null;
    }

    @Override
    public void setModifyRect(Rect rect) {

    }

    @Override
    public Rect getChatThreadShareRect() {
        return null;
    }

    @Override
    public void setChatThreadShareRect(Rect rect) {

    }

    @Override
    public Bitmap getBookmarkIcon() {
        return null;
    }

    @Override
    public void setBookmarkIcon(Bitmap bitmap) {

    }

    @Override
    public Bitmap getChatThreadShareIcon() {
        return null;
    }

    @Override
    public void setChatThreadShareIcon(Bitmap bitmap) {

    }

    @Override
    public boolean isBookmarking() {
        return false;
    }

    @Override
    public void setBookmarking(boolean bookmarking) {

    }

    @Override
    public int getUnreadCount() {
        return 0;
    }

    @Override
    public void setUnreadCount(int count) {

    }

    @Override
    public int getUnreadTextColor() {
        return 0;
    }

    @Override
    public void setUnreadTextColor(int color) {

    }

    @Override
    public void enableUnreadShadow(boolean enable) {

    }

    @Override
    public boolean isModify() {
        return false;
    }

    @Override
    public void setModify(boolean modify) {

    }

    @Override
    public int getModifyTextColor() {
        return 0;
    }

    @Override
    public void setModifyTextColor(int color) {

    }

    @Override
    public boolean isSharedToChatRoomFromChatThread() {
        return false;
    }

    @Override
    public void setSharedToChatRoomFromChatThread(boolean shared) {

    }

    @Override
    public void setChatLogId(long chatLogId) {

    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isAnimating() {
        return false;
    }

    @Override
    public int getBookmarkAndDateSpace() {
        return 0;
    }
}
