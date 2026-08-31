package app.revanced.extension.kakaotalk.keywordlog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;

final class KeywordLogStore extends SQLiteOpenHelper {
    static final int SORT_TIME = 0;
    static final int SORT_CHAT_ROOM = 1;
    static final int SORT_SENDER = 2;

    private static final String DATABASE_NAME = "morphe_keyword_log.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE = "keyword_log";
    private static final int MAX_ROWS = 5000;

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_PROFILE_URL = "profile_url";
    private static final String COLUMN_CHAT_ROOM_NAME = "chat_room_name";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_PROTECTED = "protected";

    private static final String[] COLUMNS = {
            COLUMN_ID,
            COLUMN_CHAT_ID,
            COLUMN_USER_ID,
            COLUMN_SENDER,
            COLUMN_PROFILE_URL,
            COLUMN_CHAT_ROOM_NAME,
            COLUMN_MESSAGE,
            COLUMN_CREATED_AT,
            COLUMN_PROTECTED,
    };

    private static volatile KeywordLogStore instance;

    private KeywordLogStore(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    static KeywordLogStore getInstance() {
        KeywordLogStore local = instance;
        if (local != null) return local;

        synchronized (KeywordLogStore.class) {
            if (instance == null) {
                instance = new KeywordLogStore(Utils.getContext());
            }
            return instance;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE + " (\n"
                + COLUMN_ID + " INTEGER PRIMARY KEY NOT NULL,\n"
                + COLUMN_CHAT_ID + " INTEGER NOT NULL,\n"
                + COLUMN_USER_ID + " INTEGER NOT NULL,\n"
                + COLUMN_SENDER + " TEXT,\n"
                + COLUMN_PROFILE_URL + " TEXT,\n"
                + COLUMN_CHAT_ROOM_NAME + " TEXT,\n"
                + COLUMN_MESSAGE + " TEXT,\n"
                + COLUMN_CREATED_AT + " INTEGER NOT NULL,\n"
                + COLUMN_PROTECTED + " INTEGER NOT NULL DEFAULT 0\n"
                + ")");
        database.execSQL("CREATE INDEX idx_" + TABLE + "_created_at ON " + TABLE
                + " (" + COLUMN_CREATED_AT + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE " + TABLE
                    + " ADD COLUMN " + COLUMN_PROFILE_URL + " TEXT");
        }
    }

    void insert(
            long id,
            long chatRoomId,
            long userId,
            String sender,
            String profileUrl,
            String chatRoomName,
            String message,
            long createdAt
    ) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CHAT_ID, chatRoomId);
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_SENDER, sender);
            values.put(COLUMN_PROFILE_URL, profileUrl);
            values.put(COLUMN_CHAT_ROOM_NAME, chatRoomName);
            values.put(COLUMN_MESSAGE, message);
            values.put(COLUMN_CREATED_AT, createdAt);

            SQLiteDatabase database = getWritableDatabase();

            ContentValues inserted = new ContentValues(values);
            inserted.put(COLUMN_ID, id);
            long rowId = database.insertWithOnConflict(
                    TABLE, null, inserted, SQLiteDatabase.CONFLICT_IGNORE);

            // Updating instead of replacing keeps a row the user protected from losing that flag.
            if (rowId == -1L) {
                database.update(TABLE, values, COLUMN_ID + " = ?",
                        new String[]{String.valueOf(id)});
            }

            trim(database);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to record a keyword log", ex);
        }
    }

    private void trim(SQLiteDatabase database) {
        database.execSQL("DELETE FROM " + TABLE
                + " WHERE " + COLUMN_PROTECTED + " = 0"
                + " AND " + COLUMN_ID + " NOT IN ("
                + "SELECT " + COLUMN_ID + " FROM " + TABLE
                + " ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT " + MAX_ROWS + ")");
    }

    List<KeywordLogEntry> query(int sort, int offset, int limit) {
        List<KeywordLogEntry> entries = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                COLUMNS,
                null,
                null,
                null,
                null,
                orderBy(sort),
                offset + ", " + limit
        )) {
            while (cursor.moveToNext()) {
                entries.add(new KeywordLogEntry(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getLong(7),
                        cursor.getInt(8) != 0
                ));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to read keyword logs", ex);
        }

        return entries;
    }

    /**
     * Every ordering ends in a unique column so paging by offset cannot skip or repeat ties.
     */
    private static String orderBy(int sort) {
        switch (sort) {
            case SORT_CHAT_ROOM:
                return COLUMN_CHAT_ROOM_NAME + " COLLATE NOCASE ASC, "
                        + COLUMN_CREATED_AT + " DESC, " + COLUMN_ID + " DESC";
            case SORT_SENDER:
                return COLUMN_SENDER + " COLLATE NOCASE ASC, "
                        + COLUMN_CREATED_AT + " DESC, " + COLUMN_ID + " DESC";
            default:
                return COLUMN_CREATED_AT + " DESC, " + COLUMN_ID + " DESC";
        }
    }

    KeywordLogEntry latest() {
        List<KeywordLogEntry> entries = query(SORT_TIME, 0, 1);
        return entries.isEmpty() ? null : entries.get(0);
    }

    void setProtected(long id, boolean value) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PROTECTED, value ? 1 : 0);
            getWritableDatabase().update(TABLE, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)});
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to update keyword log protection", ex);
        }
    }

    void delete(Collection<Long> ids) {
        if (ids.isEmpty()) return;

        StringBuilder placeholders = new StringBuilder();
        String[] arguments = new String[ids.size()];
        int index = 0;
        for (Long id : ids) {
            if (index > 0) placeholders.append(',');
            placeholders.append('?');
            arguments[index++] = String.valueOf(id);
        }

        try {
            getWritableDatabase().delete(TABLE,
                    COLUMN_ID + " IN (" + placeholders + ")", arguments);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to delete keyword logs", ex);
        }
    }

    void deleteUnprotected() {
        try {
            getWritableDatabase().delete(TABLE, COLUMN_PROTECTED + " = 0", null);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to clear keyword logs", ex);
        }
    }
}