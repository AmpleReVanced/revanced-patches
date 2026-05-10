package app.revanced.extension.unicorn.core;

public final class NativeBridge {
    private NativeBridge() {
    }

    public static long m715348f8049657fc(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetDeviceIds", "J", true, p0, p1)).longValue();
    }

    public static String mdc04b1f2e89ccaa2(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetExpiredAt", "Ljava/lang/String;", true, p0, p1);
    }

    public static String m7d0e55c26b376e34(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetId", "Ljava/lang/String;", true, p0, p1);
    }

    public static boolean mbf9735a2d86c24fe(Object p0, long p1) {
        return ((Boolean) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetIsExpired", "Z", true, p0, p1)).booleanValue();
    }

    public static boolean md4b5ff058dd8ab01(Object p0, long p1) {
        return ((Boolean) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetIsTrial", "Z", true, p0, p1)).booleanValue();
    }

    public static int m28803de969aeb2cc(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetLife", "I", true, p0, p1)).intValue();
    }

    public static long mdb20802c367ed83c(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetLocalizations", "J", true, p0, p1)).longValue();
    }

    public static int m5b1e470e6be8e4f3(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetMaxDevice", "I", true, p0, p1)).intValue();
    }

    public static String mc0325b4fc02cd8cb(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_GetScope", "Ljava/lang/String;", true, p0, p1);
    }

    public static void m5fa306e1d8835826(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/License$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static String m115b3fcab8a70ca0(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Auth$Companion;", "native_GetEmail", "Ljava/lang/String;", true, p0, p1);
    }

    public static String me083b9e1186842a3(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Auth$Companion;", "native_GetId", "Ljava/lang/String;", true, p0, p1);
    }

    public static int mf2d3a0b8e8ca96fa(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Auth$Companion;", "native_GetScope", "I", true, p0, p1)).intValue();
    }

    public static void mc6372b8ef3c86f5b(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Auth$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static void m8623dc8760dc5691(Object p0, long p1, Object p2, Object p3) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_DetachLicense", "V", true, p0, p1, p2, p3);
    }

    public static long m2fee7fc9b62344c8(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_GetAuth", "J", true, p0, p1)).longValue();
    }

    public static long m6bce19c6636f11ca(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_GetDevice", "J", true, p0, p1)).longValue();
    }

    public static long me8410b9d13d64469(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_GetLicense", "J", true, p0, p1)).longValue();
    }

    public static void m6acc1fbeb61f0750(Object p0, long p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_SignIn", "V", true, p0, p1, p2, p3, p4, p5, p6);
    }

    public static void m4e9f61a73dfe5088(Object p0, long p1, Object p2, Object p3) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_StartAppProcess", "V", true, p0, p1, p2, p3);
    }

    public static void m036cfc8d0c599368(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m8414452a7a5c3930(Object p0, long p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Companion;", "native_new", "J", true, p0, p1, p2, p3, p4, p5, p6, p7)).longValue();
    }

    public static String ma5b4c0801a777cf5(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Device$Companion;", "native_GetServerId", "Ljava/lang/String;", true, p0, p1);
    }

    public static void m96ba8afb9ae6c92a(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$Device$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m7c366fc365c7dd33(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$SignInRequireException$Companion;", "native_GetTypeId", "J", true, p0)).longValue();
    }

    public static long m885ee73c1932c9a4(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$UnconfirmedEmailException$Companion;", "native_GetTypeId", "J", true, p0)).longValue();
    }

    public static String ma8870bb25c994b1b(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/LicenseManager$UnconfirmedEmailException$Companion;", "native_GetUserId", "Ljava/lang/String;", true, p0, p1);
    }

    public static long mcc11acb774ffc272(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/ExceptionPtr$Companion;", "native_GetTypeId", "J", true, p0, p1)).longValue();
    }

    public static long m9301ddb78b20b9e8(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/ExceptionPtr$Companion;", "native_delete", "J", true, p0, p1)).longValue();
    }

    public static String mded00a3efd9acf9c(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/NativeString$Companion;", "native_ToJString", "Ljava/lang/String;", true, p0, p1);
    }

    public static long m1b94789ade2e492a(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/NativeString$Companion;", "native_delete", "J", true, p0, p1)).longValue();
    }

    public static long m288f4c0abe283226(Object p0, Object p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/NativeString$Companion;", "native_new", "J", true, p0, p1)).longValue();
    }

    public static long m0414dc6a9bc49ee9(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairLL$Companion;", "native_GetFirst", "J", true, p0, p1)).longValue();
    }

    public static long mee4ac0151d673ab2(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairLL$Companion;", "native_GetSecond", "J", true, p0, p1)).longValue();
    }

    public static void m61fef9aaf108c7c3(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairLL$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static String m397b089ea138cd29(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSL$Companion;", "native_GetFirst", "Ljava/lang/String;", true, p0, p1);
    }

    public static long m569e230fc0318c93(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSL$Companion;", "native_GetSecond", "J", true, p0, p1)).longValue();
    }

    public static void m7ff90b74073040c2(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSL$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static String m65f34785626099c6(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSS$Companion;", "native_GetFirst", "Ljava/lang/String;", true, p0, p1);
    }

    public static String m50ad14dfdc2ae248(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSS$Companion;", "native_GetSecond", "Ljava/lang/String;", true, p0, p1);
    }

    public static void me1f8bca94c600626(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSS$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long mebf6797e8545e6a5(Object p0, Object p1, Object p2) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/PairSS$Companion;", "native_new", "J", true, p0, p1, p2)).longValue();
    }

    public static long m06e91d155565a540(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/TupleLLL$Companion;", "native_Get0", "J", true, p0, p1)).longValue();
    }

    public static long m10a20e26bbd48719(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/TupleLLL$Companion;", "native_Get1", "J", true, p0, p1)).longValue();
    }

    public static long me710004cdfdf947b(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/TupleLLL$Companion;", "native_Get2", "J", true, p0, p1)).longValue();
    }

    public static void m1f1006bb63fd3841(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/TupleLLL$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long ma996701e03132fd1(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Companion;", "native_GetBegin", "J", true, p0, p1)).longValue();
    }

    public static long m98ca91632f85a9aa(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Companion;", "native_GetEnd", "J", true, p0, p1)).longValue();
    }

    public static void m44b710204fdc3bd0(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m3ebe969c98b73c5c(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Companion;", "native_new", "J", true, p0)).longValue();
    }

    public static long m29ebe0b9b1cdb67a(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Iterator$Companion;", "native_Get", "J", true, p0, p1)).longValue();
    }

    public static long m7a4f9aaac8c5a3e0(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Iterator$Companion;", "native_GetNext", "J", true, p0, p1)).longValue();
    }

    public static boolean m1438279c5c776ffa(Object p0, long p1, long p2) {
        return ((Boolean) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Iterator$Companion;", "native_IsEquals", "Z", true, p0, p1, p2)).booleanValue();
    }

    public static void m8c65c280b3b3463b(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSL$Iterator$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m95cf28573c2b4d9a(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Companion;", "native_GetBegin", "J", true, p0, p1)).longValue();
    }

    public static long mbed55c54300255a2(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Companion;", "native_GetEnd", "J", true, p0, p1)).longValue();
    }

    public static void m463c8317904e0501(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m81faf8058d9310fa(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Companion;", "native_new", "J", true, p0)).longValue();
    }

    public static long m92cedfb5899f083c(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Iterator$Companion;", "native_Get", "J", true, p0, p1)).longValue();
    }

    public static long mf8ad653afd07b0f9(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Iterator$Companion;", "native_GetNext", "J", true, p0, p1)).longValue();
    }

    public static boolean m866727f667a6cb73(Object p0, long p1, long p2) {
        return ((Boolean) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Iterator$Companion;", "native_IsEquals", "Z", true, p0, p1, p2)).booleanValue();
    }

    public static void medbded9d0757bf2c(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedMapSS$Iterator$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long mfae357385145c47d(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Companion;", "native_GetBegin", "J", true, p0, p1)).longValue();
    }

    public static long m746a945b865f03b1(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Companion;", "native_GetEnd", "J", true, p0, p1)).longValue();
    }

    public static void m56df97604182b578(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long mabb931869d6ef608(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Companion;", "native_new", "J", true, p0)).longValue();
    }

    public static String m33f40b3da2403949(Object p0, long p1) {
        return (String) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Iterator$Companion;", "native_Get", "Ljava/lang/String;", true, p0, p1);
    }

    public static long ma948b8693e4587ad(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Iterator$Companion;", "native_GetNext", "J", true, p0, p1)).longValue();
    }

    public static boolean m67b22ec80d27b778(Object p0, long p1, long p2) {
        return ((Boolean) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Iterator$Companion;", "native_IsEquals", "Z", true, p0, p1, p2)).booleanValue();
    }

    public static void m5c7c4bbec5b7d68a(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/UnorderedSetS$Iterator$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long mb0b16083c158aa96(Object p0, long p1, int p2) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/VectorLong$Companion;", "native_At", "J", true, p0, p1, p2)).longValue();
    }

    public static long m5aa5803178466933(Object p0, long p1) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/VectorLong$Companion;", "native_GetSize", "J", true, p0, p1)).longValue();
    }

    public static void m936d1d111d75c4de(Object p0, long p1, long p2) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/VectorLong$Companion;", "native_PushBack", "V", true, p0, p1, p2);
    }

    public static void mfb9cb632a9f77ca1(Object p0, long p1) {
        NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/VectorLong$Companion;", "native_delete", "V", true, p0, p1);
    }

    public static long m9ea6b30cf5d67361(Object p0) {
        return ((Number) NativeRouter.call("Lcom/unicornsoft/android/unicornpro/core/std/VectorLong$Companion;", "native_new", "J", true, p0)).longValue();
    }
}
