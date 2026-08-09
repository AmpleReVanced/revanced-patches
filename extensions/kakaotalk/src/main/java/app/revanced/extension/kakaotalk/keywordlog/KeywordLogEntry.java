package app.revanced.extension.kakaotalk.keywordlog;

public final class KeywordLogEntry {
    public final long id;
    public final long chatRoomId;
    public final long userId;
    public final String sender;
    public final String profileUrl;
    public final String chatRoomName;
    public final String message;
    public final long createdAt;
    public final boolean isProtected;

    KeywordLogEntry(
            long id,
            long chatRoomId,
            long userId,
            String sender,
            String profileUrl,
            String chatRoomName,
            String message,
            long createdAt,
            boolean isProtected
    ) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.sender = sender == null ? "" : sender;
        this.profileUrl = profileUrl;
        this.chatRoomName = chatRoomName == null ? "" : chatRoomName;
        this.message = message == null ? "" : message;
        this.createdAt = createdAt;
        this.isProtected = isProtected;
    }
}