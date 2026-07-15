package app.revanced.extension.kakaotalk.chatlog.readreceipt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.settings.preference.MorphePreferenceStyle;
import app.morphe.extension.shared.settings.preference.SettingsActivityLayout;
import app.revanced.extension.kakaotalk.chatlog.readreceipt.MessageReadReceiptsExtension.Page;
import app.revanced.extension.kakaotalk.chatlog.readreceipt.MessageReadReceiptsExtension.Person;
import app.revanced.extension.kakaotalk.chatlog.readreceipt.MessageReadReceiptsExtension.Request;
import app.revanced.extension.kakaotalk.chatlog.readreceipt.MessageReadReceiptsExtension.Snapshot;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;
import app.revanced.extension.kakaotalk.settings.MorpheSettingsIconDynamicDrawable;

public final class MessageReadReceiptsActivity extends Activity {
    private static final String EXTRA_CHAT_ID =
            "app.revanced.extension.kakaotalk.readreceipt.CHAT_ID";
    private static final String EXTRA_MESSAGE_ID =
            "app.revanced.extension.kakaotalk.readreceipt.MESSAGE_ID";
    private static final String EXTRA_SENDER_ID =
            "app.revanced.extension.kakaotalk.readreceipt.SENDER_ID";
    private static final String EXTRA_CHAT_LOG_CLASS =
            "app.revanced.extension.kakaotalk.readreceipt.CHAT_LOG_CLASS";
    private static final long PROFILE_RETRY_DELAY_MS = 1200L;
    private static final int MAX_PROFILE_RETRIES = 5;

    /**
     * Resolving a profile makes KakaoTalk queue a member info fetch for it, so pages stay small to
     * avoid asking for a whole roster at once.
     */
    private static final int PROFILE_PAGE_SIZE = 50;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TabState readTab = new TabState(true);
    private final TabState unreadTab = new TabState(false);
    private TabState selectedTab = readTab;

    private boolean darkMode;
    private boolean loadInProgress;
    private boolean profileLoadInProgress;
    private boolean profileRetryPending;
    private int loadGeneration;
    private Request request;
    private Snapshot snapshot;

    private final ExecutorService loader = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Morphe receipt profiles");
        thread.setDaemon(true);
        return thread;
    });
    private final PeopleAdapter adapter = new PeopleAdapter();
    private final Runnable profileRetry = () -> {
        profileRetryPending = false;
        retryUnresolvedProfiles();
    };

    private TextView readCountView;
    private TextView unreadCountView;
    private TextView unknownCountView;
    private TextView coverageView;
    private TextView noticeView;
    private TextView readTabView;
    private TextView unreadTabView;
    private ListView listView;
    private ProgressBar progressView;
    private TextView stateView;

    static void start(Context context, Request request) {
        Intent intent = new Intent(context, MessageReadReceiptsActivity.class);
        putRequest(intent, request);
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setContext(getApplicationContext());
        MorphePreferenceStyle.setThemeModeProvider(
                MorpheSettingsIconDynamicDrawable::isAppDarkMode
        );
        SettingsActivityLayout.applyTheme(this);
        super.onCreate(savedInstanceState);

        darkMode = MorphePreferenceStyle.isDark(this);
        request = restoreRequest(savedInstanceState);

        int containerId = SettingsActivityLayout.setContentView(
                this,
                res("morphe_kakaotalk_chatlog_read_receipts_title", "Read receipts")
        );
        FrameLayout container = findViewById(containerId);
        container.addView(createContent(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        if (request == null) {
            showError();
        } else {
            reload();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Request replacement = restoreRequest(null);

        mainHandler.removeCallbacks(profileRetry);
        loadGeneration++;
        loadInProgress = false;
        profileLoadInProgress = false;
        profileRetryPending = false;
        selectedTab = readTab;
        request = replacement;
        resetSnapshot();

        if (request == null) {
            showError();
        } else {
            reload();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (request != null) putRequest(outState, request);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MorphePreferenceStyle.isDark(this) != darkMode) recreate();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        loader.shutdownNow();
        super.onDestroy();
    }

    private View createContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), 0);

        content.addView(createSummaryCard(), matchWrap());

        noticeView = text("", 13, noticeTextColor(), Typeface.NORMAL);
        noticeView.setLineSpacing(0f, 1.08f);
        noticeView.setPadding(dp(13), dp(10), dp(13), dp(10));
        noticeView.setBackground(rounded(noticeBackgroundColor(), 12));
        noticeView.setVisibility(View.GONE);
        LinearLayout.LayoutParams noticeParams = matchWrap();
        noticeParams.topMargin = dp(10);
        content.addView(noticeView, noticeParams);

        content.addView(createTabs(), tabsLayoutParams());
        content.addView(createListArea(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return content;
    }

    private View createSummaryCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));
        card.setBackground(rounded(surfaceColor(), 16));

        LinearLayout counts = new LinearLayout(this);
        counts.setOrientation(LinearLayout.HORIZONTAL);
        readCountView = addMetric(
                counts,
                res("morphe_kakaotalk_chatlog_read_receipts_read", "Read"),
                readColor()
        );
        unreadCountView = addMetric(
                counts,
                res("morphe_kakaotalk_chatlog_read_receipts_unread", "Unread"),
                unreadColor()
        );
        unknownCountView = addMetric(
                counts,
                res("morphe_kakaotalk_chatlog_read_receipts_unknown", "Unknown"),
                secondaryTextColor()
        );
        card.addView(counts, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(dividerColor());
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.topMargin = dp(12);
        dividerParams.bottomMargin = dp(10);
        card.addView(divider, dividerParams);

        LinearLayout coverageRow = new LinearLayout(this);
        coverageRow.setOrientation(LinearLayout.HORIZONTAL);
        coverageRow.setGravity(Gravity.CENTER_VERTICAL);

        coverageView = text("", 13, primaryTextColor(), Typeface.BOLD);
        coverageRow.addView(coverageView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView refresh = text(
                res("morphe_kakaotalk_chatlog_read_receipts_refresh", "Refresh"),
                13,
                readColor(),
                Typeface.BOLD
        );
        refresh.setGravity(Gravity.CENTER);
        refresh.setPadding(dp(12), dp(7), dp(12), dp(7));
        refresh.setBackground(rounded(segmentBackgroundColor(), 9));
        refresh.setOnClickListener(view -> reload());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        refreshParams.leftMargin = dp(10);
        coverageRow.addView(refresh, refreshParams);
        card.addView(coverageRow, matchWrap());

        TextView sourceView = text(
                res(
                        "morphe_kakaotalk_chatlog_read_receipts_source",
                        "Based on KakaoTalk's current member and watermark state."
                ),
                12,
                secondaryTextColor(),
                Typeface.NORMAL
        );
        sourceView.setPadding(0, dp(5), 0, 0);
        sourceView.setLineSpacing(0f, 1.05f);
        card.addView(sourceView, matchWrap());
        return card;
    }

    private TextView addMetric(LinearLayout parent, String label, int accentColor) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setGravity(Gravity.CENTER);

        TextView count = text("–", 24, accentColor, Typeface.BOLD);
        count.setGravity(Gravity.CENTER);
        metric.addView(count, matchWrap());

        TextView caption = text(label, 12, secondaryTextColor(), Typeface.NORMAL);
        caption.setGravity(Gravity.CENTER);
        metric.addView(caption, matchWrap());
        parent.addView(metric, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return count;
    }

    private View createTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabs.setBackground(rounded(segmentBackgroundColor(), 12));

        readTabView = createTab(readTab);
        unreadTabView = createTab(unreadTab);
        tabs.addView(readTabView, new LinearLayout.LayoutParams(0, dp(40), 1f));
        tabs.addView(unreadTabView, new LinearLayout.LayoutParams(0, dp(40), 1f));
        updateTabs();
        return tabs;
    }

    private TextView createTab(TabState tab) {
        TextView view = text("", 14, primaryTextColor(), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setOnClickListener(clicked -> {
            if (selectedTab == tab) return;
            selectedTab = tab;
            mainHandler.removeCallbacks(profileRetry);
            profileRetryPending = false;
            updateTabs();
            showSelectedList();
            if (tab.people.isEmpty()) loadNextPage();
            scheduleProfileRetryIfNeeded();
        });
        return view;
    }

    private View createListArea() {
        FrameLayout frame = new FrameLayout(this);

        listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setClipToPadding(false);
        listView.setPadding(0, dp(5), 0, dp(20));
        listView.setBackgroundColor(Color.TRANSPARENT);
        frame.addView(listView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        progressView = new ProgressBar(this);
        progressView.getIndeterminateDrawable().setTint(readColor());
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(36), dp(36));
        progressParams.gravity = Gravity.CENTER;
        frame.addView(progressView, progressParams);

        stateView = text("", 14, secondaryTextColor(), Typeface.NORMAL);
        stateView.setGravity(Gravity.CENTER);
        stateView.setPadding(dp(24), dp(24), dp(24), dp(24));
        stateView.setVisibility(View.GONE);
        frame.addView(stateView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return frame;
    }

    private void reload() {
        if (loader.isShutdown()) return;
        if (request == null) {
            request = restoreRequest(null);
            if (request == null) {
                showError();
                return;
            }
        }
        if (loadInProgress) return;

        mainHandler.removeCallbacks(profileRetry);
        profileRetryPending = false;
        profileLoadInProgress = false;
        loadInProgress = true;
        final int generation = ++loadGeneration;
        final Request currentRequest = request;
        snapshot = null;
        resetSnapshotViews();
        progressView.setVisibility(View.VISIBLE);
        stateView.setVisibility(View.GONE);
        listView.setVisibility(View.INVISIBLE);

        loader.execute(() -> {
            Snapshot loaded = MessageReadReceiptsExtension.loadSnapshot(currentRequest);
            mainHandler.post(() -> {
                if (isFinishing() || generation != loadGeneration) return;
                applySnapshot(loaded);
            });
        });
    }

    private void loadNextPage() {
        Snapshot currentSnapshot = snapshot;
        if (loader.isShutdown() || request == null || currentSnapshot == null ||
                currentSnapshot.failed || loadInProgress || profileLoadInProgress) {
            return;
        }

        final TabState tab = selectedTab;
        if (tab.loading) return;

        final int offset = tab.people.size();
        if (offset >= tab.total(currentSnapshot)) return;

        tab.loading = true;
        tab.error = false;
        final int generation = loadGeneration;
        final Request currentRequest = request;
        showSelectedList();

        loader.execute(() -> {
            Page page = MessageReadReceiptsExtension.loadPage(
                    currentRequest,
                    currentSnapshot,
                    tab.readers,
                    offset,
                    PROFILE_PAGE_SIZE
            );
            mainHandler.post(() -> {
                if (isFinishing() || generation != loadGeneration ||
                        currentSnapshot != snapshot) {
                    return;
                }

                tab.loading = false;
                if (page.failed) {
                    tab.error = true;
                } else if (tab.people.size() == offset) {
                    tab.people.addAll(page.people);
                    tab.retryCount = 0;
                }

                if (tab == selectedTab) showSelectedList();
                scheduleProfileRetryIfNeeded();
            });
        });
    }

    private void retryUnresolvedProfiles() {
        Request currentRequest = request;
        final TabState tab = selectedTab;
        if (loader.isShutdown() || currentRequest == null || snapshot == null ||
                snapshot.failed || loadInProgress || tab.loading || profileLoadInProgress ||
                !MessageReadReceiptsExtension.hasUnresolvedProfiles(tab.people)) {
            return;
        }

        profileLoadInProgress = true;
        final int generation = loadGeneration;
        final List<Person> captured = new ArrayList<>(tab.people);
        loader.execute(() -> {
            List<Person> refreshed =
                    MessageReadReceiptsExtension.retryUnresolvedProfiles(currentRequest, captured);
            mainHandler.post(() -> {
                if (isFinishing() || generation != loadGeneration) return;
                profileLoadInProgress = false;

                if (tab.people.size() == captured.size() && refreshed != captured) {
                    tab.people.clear();
                    tab.people.addAll(refreshed);
                }
                if (tab == selectedTab) {
                    showSelectedList();
                } else if (selectedTab.people.isEmpty()) {
                    loadNextPage();
                    return;
                }
                scheduleProfileRetryIfNeeded();
            });
        });
    }

    private void applySnapshot(Snapshot loaded) {
        snapshot = loaded;
        loadInProgress = false;
        progressView.setVisibility(View.GONE);

        if (loaded.failed) {
            showError();
            return;
        }

        readTab.reset();
        unreadTab.reset();
        profileLoadInProgress = false;

        readCountView.setText(formatCount(loaded.readerIds.size()));
        unreadCountView.setText(formatCount(loaded.nonReaderIds.size()));
        unknownCountView.setText(formatCount(loaded.unknownCount));
        coverageView.setText(format(
                "morphe_kakaotalk_chatlog_read_receipts_coverage",
                "%1$d of %2$d current members can be classified",
                loaded.knownCount,
                loaded.audienceCount
        ));

        updateNotice(loaded);
        updateTabs();
        showSelectedList();
        loadNextPage();
    }

    private void scheduleProfileRetryIfNeeded() {
        TabState tab = selectedTab;
        if (!profileRetryPending && !loadInProgress && !tab.loading && !profileLoadInProgress &&
                tab.retryCount < MAX_PROFILE_RETRIES &&
                MessageReadReceiptsExtension.hasUnresolvedProfiles(tab.people)) {
            profileRetryPending = true;
            tab.retryCount++;
            mainHandler.postDelayed(profileRetry, PROFILE_RETRY_DELAY_MS);
        }
    }

    private void updateNotice(Snapshot loaded) {
        String message = loaded.unknownCount > 0
                ? format(
                        "morphe_kakaotalk_chatlog_read_receipts_partial",
                        "Read state is unavailable for %1$d of the members KakaoTalk counts " +
                                "in this chat.",
                        loaded.unknownCount
                )
                : null;

        noticeView.setText(message == null ? "" : message);
        noticeView.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private void updateTabs() {
        int readCount = snapshot == null ? 0 : snapshot.readerIds.size();
        int unreadCount = snapshot == null ? 0 : snapshot.nonReaderIds.size();
        readTabView.setText(format(
                "morphe_kakaotalk_chatlog_read_receipts_tab_read",
                "Read %1$d",
                readCount
        ));
        unreadTabView.setText(format(
                "morphe_kakaotalk_chatlog_read_receipts_tab_unread",
                "Unread %1$d",
                unreadCount
        ));
        styleTab(readTabView, selectedTab == readTab);
        styleTab(unreadTabView, selectedTab == unreadTab);
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? primaryTextColor() : secondaryTextColor());
        tab.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        tab.setBackground(selected ? rounded(selectedSegmentColor(), 9) : null);
    }

    private void showSelectedList() {
        if (snapshot == null || snapshot.failed) return;

        TabState tab = selectedTab;
        int total = tab.total(snapshot);
        adapter.setState(tab.people, total, tab.loading, tab.error);
        progressView.setVisibility(
                tab.loading && tab.people.isEmpty() ? View.VISIBLE : View.GONE
        );
        if (total == 0) {
            showEmpty(res(
                    tab.readers
                            ? "morphe_kakaotalk_chatlog_read_receipts_empty_read"
                            : "morphe_kakaotalk_chatlog_read_receipts_empty_unread",
                    tab.readers ? "No confirmed readers" : "No confirmed unread members"
            ));
        } else {
            stateView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String message) {
        stateView.setText(message);
        stateView.setOnClickListener(null);
        stateView.setClickable(false);
        stateView.setVisibility(View.VISIBLE);
        listView.setVisibility(View.INVISIBLE);
    }

    private void showError() {
        resetSnapshotViews();
        progressView.setVisibility(View.GONE);
        listView.setVisibility(View.INVISIBLE);
        stateView.setText(res(
                "morphe_kakaotalk_chatlog_read_receipts_error",
                "Could not load read receipts. Tap to retry."
        ));
        stateView.setOnClickListener(view -> reload());
        stateView.setVisibility(View.VISIBLE);
    }

    private void resetSnapshot() {
        snapshot = null;
        resetSnapshotViews();
    }

    private void resetSnapshotViews() {
        readTab.reset();
        unreadTab.reset();
        profileLoadInProgress = false;
        readCountView.setText("–");
        unreadCountView.setText("–");
        unknownCountView.setText("–");
        coverageView.setText("");
        noticeView.setText("");
        noticeView.setVisibility(View.GONE);
        adapter.setState(Collections.emptyList(), 0, false, false);
        updateTabs();
    }

    private Request restoreRequest(Bundle savedInstanceState) {
        Bundle state = savedInstanceState;
        if (state == null || !state.containsKey(EXTRA_CHAT_ID)) {
            state = getIntent().getExtras();
        }
        if (state == null) return null;

        return MessageReadReceiptsExtension.restoreRequest(
                state.getLong(EXTRA_CHAT_ID, 0L),
                state.getLong(EXTRA_MESSAGE_ID, 0L),
                state.getLong(EXTRA_SENDER_ID, 0L),
                state.getString(EXTRA_CHAT_LOG_CLASS)
        );
    }

    private static void putRequest(
            Intent intent,
            Request request
    ) {
        intent.putExtra(EXTRA_CHAT_ID, request.chatId);
        intent.putExtra(EXTRA_MESSAGE_ID, request.messageId);
        intent.putExtra(EXTRA_SENDER_ID, request.senderId);
        intent.putExtra(EXTRA_CHAT_LOG_CLASS, request.chatLogClassName);
    }

    private static void putRequest(
            Bundle state,
            Request request
    ) {
        state.putLong(EXTRA_CHAT_ID, request.chatId);
        state.putLong(EXTRA_MESSAGE_ID, request.messageId);
        state.putLong(EXTRA_SENDER_ID, request.senderId);
        state.putString(EXTRA_CHAT_LOG_CLASS, request.chatLogClassName);
    }

    private TextView text(String value, int sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
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

    private LinearLayout.LayoutParams tabsLayoutParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(12);
        params.bottomMargin = dp(4);
        return params;
    }

    private String res(String name, String fallback) {
        int id = ResourceHelper.getResourceId("string", name);
        return id == 0 ? fallback : getString(id);
    }

    private String format(String name, String fallback, Object... arguments) {
        int id = ResourceHelper.getResourceId("string", name);
        return id == 0
                ? String.format(Locale.getDefault(), fallback, arguments)
                : getString(id, arguments);
    }

    private String formatCount(int count) {
        return String.format(Locale.getDefault(), "%,d", count);
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

    private int surfaceColor() {
        return MorphePreferenceStyle.pressedBackgroundColor(this);
    }

    private int segmentBackgroundColor() {
        return MorphePreferenceStyle.pressedBackgroundColor(this);
    }

    private int selectedSegmentColor() {
        return darkMode ? 0xFF3A3A3E : Color.WHITE;
    }

    private int dividerColor() {
        return darkMode ? 0xFF303034 : 0xFFECECEF;
    }

    private int noticeBackgroundColor() {
        return darkMode ? 0xFF342F17 : 0xFFFFF7CC;
    }

    private int noticeTextColor() {
        return darkMode ? 0xFFF0DD83 : 0xFF665810;
    }

    private int readColor() {
        return darkMode ? 0xFFFFE34F : 0xFFD2B900;
    }

    private int unreadColor() {
        return darkMode ? 0xFF78A5FF : 0xFF4777D8;
    }

    private static final class TabState {
        final boolean readers;
        final List<Person> people = new ArrayList<>();
        boolean loading;
        boolean error;
        int retryCount;

        TabState(boolean readers) {
            this.readers = readers;
        }

        void reset() {
            people.clear();
            loading = false;
            error = false;
            retryCount = 0;
        }

        int total(Snapshot snapshot) {
            if (snapshot == null || snapshot.failed) return 0;
            return readers ? snapshot.readerIds.size() : snapshot.nonReaderIds.size();
        }
    }

    private final class PeopleAdapter extends BaseAdapter {
        private List<Person> people = Collections.emptyList();
        private int total;
        private boolean loading;
        private boolean pageError;

        void setState(
                List<Person> value,
                int totalCount,
                boolean loadingPage,
                boolean failed
        ) {
            people = value;
            total = Math.max(value.size(), totalCount);
            loading = loadingPage;
            pageError = failed;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return people.size() + (people.size() < total ? 1 : 0);
        }

        @Override
        public Person getItem(int position) {
            return position < people.size() ? people.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            Person person = getItem(position);
            return person == null ? -1L : person.userId;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position < people.size() ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == 1) {
                return bindLoadMoreRow(convertView);
            }

            RowHolder holder;
            if (convertView == null) {
                convertView = createRow();
                holder = (RowHolder) convertView.getTag();
            } else {
                holder = (RowHolder) convertView.getTag();
            }

            Person person = getItem(position);
            if (person == null) return convertView;
            holder.name.setText(person.name);
            String state = res(
                    person.read
                            ? "morphe_kakaotalk_chatlog_read_receipts_read"
                            : "morphe_kakaotalk_chatlog_read_receipts_unread",
                    person.read ? "Read" : "Unread"
            );
            holder.state.setText(state);
            holder.state.setTextColor(person.read ? secondaryTextColor() : unreadColor());
            holder.indicator.setText(person.read ? "✓" : "○");
            holder.indicator.setTextColor(person.read ? readColor() : unreadColor());
            holder.fallback.setText(initial(person.name));

            boolean profileLoaded = holder.profileView != null &&
                    ProfileViewBridge.load(
                            holder.profileView,
                            person.userId,
                            person.imageUrl,
                            person.imageType
                    );
            holder.profileViewVisibility(profileLoaded);
            convertView.setContentDescription(person.name + ", " + state);
            return convertView;
        }

        private View bindLoadMoreRow(View convertView) {
            TextView loadMore;
            if (convertView instanceof TextView) {
                loadMore = (TextView) convertView;
            } else {
                loadMore = text("", 14, primaryTextColor(), Typeface.BOLD);
                loadMore.setGravity(Gravity.CENTER);
                loadMore.setMinHeight(dp(56));
                loadMore.setPadding(dp(12), dp(10), dp(12), dp(10));
            }

            if (loading) {
                loadMore.setText(res(
                        "morphe_kakaotalk_chatlog_read_receipts_loading_more",
                        "Loading…"
                ));
                loadMore.setTextColor(secondaryTextColor());
                loadMore.setOnClickListener(null);
            } else if (pageError) {
                loadMore.setText(res(
                        "morphe_kakaotalk_chatlog_read_receipts_load_more_error",
                        "Could not load more. Tap to retry."
                ));
                loadMore.setTextColor(unreadColor());
                loadMore.setOnClickListener(view -> loadNextPage());
            } else {
                loadMore.setText(format(
                        "morphe_kakaotalk_chatlog_read_receipts_load_more",
                        "Load more (%1$d of %2$d shown)",
                        people.size(),
                        total
                ));
                loadMore.setTextColor(readColor());
                loadMore.setOnClickListener(view -> loadNextPage());
            }
            return loadMore;
        }

        private View createRow() {
            LinearLayout row = new LinearLayout(MessageReadReceiptsActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            row.setMinimumHeight(dp(68));

            FrameLayout avatar = new FrameLayout(MessageReadReceiptsActivity.this);
            TextView fallback = text("", 17, primaryTextColor(), Typeface.BOLD);
            fallback.setGravity(Gravity.CENTER);
            fallback.setBackground(circle(darkMode ? 0xFF45454A : 0xFFE1E1E5));
            avatar.addView(fallback, new FrameLayout.LayoutParams(dp(46), dp(46)));

            View profileView = ProfileViewBridge.create(MessageReadReceiptsActivity.this);
            if (profileView != null) {
                profileView.setVisibility(View.GONE);
                avatar.addView(profileView, new FrameLayout.LayoutParams(dp(46), dp(46)));
            }
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(46), dp(46));
            avatarParams.rightMargin = dp(12);
            row.addView(avatar, avatarParams);

            LinearLayout labels = new LinearLayout(MessageReadReceiptsActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text("", 15, primaryTextColor(), Typeface.BOLD);
            name.setSingleLine(true);
            labels.addView(name, matchWrap());
            TextView state = text("", 12, secondaryTextColor(), Typeface.NORMAL);
            state.setPadding(0, dp(2), 0, 0);
            labels.addView(state, matchWrap());
            row.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView indicator = text("", 21, readColor(), Typeface.BOLD);
            indicator.setGravity(Gravity.CENTER);
            row.addView(indicator, new LinearLayout.LayoutParams(dp(36), dp(46)));

            row.setTag(new RowHolder(profileView, fallback, name, state, indicator));
            return row;
        }
    }

    private static final class RowHolder {
        final View profileView;
        final TextView fallback;
        final TextView name;
        final TextView state;
        final TextView indicator;

        RowHolder(
                View profileView,
                TextView fallback,
                TextView name,
                TextView state,
                TextView indicator
        ) {
            this.profileView = profileView;
            this.fallback = fallback;
            this.name = name;
            this.state = state;
            this.indicator = indicator;
        }

        void profileViewVisibility(boolean visible) {
            fallback.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (profileView != null) profileView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private static final class ProfileViewBridge {
        private static Constructor<?> constructor;
        private static Method loadMethod;
        private static boolean resolved;

        private ProfileViewBridge() {
        }

        static View create(Context context) {
            resolve();
            if (constructor == null) return null;
            try {
                Object instance = constructor.newInstance(context);
                return instance instanceof View ? (View) instance : null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        static boolean load(View profileView, long userId, String imageUrl, int imageType) {
            resolve();
            if (loadMethod == null || profileView == null || userId <= 0L) return false;
            try {
                loadMethod.invoke(profileView, userId, imageUrl, imageType);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static synchronized void resolve() {
            if (resolved) return;
            resolved = true;
            try {
                Class<?> type = Class.forName("com.kakao.talk.widget.ProfileView");
                constructor = type.getConstructor(Context.class);
                loadMethod = type.getMethod("load", long.class, String.class, int.class);
            } catch (Throwable ignored) {
                constructor = null;
                loadMethod = null;
            }
        }
    }

    private String initial(String value) {
        if (value == null || value.trim().isEmpty()) return "?";
        String trimmed = value.trim();
        int end = trimmed.offsetByCodePoints(0, 1);
        return trimmed.substring(0, end).toUpperCase(Locale.getDefault());
    }
}
