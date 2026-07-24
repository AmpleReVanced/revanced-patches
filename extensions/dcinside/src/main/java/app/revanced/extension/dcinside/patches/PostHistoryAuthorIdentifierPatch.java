package app.revanced.extension.dcinside.patches;

import androidx.annotation.Nullable;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressWarnings("unused")
public final class PostHistoryAuthorIdentifierPatch {
    /**
     * Zero width, so the identifier can share the single stored name column.
     */
    private static final char IDENTIFIER_SEPARATOR = '\u2063';

    private PostHistoryAuthorIdentifierPatch() {
    }

    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    public static String foldAuthorIdentifier(
            @Nullable String name,
            @Nullable String userId,
            @Nullable String ip
    ) {
        String nickname = name == null ? "" : nicknameOf(name);

        String identifier = firstNonBlank(userId, ip);
        return identifier == null ? nickname : nickname + IDENTIFIER_SEPARATOR + identifier;
    }

    /**
     * Recently viewed posts are copied into the post archive and post series tables,
     * which must keep storing the original nickname.
     */
    public static String stripAuthorIdentifier(@Nullable String storedName) {
        return storedName == null ? "" : nicknameOf(storedName);
    }

    public static String formatAuthorName(@Nullable String storedName) {
        if (storedName == null) {
            return "";
        }

        String nickname = nicknameOf(storedName);
        if (nickname.length() == storedName.length() || !Settings.showPostHistoryAuthorIdentifier()) {
            return nickname;
        }

        String identifier = storedName.substring(nickname.length() + 1);
        return identifier.isEmpty() ? nickname : nickname + " (" + identifier + ")";
    }

    private static String nicknameOf(String storedName) {
        int separatorIndex = storedName.indexOf(IDENTIFIER_SEPARATOR);
        return separatorIndex < 0 ? storedName : storedName.substring(0, separatorIndex);
    }

    @Nullable
    private static String firstNonBlank(@Nullable String first, @Nullable String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }
}