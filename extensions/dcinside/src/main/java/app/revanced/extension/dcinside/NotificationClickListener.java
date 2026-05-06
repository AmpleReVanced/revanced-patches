package app.revanced.extension.dcinside;

import android.content.Context;
import android.content.Intent;
import android.view.View;

public final class NotificationClickListener implements View.OnClickListener {
    private final Context context;

    public NotificationClickListener(Context context) {
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setClassName(context, "com.dcinside.app.push.PushNoticeListActivity");
        context.startActivity(intent);
    }
}
