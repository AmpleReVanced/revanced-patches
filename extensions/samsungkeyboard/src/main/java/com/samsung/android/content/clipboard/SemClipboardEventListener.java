package com.samsung.android.content.clipboard;

import com.samsung.android.content.clipboard.data.SemClipData;

public interface SemClipboardEventListener {
    void onClipboardUpdated(int type, SemClipData data);

    void onFilterUpdated(int filter);
}