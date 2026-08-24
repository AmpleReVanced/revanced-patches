package com.samsung.android.content.clipboard.data;

import android.content.ClipData;
import android.text.Html;

public class SemHtmlClipData extends SemClipData {
    private static final long serialVersionUID = 1L;
    private String html = "";
    private String plainText = "";
    private String thumbnailImagePath = "";

    public String getHtml() {
        return html;
    }

    public String getPlainText() {
        return plainText;
    }

    public boolean setHtml(CharSequence value) {
        html = value == null ? "" : value.toString();
        plainText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        return true;
    }

    public boolean setThumbnailImagePath(String path) {
        thumbnailImagePath = path == null ? "" : path;
        return true;
    }

    @Override
    public ClipData getClipData() {
        return ClipData.newHtmlText(null, plainText, html);
    }
}