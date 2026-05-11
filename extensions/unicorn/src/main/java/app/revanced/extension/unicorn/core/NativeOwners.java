package app.revanced.extension.unicorn.core;

final class NativeOwners {
    static final String CORE = "Lcom/unicornsoft/android/unicornpro/core/";
    static final String STD_PREFIX = CORE + "std/";

    static final String LICENSE = CORE + "License$Companion;";
    static final String LICENSE_MANAGER = CORE + "LicenseManager$Companion;";
    static final String LICENSE_AUTH = CORE + "LicenseManager$Auth$Companion;";
    static final String LICENSE_DEVICE = CORE + "LicenseManager$Device$Companion;";
    static final String LICENSE_TRACKER = CORE + "LicenseTracker$Companion;";
    static final String SIGN_IN_REQUIRE = CORE + "LicenseManager$SignInRequireException$Companion;";
    static final String UNCONFIRMED_EMAIL = CORE + "LicenseManager$UnconfirmedEmailException$Companion;";

    static final String STD_EXCEPTION_PTR = STD_PREFIX + "ExceptionPtr$Companion;";
    static final String STD_PAIR_SS = STD_PREFIX + "PairSS$Companion;";
    static final String STD_UNORDERED_MAP_SS = STD_PREFIX + "UnorderedMapSS$Companion;";
    static final String STD_UNORDERED_MAP_SS_ITERATOR = STD_PREFIX + "UnorderedMapSS$Iterator$Companion;";
    static final String STD_UNORDERED_SET_S = STD_PREFIX + "UnorderedSetS$Companion;";
    static final String STD_UNORDERED_SET_S_ITERATOR = STD_PREFIX + "UnorderedSetS$Iterator$Companion;";

    private NativeOwners() {
    }
}
