package com.samsung.android.content.clipboard.data;

import android.content.ClipData;
import android.net.Uri;

public class SemImageClipData extends SemClipData {
    private static final long serialVersionUID = 1L;
    private transient ClipData clipData;
    private String imagePath;

    public boolean setImagePath(String path) {
        imagePath = path;
        clipData = path == null ? null : ClipData.newRawUri(null, Uri.parse(path));
        return clipData != null;
    }

    public void setClipData(ClipData data) {
        clipData = data;
    }

    @Override
    public ClipData getClipData() {
        if (clipData == null && imagePath != null) {
            clipData = ClipData.newRawUri(null, Uri.parse(imagePath));
        }
        return clipData;
    }
}