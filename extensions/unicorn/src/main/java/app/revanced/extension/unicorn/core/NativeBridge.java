package app.revanced.extension.unicorn.core;

@SuppressWarnings("unused")
public final class NativeBridge {
    private static final String STRING = "Ljava/lang/String;";

    private NativeBridge() {
    }

    private static Object call(String owner, String method, String returnType, Object... args) {
        return NativeRouter.call(owner, method, returnType, true, args);
    }

    private static long callLong(String owner, String method, Object... args) {
        return ((Number) call(owner, method, "J", args)).longValue();
    }

    private static int callInt(String owner, String method, Object... args) {
        return ((Number) call(owner, method, "I", args)).intValue();
    }

    private static boolean callBoolean(String owner, String method, Object... args) {
        return ((Boolean) call(owner, method, "Z", args)).booleanValue();
    }

    private static String callString(String owner, String method, Object... args) {
        return (String) call(owner, method, STRING, args);
    }

    private static void callVoid(String owner, String method, Object... args) {
        call(owner, method, "V", args);
    }

    public static long m715348f8049657fc(Object p0, long p1) {
        return callLong(NativeOwners.LICENSE, "native_GetDeviceIds", p0, p1);
    }

    public static String mdc04b1f2e89ccaa2(Object p0, long p1) {
        return callString(NativeOwners.LICENSE, "native_GetExpiredAt", p0, p1);
    }

    public static String m7d0e55c26b376e34(Object p0, long p1) {
        return callString(NativeOwners.LICENSE, "native_GetId", p0, p1);
    }

    public static boolean mbf9735a2d86c24fe(Object p0, long p1) {
        return callBoolean(NativeOwners.LICENSE, "native_GetIsExpired", p0, p1);
    }

    public static boolean md4b5ff058dd8ab01(Object p0, long p1) {
        return callBoolean(NativeOwners.LICENSE, "native_GetIsTrial", p0, p1);
    }

    public static int m28803de969aeb2cc(Object p0, long p1) {
        return callInt(NativeOwners.LICENSE, "native_GetLife", p0, p1);
    }

    public static long mdb20802c367ed83c(Object p0, long p1) {
        return callLong(NativeOwners.LICENSE, "native_GetLocalizations", p0, p1);
    }

    public static int m5b1e470e6be8e4f3(Object p0, long p1) {
        return callInt(NativeOwners.LICENSE, "native_GetMaxDevice", p0, p1);
    }

    public static String mc0325b4fc02cd8cb(Object p0, long p1) {
        return callString(NativeOwners.LICENSE, "native_GetScope", p0, p1);
    }

    public static void m5fa306e1d8835826(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE, "native_delete", p0, p1);
    }

    public static String m115b3fcab8a70ca0(Object p0, long p1) {
        return callString(NativeOwners.LICENSE_AUTH, "native_GetEmail", p0, p1);
    }

    public static String me083b9e1186842a3(Object p0, long p1) {
        return callString(NativeOwners.LICENSE_AUTH, "native_GetId", p0, p1);
    }

    public static int mf2d3a0b8e8ca96fa(Object p0, long p1) {
        return callInt(NativeOwners.LICENSE_AUTH, "native_GetScope", p0, p1);
    }

    public static void mc6372b8ef3c86f5b(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_AUTH, "native_delete", p0, p1);
    }

    public static void m8623dc8760dc5691(Object p0, long p1, Object p2, Object p3) {
        callVoid(NativeOwners.LICENSE_MANAGER, "native_DetachLicense", p0, p1, p2, p3);
    }

    public static long m2fee7fc9b62344c8(Object p0, long p1) {
        return callLong(NativeOwners.LICENSE_MANAGER, "native_GetAuth", p0, p1);
    }

    public static long m6bce19c6636f11ca(Object p0, long p1) {
        return callLong(NativeOwners.LICENSE_MANAGER, "native_GetDevice", p0, p1);
    }

    public static long me8410b9d13d64469(Object p0, long p1) {
        return callLong(NativeOwners.LICENSE_MANAGER, "native_GetLicense", p0, p1);
    }

    public static void m6acc1fbeb61f0750(Object p0, long p1, Object p2, Object p3, Object p4,
            Object p5, Object p6) {
        callVoid(NativeOwners.LICENSE_MANAGER, "native_SignIn", p0, p1, p2, p3, p4, p5, p6);
    }

    public static void m4e9f61a73dfe5088(Object p0, long p1, Object p2, Object p3) {
        callVoid(NativeOwners.LICENSE_MANAGER, "native_StartAppProcess", p0, p1, p2, p3);
    }

    public static String ma5b4c0801a777cf5(Object p0, long p1) {
        return callString(NativeOwners.LICENSE_DEVICE, "native_GetServerId", p0, p1);
    }

    public static void m96ba8afb9ae6c92a(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_DEVICE, "native_delete", p0, p1);
    }

    public static void m2439bfc7ef160335(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_TRACKER, "native_Interrupt", p0, p1);
    }

    public static void mff4b3a6b883b3088(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_TRACKER, "native_delete", p0, p1);
    }

    public static long m89642d75e6470427(Object p0, long p1, long p2, Object p3, Object p4) {
        return callLong(NativeOwners.LICENSE_TRACKER, "native_new", p0, p1, p2, p3, p4);
    }

    public static long m7c366fc365c7dd33(Object p0) {
        return callLong(NativeOwners.SIGN_IN_REQUIRE, "native_GetTypeId", p0);
    }

    public static long m885ee73c1932c9a4(Object p0) {
        return callLong(NativeOwners.UNCONFIRMED_EMAIL, "native_GetTypeId", p0);
    }

    public static String ma8870bb25c994b1b(Object p0, long p1) {
        return callString(NativeOwners.UNCONFIRMED_EMAIL, "native_GetUserId", p0, p1);
    }

    public static long mcc11acb774ffc272(Object p0, long p1) {
        return callLong(NativeOwners.STD_EXCEPTION_PTR, "native_GetTypeId", p0, p1);
    }

    public static long m9301ddb78b20b9e8(Object p0, long p1) {
        return callLong(NativeOwners.STD_EXCEPTION_PTR, "native_delete", p0, p1);
    }

    public static String m65f34785626099c6(Object p0, long p1) {
        return callString(NativeOwners.STD_PAIR_SS, "native_GetFirst", p0, p1);
    }

    public static String m50ad14dfdc2ae248(Object p0, long p1) {
        return callString(NativeOwners.STD_PAIR_SS, "native_GetSecond", p0, p1);
    }

    public static void me1f8bca94c600626(Object p0, long p1) {
        callVoid(NativeOwners.STD_PAIR_SS, "native_delete", p0, p1);
    }

    public static long m95cf28573c2b4d9a(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SS, "native_GetBegin", p0, p1);
    }

    public static long mbed55c54300255a2(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SS, "native_GetEnd", p0, p1);
    }

    public static void m463c8317904e0501(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_MAP_SS, "native_delete", p0, p1);
    }

    public static long m92cedfb5899f083c(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, "native_Get", p0, p1);
    }

    public static long mf8ad653afd07b0f9(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, "native_GetNext", p0, p1);
    }

    public static boolean m866727f667a6cb73(Object p0, long p1, long p2) {
        return callBoolean(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, "native_IsEquals", p0, p1, p2);
    }

    public static void medbded9d0757bf2c(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, "native_delete", p0, p1);
    }

    public static long mfae357385145c47d(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_SET_S, "native_GetBegin", p0, p1);
    }

    public static long m746a945b865f03b1(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_SET_S, "native_GetEnd", p0, p1);
    }

    public static void m56df97604182b578(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_SET_S, "native_delete", p0, p1);
    }

    public static String m33f40b3da2403949(Object p0, long p1) {
        return callString(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, "native_Get", p0, p1);
    }

    public static long ma948b8693e4587ad(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, "native_GetNext", p0, p1);
    }

    public static boolean m67b22ec80d27b778(Object p0, long p1, long p2) {
        return callBoolean(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, "native_IsEquals", p0, p1, p2);
    }

    public static void m5c7c4bbec5b7d68a(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, "native_delete", p0, p1);
    }
}
