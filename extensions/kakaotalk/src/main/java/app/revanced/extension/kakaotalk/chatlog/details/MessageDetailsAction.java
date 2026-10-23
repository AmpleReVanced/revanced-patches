package app.revanced.extension.kakaotalk.chatlog.details;

import android.app.Activity;

import java.util.Collections;
import java.util.Set;

public final class MessageDetailsAction {
    private final MessageDetailsSource message;

    public MessageDetailsAction(Object message, Object chatRoom) {
        this.message = (MessageDetailsSource) message;
    }

    public String getLabel() {
        return MessageDetailsExtension.menuTitle();
    }

    public Set<?> getSupportedTypes() {
        return Collections.emptySet();
    }

    public void perform(Activity activity) {
        MessageDetailsExtension.open(activity, message);
    }
}