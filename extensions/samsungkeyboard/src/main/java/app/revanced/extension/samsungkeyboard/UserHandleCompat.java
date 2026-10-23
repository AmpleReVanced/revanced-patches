package app.revanced.extension.samsungkeyboard;

import android.os.Binder;
import android.os.Process;
import android.os.UserHandle;

public final class UserHandleCompat {
    private UserHandleCompat() {
    }

    public static int semGetMyUserId() {
        return semGetUserId(Process.myUid());
    }

    public static int semGetCallingUserId() {
        return semGetUserId(Binder.getCallingUid());
    }

    public static int semGetUserId(int uid) {
        return uid / 100000;
    }

    public static int semGetIdentifier(UserHandle user) {
        return user.hashCode();
    }
}