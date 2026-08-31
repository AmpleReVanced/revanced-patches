package com.samsung.android.content.clipboard.data;

import android.content.ClipData;

import java.io.Serializable;

public abstract class SemClipData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long timestamp = System.currentTimeMillis();
    private boolean protectedValue;

    public abstract ClipData getClipData();

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isProtected() {
        return protectedValue;
    }

    public void setProtected(boolean value) {
        protectedValue = value;
    }

    public void toLoad() {
    }

    public void toSave() {
    }
}