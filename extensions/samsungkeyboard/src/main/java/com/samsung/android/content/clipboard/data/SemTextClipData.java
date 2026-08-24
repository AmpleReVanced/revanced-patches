package com.samsung.android.content.clipboard.data;

import android.content.ClipData;

public class SemTextClipData extends SemClipData {
    private static final long serialVersionUID = 1L;
    private String text = "";

    public CharSequence getText() {
        return text;
    }

    public boolean setText(CharSequence value) {
        text = value == null ? "" : value.toString();
        return true;
    }

    @Override
    public ClipData getClipData() {
        return ClipData.newPlainText(null, text);
    }
}