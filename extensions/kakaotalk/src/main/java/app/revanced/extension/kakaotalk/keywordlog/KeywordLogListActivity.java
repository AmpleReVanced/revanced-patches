package app.revanced.extension.kakaotalk.keywordlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.MorphePreferenceStyle;
import app.morphe.extension.shared.settings.preference.SettingsActivityLayout;
import app.revanced.extension.kakaotalk.helper.KakaoProfileViewBridge;
import app.revanced.extension.kakaotalk.settings.MorpheSettingsIconDynamicDrawable;

public final class KeywordLogListActivity extends Activity {
    private static final int PAGE_SIZE = 50;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;

    private final List<Object> rows = new ArrayList<>();
    private final RowAdapter adapter = new RowAdapter();

    private boolean darkMode;
    private int sort = KeywordLogStore.SORT_TIME;
    private int loadedCount;
    private int loadGeneration;
    private boolean exhausted;
    private boolean loading;

    private ListView listView;
    private TextView emptyView;
    private TextView timeTab;
    private TextView chatRoomTab;
    private TextView senderTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());
        MorphePreferenceStyle.setThemeModeProvider(
                MorpheSettingsIconDynamicDrawable::isAppDarkMode
        );
        SettingsActivityLayout.applyTheme(this);
        super.onCreate(savedInstanceState);

        darkMode = MorphePreferenceStyle.isDark(this);
        sort = preferences().getInt("sort", KeywordLogStore.SORT_TIME);

        int containerId = SettingsActivityLayout.setContentView(
                this,
                KeywordLogPatch.string("title_for_keyword_log_list")
        );
        FrameLayout container = findViewById(containerId);
        container.addView(createContent(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        loadNextPage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MorphePreferenceStyle.isDark(this) != darkMode) recreate();
    }

    private View createContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), 0);

        content.addView(createSortTabs(), matchWrap());
        content.addView(createDeleteAllAction(), deleteAllLayoutParams());
        content.addView(createListArea(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        return content;
    }

    private View createSortTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabs.setBackground(rounded(MorphePreferenceStyle.pressedBackgroundColor(this), 12));

        timeTab = createSortTab(
                KeywordLogPatch.string("morphe_kakaotalk_keyword_log_sort_latest"),
                KeywordLogStore.SORT_TIME
        );
        chatRoomTab = createSortTab(
                KeywordLogPatch.string("label_for_keyword_log_chatroom_first"),
                KeywordLogStore.SORT_CHAT_ROOM
        );
        senderTab = createSortTab(
                KeywordLogPatch.string("label_for_keyword_log_friend_first"),
                KeywordLogStore.SORT_SENDER
        );

        tabs.addView(timeTab, new LinearLayout.LayoutParams(0, dp(40), 1f));
        tabs.addView(chatRoomTab, new LinearLayout.LayoutParams(0, dp(40), 1f));
        tabs.addView(senderTab, new LinearLayout.LayoutParams(0, dp(40), 1f));
        updateSortTabs();
        return tabs;
    }

    private TextView createSortTab(String label, int value) {
        TextView view = text(label, 13, primaryTextColor(), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setOnClickListener(clicked -> applySort(value));
        return view;
    }

    private void updateSortTabs() {
        styleSortTab(timeTab, sort == KeywordLogStore.SORT_TIME);
        styleSortTab(chatRoomTab, sort == KeywordLogStore.SORT_CHAT_ROOM);
        styleSortTab(senderTab, sort == KeywordLogStore.SORT_SENDER);
    }

    private void styleSortTab(TextView view, boolean selected) {
        view.setBackground(selected
                ? rounded(darkMode ? 0xFF3A3A3E : 0xFFFFFFFF, 10)
                : null);
        view.setTextColor(selected ? primaryTextColor() : secondaryTextColor());
    }

    private View createDeleteAllAction() {
        TextView action = text(
                KeywordLogPatch.string("desc_for_keyword_log_remove_all"),
                12,
                secondaryTextColor(),
                Typeface.NORMAL
        );
        action.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        action.setSingleLine(true);
        action.setPadding(dp(8), dp(6), dp(4), dp(2));
        action.setOnClickListener(clicked -> confirmDeleteAll());
        return action;
    }

    private LinearLayout.LayoutParams deleteAllLayoutParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(2);
        return params;
    }

    private View createListArea() {
        FrameLayout content = new FrameLayout(this);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setPadding(0, dp(4), 0, dp(16));
        listView.setClipToPadding(false);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object row = rows.get(position);
                if (row instanceof KeywordLogEntry) openChatRoom((KeywordLogEntry) row);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object row = rows.get(position);
                if (!(row instanceof KeywordLogEntry)) return false;

                showEntryMenu((KeywordLogEntry) row);
                return true;
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= total - 1) loadNextPage();
            }
        });
        content.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        emptyView = text(
                KeywordLogPatch.string("desc_for_keyword_log_list_empty"),
                14,
                secondaryTextColor(),
                Typeface.NORMAL
        );
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        content.addView(emptyView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        return content;
    }

    private void applySort(int newSort) {
        if (sort == newSort) return;

        sort = newSort;
        preferences().edit().putInt("sort", newSort).apply();
        updateSortTabs();
        reload();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setMessage(KeywordLogPatch.string("desc_for_keyword_log_remove_all"))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> Utils.runOnBackgroundThread(() -> {
                    KeywordLogStore.getInstance().deleteUnprotected();
                    Utils.runOnMainThread(this::reload);
                }))
                .show();
    }

    private void showEntryMenu(KeywordLogEntry entry) {
        String protectLabel = entry.isProtected
                ? KeywordLogPatch.string("label_for_keyword_log_no_protection")
                : KeywordLogPatch.string("label_for_keyword_log_protection");

        new AlertDialog.Builder(this)
                .setItems(
                        new CharSequence[]{
                                protectLabel,
                                KeywordLogPatch.string("desc_for_keyword_log_remove"),
                        },
                        (dialog, which) -> {
                            if (which == 0) {
                                Utils.runOnBackgroundThread(() -> {
                                    KeywordLogStore.getInstance()
                                            .setProtected(entry.id, !entry.isProtected);
                                    Utils.runOnMainThread(this::reload);
                                });
                            } else {
                                confirmDelete(entry);
                            }
                        }
                )
                .show();
    }

    private void confirmDelete(KeywordLogEntry entry) {
        new AlertDialog.Builder(this)
                .setMessage(KeywordLogPatch.string("desc_for_keyword_log_remove"))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> Utils.runOnBackgroundThread(() -> {
                    KeywordLogStore.getInstance().delete(Collections.singletonList(entry.id));
                    Utils.runOnMainThread(this::reload);
                }))
                .show();
    }

    private void openChatRoom(KeywordLogEntry entry) {
        try {
            Intent intent = KeywordLogPatch.createChatRoomIntent(this, entry.chatRoomId);
            if (intent == null) {
                Utils.showToastShort(
                        KeywordLogPatch.string("label_for_keyword_log_chat_room_not_found")
                );
                return;
            }
            startActivity(intent);
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to open a chat room", ex);
            Utils.showToastShort(
                    KeywordLogPatch.string("label_for_keyword_log_chat_room_not_found")
            );
        }
    }

    private void reload() {
        rows.clear();
        loadedCount = 0;
        exhausted = false;
        loading = false;
        loadGeneration++;
        adapter.notifyDataSetChanged();
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || exhausted) return;
        loading = true;

        final int offset = loadedCount;
        final int requestedSort = sort;
        final int generation = loadGeneration;
        Utils.runOnBackgroundThread(() -> {
            List<KeywordLogEntry> page =
                    KeywordLogStore.getInstance().query(requestedSort, offset, PAGE_SIZE);
            Utils.runOnMainThread(() -> onPageLoaded(page, generation));
        });
    }

    private void onPageLoaded(List<KeywordLogEntry> page, int generation) {
        if (generation != loadGeneration) return;

        loading = false;

        if (page.size() < PAGE_SIZE) exhausted = true;
        if (!page.isEmpty()) {
            loadedCount += page.size();
            appendRows(page);
        }

        boolean empty = rows.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void appendRows(List<KeywordLogEntry> page) {
        String lastHeader = null;
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (rows.get(i) instanceof String) {
                lastHeader = (String) rows.get(i);
                break;
            }
        }

        for (KeywordLogEntry entry : page) {
            String header = headerFor(entry);
            if (header != null && !header.equals(lastHeader)) {
                rows.add(header);
                lastHeader = header;
            }
            rows.add(entry);
        }
    }

    private String headerFor(KeywordLogEntry entry) {
        switch (sort) {
            case KeywordLogStore.SORT_CHAT_ROOM:
                return chatRoomNameOf(entry);
            case KeywordLogStore.SORT_SENDER:
                return senderNameOf(entry);
            default:
                return dateHeaderOf(entry);
        }
    }

    private String dateHeaderOf(KeywordLogEntry entry) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(entry.createdAt);

        if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return KeywordLogPatch.string("title_for_keyword_log_today");
        }

        return new SimpleDateFormat("yyyy. M. d.", Locale.getDefault())
                .format(new Date(entry.createdAt));
    }

    private String chatRoomNameOf(KeywordLogEntry entry) {
        if (entry.chatRoomName.length() > 0) return entry.chatRoomName;
        return KeywordLogPatch.string("label_for_keyword_log_undefined_chat_room");
    }

    private String senderNameOf(KeywordLogEntry entry) {
        if (entry.sender.length() > 0) return entry.sender;
        return KeywordLogPatch.string("label_for_keyword_log_undefined_chat_room");
    }

    private final class RowAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            Object row = rows.get(position);
            return row instanceof KeywordLogEntry ? ((KeywordLogEntry) row).id : -position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ENTRY;
        }

        @Override
        public boolean isEnabled(int position) {
            return rows.get(position) instanceof KeywordLogEntry;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object row = rows.get(position);
            return row instanceof String
                    ? bindHeader(convertView, (String) row)
                    : bindEntry(convertView, (KeywordLogEntry) row);
        }

        private View bindHeader(View convertView, String title) {
            TextView header = convertView instanceof TextView
                    ? (TextView) convertView
                    : createHeader();
            header.setText(title);
            return header;
        }

        private TextView createHeader() {
            TextView header = text("", 12, secondaryTextColor(), Typeface.BOLD);
            header.setPadding(dp(20), dp(16), dp(20), dp(6));
            return header;
        }

        private View bindEntry(View convertView, KeywordLogEntry entry) {
            View row = convertView != null && convertView.getTag() instanceof RowHolder
                    ? convertView
                    : createRow();
            RowHolder holder = (RowHolder) row.getTag();

            String sender = senderNameOf(entry);
            holder.sender.setText(sender);
            holder.chatRoom.setText(chatRoomNameOf(entry));
            holder.time.setText(formatTime(entry.createdAt));
            holder.message.setText(highlight(entry.message));
            holder.lock.setVisibility(entry.isProtected ? View.VISIBLE : View.GONE);
            holder.fallback.setText(initial(sender));

            boolean loaded = KakaoProfileViewBridge.load(
                    holder.profileView,
                    entry.userId,
                    entry.profileUrl,
                    0
            );
            holder.showProfile(loaded);

            row.setContentDescription(sender + ", " + entry.message);
            return row;
        }

        private View createRow() {
            LinearLayout row = new LinearLayout(KeywordLogListActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            row.setMinimumHeight(dp(68));

            FrameLayout avatar = new FrameLayout(KeywordLogListActivity.this);
            TextView fallback = text("", 17, primaryTextColor(), Typeface.BOLD);
            fallback.setGravity(Gravity.CENTER);
            fallback.setBackground(circle(darkMode ? 0xFF45454A : 0xFFE1E1E5));
            avatar.addView(fallback, new FrameLayout.LayoutParams(dp(46), dp(46)));

            View profileView = KakaoProfileViewBridge.create(KeywordLogListActivity.this);
            if (profileView != null) {
                profileView.setVisibility(View.GONE);
                avatar.addView(profileView, new FrameLayout.LayoutParams(dp(46), dp(46)));
            }

            LinearLayout.LayoutParams avatarParams =
                    new LinearLayout.LayoutParams(dp(46), dp(46));
            avatarParams.rightMargin = dp(12);
            row.addView(avatar, avatarParams);

            LinearLayout labels = new LinearLayout(KeywordLogListActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);

            LinearLayout titleRow = new LinearLayout(KeywordLogListActivity.this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView sender = text("", 15, primaryTextColor(), Typeface.BOLD);
            sender.setSingleLine(true);
            titleRow.addView(sender, wrapWrap());

            TextView lock = text("🔒", 11, secondaryTextColor(), Typeface.NORMAL);
            LinearLayout.LayoutParams lockParams = wrapWrap();
            lockParams.leftMargin = dp(4);
            titleRow.addView(lock, lockParams);

            TextView time = text("", 11, secondaryTextColor(), Typeface.NORMAL);
            time.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams timeParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            timeParams.leftMargin = dp(8);
            titleRow.addView(time, timeParams);

            labels.addView(titleRow, matchWrap());

            TextView message = text("", 15, primaryTextColor(), Typeface.NORMAL);
            message.setMaxLines(2);
            LinearLayout.LayoutParams messageParams = matchWrap();
            messageParams.topMargin = dp(3);
            labels.addView(message, messageParams);

            TextView chatRoom = text("", 12, secondaryTextColor(), Typeface.NORMAL);
            chatRoom.setSingleLine(true);
            LinearLayout.LayoutParams chatRoomParams = matchWrap();
            chatRoomParams.topMargin = dp(3);
            labels.addView(chatRoom, chatRoomParams);

            row.addView(labels, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            row.setTag(new RowHolder(profileView, fallback, sender, lock, time, message, chatRoom));
            return row;
        }
    }

    private static final class RowHolder {
        final View profileView;
        final TextView fallback;
        final TextView sender;
        final TextView lock;
        final TextView time;
        final TextView message;
        final TextView chatRoom;

        RowHolder(
                View profileView,
                TextView fallback,
                TextView sender,
                TextView lock,
                TextView time,
                TextView message,
                TextView chatRoom
        ) {
            this.profileView = profileView;
            this.fallback = fallback;
            this.sender = sender;
            this.lock = lock;
            this.time = time;
            this.message = message;
            this.chatRoom = chatRoom;
        }

        void showProfile(boolean visible) {
            fallback.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (profileView != null) profileView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }


    private CharSequence highlight(String message) {
        try {
            return KeywordLogPatch.highlight(new SpannableStringBuilder(message));
        } catch (Throwable ex) {
            Logger.printException(() -> "Failed to highlight a keyword", ex);
            return message;
        }
    }

    private String initial(String value) {
        if (value == null || value.trim().isEmpty()) return "?";

        String trimmed = value.trim();
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1))
                .toUpperCase(Locale.getDefault());
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("a h:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private TextView text(String value, int sp, int color, int typefaceStyle) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, typefaceStyle);
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private SharedPreferences preferences() {
        return getSharedPreferences("morphe_keyword_log_ui", Context.MODE_PRIVATE);
    }

    private int dp(int value) {
        return MorphePreferenceStyle.dp(this, value);
    }

    private int primaryTextColor() {
        return MorphePreferenceStyle.primaryTextColor(this);
    }

    private int secondaryTextColor() {
        return MorphePreferenceStyle.secondaryTextColor(this);
    }
}