package app.revanced.extension.kakaotalk.chatlog.readreceipt;

import android.content.Context;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;
import app.revanced.extension.kakaotalk.settings.Settings;

public final class MessageReadReceiptsExtension {
    private static final String TAG = "MorpheReadReceipts";
    private static final String LOG_ID_METHOD = "revanced_read_receipt_log_id";
    private static final String CHAT_ID_METHOD = "revanced_read_receipt_chat_id";
    private static final String SENDER_ID_METHOD = "revanced_read_receipt_sender_id";
    private static final String MEMBER_SET_BY_CHAT_ID_METHOD =
            "revanced_read_receipt_member_set_by_chat_id";
    private static final String WATERMARKS_METHOD = "revanced_read_receipt_watermarks";
    private static final String ACTIVE_COUNT_METHOD = "revanced_read_receipt_active_count";
    private static final String CURRENT_USER_ID_METHOD = "revanced_read_receipt_current_user_id";
    private static final String PROFILE_METHOD = "revanced_read_receipt_profile";
    private static final String PROFILE_NAME_METHOD = "revanced_read_receipt_name";
    private static final String PROFILE_IMAGE_URL_METHOD = "revanced_read_receipt_image_url";
    private static final String PROFILE_IMAGE_TYPE_METHOD = "revanced_read_receipt_image_type";
    private static final String PROFILE_COMPLETE_METHOD = "revanced_read_receipt_profile_complete";

    /**
     * The image type KakaoTalk passes for members that do not use an open chat profile.
     */
    private static final int DEFAULT_IMAGE_TYPE = 0;

    private static final Map<Class<?>, ChatLogAccessors> CHAT_LOG_ACCESSORS =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, MemberSetAccessors> MEMBER_SET_ACCESSORS =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, ProfileAccessors> PROFILE_ACCESSORS =
            new ConcurrentHashMap<>();

    private MessageReadReceiptsExtension() {
    }

    /**
     * The injected bridge passes the synchronized backing map rather than the unmodifiable view, so
     * that this locks on the same monitor KakaoTalk holds when it updates a watermark.
     */
    public static Object snapshotWatermarks(Object value) {
        if (!(value instanceof Map)) return Collections.emptyMap();

        Map<?, ?> source = (Map<?, ?>) value;
        synchronized (source) {
            Map<Long, Long> result = new LinkedHashMap<>(source.size());
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (!(entry.getKey() instanceof Number) ||
                        !(entry.getValue() instanceof Number)) {
                    continue;
                }
                long userId = ((Number) entry.getKey()).longValue();
                long watermark = ((Number) entry.getValue()).longValue();
                if (userId > 0L && watermark >= 0L) result.put(userId, watermark);
            }
            return result;
        }
    }

    public static void bind(View view, Object chatLog) {
        if (view == null) return;

        if (chatLog == null || !Settings.showMessageReadReceipts()) {
            view.setOnClickListener(null);
            // Clearing the listener alone leaves the view clickable, swallowing the row's touches.
            view.setClickable(false);
            view.setContentDescription(null);
            view.setTooltipText(null);
            return;
        }

        String action = resourceString(
                "morphe_kakaotalk_chatlog_read_receipts_open",
                "Show read and unread members"
        );
        view.setContentDescription(action);
        view.setTooltipText(action);
        view.setOnClickListener(clickedView -> {
            clickedView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            try {
                Request request = createRequest(chatLog);
                MessageReadReceiptsActivity.start(clickedView.getContext(), request);
            } catch (Throwable throwable) {
                Log.e(TAG, "Failed to open read receipts", throwable);
            }
        });
    }

    static Request restoreRequest(
            long chatId,
            long messageId,
            long senderId,
            String chatLogClassName
    ) {
        Request request = new Request(
                chatId,
                messageId,
                senderId,
                chatLogClassName
        );
        return request.isValid() ? request : null;
    }

    static Snapshot loadSnapshot(Request request) {
        if (request == null || !request.isValid()) {
            return Snapshot.error();
        }

        try {
            Object memberSet = request.memberSet();
            if (memberSet == null) {
                return Snapshot.error();
            }

            MemberSetAccessors memberMethods = memberSetAccessors(memberSet.getClass());
            Map<Long, Long> watermarks = requireWatermarkSnapshot(
                    memberMethods.watermarks.invoke(memberSet)
            );
            int activeMemberCount = Math.max(
                    0,
                    ((Number) memberMethods.activeCount.invoke(memberSet)).intValue()
            );
            long currentUserId = number(memberMethods.currentUserId.invoke(memberSet));

            // KakaoTalk counts every watermark below the message id as unread, zero included, so
            // classifying zero the same way keeps this in step with the count it already shows.
            Set<Long> readerIds = new LinkedHashSet<>();
            Set<Long> nonReaderIds = new LinkedHashSet<>();
            for (Map.Entry<Long, Long> entry : watermarks.entrySet()) {
                long userId = entry.getKey();
                if (userId <= 0L) continue;

                if (entry.getValue() >= request.messageId) {
                    readerIds.add(userId);
                } else {
                    nonReaderIds.add(userId);
                }
            }

            // Neither carries a watermark of their own, but both have seen the message.
            markRead(readerIds, nonReaderIds, request.senderId);
            markRead(readerIds, nonReaderIds, currentUserId);

            int knownCount = readerIds.size() + nonReaderIds.size();
            // KakaoTalk can report more members than this device holds watermarks for.
            int audienceCount = Math.max(activeMemberCount, knownCount);

            return new Snapshot(
                    request.chatId,
                    request.messageId,
                    immutableList(readerIds),
                    immutableList(nonReaderIds),
                    audienceCount - knownCount,
                    audienceCount,
                    knownCount,
                    false
            );
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to load read receipts", throwable);
            return Snapshot.error();
        }
    }

    static Page loadPage(
            Request request,
            Snapshot snapshot,
            boolean readers,
            int offset,
            int limit
    ) {
        if (request == null || !request.isValid() || snapshot == null ||
                snapshot.failed || snapshot.chatId != request.chatId ||
                snapshot.messageId != request.messageId || offset < 0 || limit <= 0) {
            return Page.error();
        }

        List<Long> ids = readers ? snapshot.readerIds : snapshot.nonReaderIds;
        if (offset >= ids.size()) return Page.empty();

        try {
            Object memberSet = request.memberSet();
            if (memberSet == null) {
                return Page.error();
            }

            MemberSetAccessors memberMethods = memberSetAccessors(memberSet.getClass());
            int end = Math.min(ids.size(), offset + limit);
            List<Person> people = new ArrayList<>(end - offset);
            for (int index = offset; index < end; index++) {
                people.add(resolvePerson(memberSet, memberMethods, ids.get(index), readers));
            }
            return new Page(Collections.unmodifiableList(people), false);
        } catch (Throwable throwable) {
            Log.w(TAG, "Could not load read receipt profiles", throwable);
            return Page.error();
        }
    }

    /**
     * Resolving an incomplete profile makes KakaoTalk queue a member info fetch as a side effect,
     * which is what a later retry picks up. Without it this would repeat the same failed lookup.
     */
    static List<Person> retryUnresolvedProfiles(Request request, List<Person> people) {
        if (request == null || !request.isValid() || people == null || people.isEmpty()) {
            return people;
        }

        try {
            Object memberSet = request.memberSet();
            if (memberSet == null) return people;

            MemberSetAccessors memberMethods = memberSetAccessors(memberSet.getClass());
            List<Person> refreshed = null;
            for (int index = 0; index < people.size(); index++) {
                Person person = people.get(index);
                if (person.profileResolved) continue;

                Person retry = resolvePerson(
                        memberSet,
                        memberMethods,
                        person.userId,
                        person.read
                );
                if (!retry.profileResolved) continue;
                if (refreshed == null) refreshed = new ArrayList<>(people);
                refreshed.set(index, retry);
            }
            return refreshed == null ? people : Collections.unmodifiableList(refreshed);
        } catch (Throwable throwable) {
            Log.w(TAG, "Could not refresh unresolved read receipt profiles", throwable);
            return people;
        }
    }

    static boolean hasUnresolvedProfiles(List<Person> people) {
        if (people == null) return false;
        for (Person person : people) {
            if (!person.profileResolved) return true;
        }
        return false;
    }

    private static Person resolvePerson(
            Object memberSet,
            MemberSetAccessors memberMethods,
            long userId,
            boolean read
    ) {
        try {
            Object profile = memberMethods.profile.invoke(memberSet, userId);
            if (profile == null) return Person.fallback(userId, read);

            ProfileAccessors accessors = profileAccessors(profile.getClass());
            Object nameValue = accessors.name.invoke(profile);
            Object imageUrlValue = accessors.imageUrl.invoke(profile);
            boolean profileComplete = (Boolean) accessors.complete.invoke(profile);
            int imageType = ((Number) accessors.imageType.invoke(profile)).intValue();
            String name = nameValue instanceof String ? (String) nameValue : null;
            String imageUrl = imageUrlValue instanceof String ? (String) imageUrlValue : null;
            boolean hasName = name != null && !name.trim().isEmpty();
            return new Person(
                    userId,
                    hasName ? name : fallbackName(userId),
                    imageUrl,
                    imageType,
                    read,
                    profileComplete && hasName
            );
        } catch (Throwable throwable) {
            return Person.fallback(userId, read);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Long> requireWatermarkSnapshot(Object value) {
        if (!(value instanceof Map)) {
            throw new IllegalStateException("ChatMemberSet did not return a watermark snapshot");
        }
        return (Map<Long, Long>) value;
    }

    private static List<Long> immutableList(Set<Long> ids) {
        return Collections.unmodifiableList(new ArrayList<>(ids));
    }

    private static void markRead(Set<Long> readers, Set<Long> nonReaders, long userId) {
        if (userId <= 0L) return;
        nonReaders.remove(userId);
        readers.add(userId);
    }

    private static Request createRequest(Object chatLog) throws Exception {
        ChatLogAccessors accessors = chatLogAccessors(chatLog.getClass());
        Request request = new Request(
                number(accessors.chatId.invoke(chatLog)),
                number(accessors.logId.invoke(chatLog)),
                number(accessors.senderId.invoke(chatLog)),
                chatLog.getClass().getName()
        );
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid message read receipt request");
        }
        return request;
    }

    private static ChatLogAccessors chatLogAccessors(Class<?> type) throws NoSuchMethodException {
        ChatLogAccessors accessors = CHAT_LOG_ACCESSORS.get(type);
        if (accessors != null) return accessors;

        ChatLogAccessors created = new ChatLogAccessors(type);
        ChatLogAccessors previous = CHAT_LOG_ACCESSORS.putIfAbsent(type, created);
        return previous == null ? created : previous;
    }

    private static MemberSetAccessors memberSetAccessors(Class<?> type) throws NoSuchMethodException {
        MemberSetAccessors accessors = MEMBER_SET_ACCESSORS.get(type);
        if (accessors != null) return accessors;

        MemberSetAccessors created = new MemberSetAccessors(type);
        MemberSetAccessors previous = MEMBER_SET_ACCESSORS.putIfAbsent(type, created);
        return previous == null ? created : previous;
    }

    private static ProfileAccessors profileAccessors(Class<?> type) throws NoSuchMethodException {
        ProfileAccessors accessors = PROFILE_ACCESSORS.get(type);
        if (accessors != null) return accessors;

        ProfileAccessors created = new ProfileAccessors(type);
        ProfileAccessors previous = PROFILE_ACCESSORS.putIfAbsent(type, created);
        return previous == null ? created : previous;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static String fallbackName(long userId) {
        return resourceString("morphe_kakaotalk_chatlog_read_receipts_user", "User") +
                " " + userId;
    }

    private static String resourceString(String name, String fallback) {
        try {
            Context context = Utils.getContext();
            int resourceId = ResourceHelper.getResourceId("string", name);
            return resourceId == 0 ? fallback : context.getString(resourceId);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static final class ChatLogAccessors {
        final Method logId;
        final Method chatId;
        final Method senderId;
        final Method memberSetByChatId;

        ChatLogAccessors(Class<?> type) throws NoSuchMethodException {
            logId = type.getMethod(LOG_ID_METHOD);
            chatId = type.getMethod(CHAT_ID_METHOD);
            senderId = type.getMethod(SENDER_ID_METHOD);
            memberSetByChatId = type.getMethod(MEMBER_SET_BY_CHAT_ID_METHOD, long.class);
        }
    }

    static final class Request {
        final long chatId;
        final long messageId;
        final long senderId;
        final String chatLogClassName;

        Request(
                long chatId,
                long messageId,
                long senderId,
                String chatLogClassName
        ) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.senderId = senderId;
            this.chatLogClassName = chatLogClassName;
        }

        boolean isValid() {
            return chatId > 0L &&
                    messageId > 0L &&
                    chatLogClassName != null &&
                    !chatLogClassName.isEmpty();
        }

        Object memberSet() throws Exception {
            Class<?> chatLogType = Class.forName(
                    chatLogClassName,
                    false,
                    MessageReadReceiptsExtension.class.getClassLoader()
            );
            return chatLogAccessors(chatLogType).memberSetByChatId.invoke(null, chatId);
        }
    }

    private static final class MemberSetAccessors {
        final Method watermarks;
        final Method activeCount;
        final Method currentUserId;
        final Method profile;

        MemberSetAccessors(Class<?> type) throws NoSuchMethodException {
            watermarks = type.getMethod(WATERMARKS_METHOD);
            activeCount = type.getMethod(ACTIVE_COUNT_METHOD);
            currentUserId = type.getMethod(CURRENT_USER_ID_METHOD);
            profile = type.getMethod(PROFILE_METHOD, long.class);
        }
    }

    private static final class ProfileAccessors {
        final Method name;
        final Method imageUrl;
        final Method imageType;
        final Method complete;

        ProfileAccessors(Class<?> type) throws NoSuchMethodException {
            name = type.getMethod(PROFILE_NAME_METHOD);
            imageUrl = type.getMethod(PROFILE_IMAGE_URL_METHOD);
            imageType = type.getMethod(PROFILE_IMAGE_TYPE_METHOD);
            complete = type.getMethod(PROFILE_COMPLETE_METHOD);
        }
    }

    static final class Person {
        final long userId;
        final String name;
        final String imageUrl;
        final int imageType;
        final boolean read;
        final boolean profileResolved;

        Person(
                long userId,
                String name,
                String imageUrl,
                int imageType,
                boolean read,
                boolean profileResolved
        ) {
            this.userId = userId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.imageType = imageType;
            this.read = read;
            this.profileResolved = profileResolved;
        }

        static Person fallback(long userId, boolean read) {
            return new Person(
                    userId,
                    fallbackName(userId),
                    null,
                    DEFAULT_IMAGE_TYPE,
                    read,
                    false
            );
        }
    }

    static final class Snapshot {
        final long chatId;
        final long messageId;
        final List<Long> readerIds;
        final List<Long> nonReaderIds;
        final int unknownCount;
        final int audienceCount;
        final int knownCount;
        final boolean failed;

        Snapshot(
                long chatId,
                long messageId,
                List<Long> readerIds,
                List<Long> nonReaderIds,
                int unknownCount,
                int audienceCount,
                int knownCount,
                boolean failed
        ) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.readerIds = readerIds;
            this.nonReaderIds = nonReaderIds;
            this.unknownCount = unknownCount;
            this.audienceCount = audienceCount;
            this.knownCount = knownCount;
            this.failed = failed;
        }

        static Snapshot error() {
            return new Snapshot(
                    0L,
                    0L,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    true
            );
        }
    }

    static final class Page {
        final List<Person> people;
        final boolean failed;

        Page(List<Person> people, boolean failed) {
            this.people = people;
            this.failed = failed;
        }

        static Page empty() {
            return new Page(Collections.emptyList(), false);
        }

        static Page error() {
            return new Page(Collections.emptyList(), true);
        }
    }
}
