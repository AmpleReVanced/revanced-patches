package app.revanced.extension.unicorn.core;

final class NativeOwners {
    static final String CORE = "Lcom/unicornsoft/android/unicornpro/core/";
    static final String STD_PREFIX = CORE + "std/";
    static final String CONFIG_PREFIX = CORE + "config/";

    static final String CONFIG = CORE + "Config$Companion;";
    static final String CONFIG_FILE = CONFIG_PREFIX + "File$Companion;";

    static final String LICENSE = CORE + "License$Companion;";
    static final String LICENSE_MANAGER = CORE + "LicenseManager$Companion;";
    static final String LICENSE_AUTH = CORE + "LicenseManager$Auth$Companion;";
    static final String LICENSE_DEVICE = CORE + "LicenseManager$Device$Companion;";
    static final String SIGN_IN_REQUIRE = CORE + "LicenseManager$SignInRequireException$Companion;";
    static final String UNCONFIRMED_EMAIL = CORE + "LicenseManager$UnconfirmedEmailException$Companion;";

    static final String STD_EXCEPTION_PTR = STD_PREFIX + "ExceptionPtr$Companion;";
    static final String STD_NATIVE_STRING = STD_PREFIX + "NativeString$Companion;";
    static final String STD_PAIR_LL = STD_PREFIX + "PairLL$Companion;";
    static final String STD_PAIR_SL = STD_PREFIX + "PairSL$Companion;";
    static final String STD_PAIR_SS = STD_PREFIX + "PairSS$Companion;";
    static final String STD_TUPLE_LLL = STD_PREFIX + "TupleLLL$Companion;";
    static final String STD_UNORDERED_MAP_SL = STD_PREFIX + "UnorderedMapSL$Companion;";
    static final String STD_UNORDERED_MAP_SL_ITERATOR = STD_PREFIX + "UnorderedMapSL$Iterator$Companion;";
    static final String STD_UNORDERED_MAP_SS = STD_PREFIX + "UnorderedMapSS$Companion;";
    static final String STD_UNORDERED_MAP_SS_ITERATOR = STD_PREFIX + "UnorderedMapSS$Iterator$Companion;";
    static final String STD_UNORDERED_SET_S = STD_PREFIX + "UnorderedSetS$Companion;";
    static final String STD_UNORDERED_SET_S_ITERATOR = STD_PREFIX + "UnorderedSetS$Iterator$Companion;";
    static final String STD_VECTOR_LONG = STD_PREFIX + "VectorLong$Companion;";

    private NativeOwners() {
    }
}
