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

    public static void m036cfc8d0c599368(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_MANAGER, "native_delete", p0, p1);
    }

    public static long m8414452a7a5c3930(Object p0, long p1, Object p2, Object p3, Object p4,
            Object p5, Object p6, Object p7) {
        return callLong(NativeOwners.LICENSE_MANAGER, "native_new", p0, p1, p2, p3, p4, p5, p6, p7);
    }

    public static String ma5b4c0801a777cf5(Object p0, long p1) {
        return callString(NativeOwners.LICENSE_DEVICE, "native_GetServerId", p0, p1);
    }

    public static void m96ba8afb9ae6c92a(Object p0, long p1) {
        callVoid(NativeOwners.LICENSE_DEVICE, "native_delete", p0, p1);
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

    public static String mded00a3efd9acf9c(Object p0, long p1) {
        return callString(NativeOwners.STD_NATIVE_STRING, "native_ToJString", p0, p1);
    }

    public static long m1b94789ade2e492a(Object p0, long p1) {
        return callLong(NativeOwners.STD_NATIVE_STRING, "native_delete", p0, p1);
    }

    public static long m288f4c0abe283226(Object p0, Object p1) {
        return callLong(NativeOwners.STD_NATIVE_STRING, "native_new", p0, p1);
    }

    public static long m0414dc6a9bc49ee9(Object p0, long p1) {
        return callLong(NativeOwners.STD_PAIR_LL, "native_GetFirst", p0, p1);
    }

    public static long mee4ac0151d673ab2(Object p0, long p1) {
        return callLong(NativeOwners.STD_PAIR_LL, "native_GetSecond", p0, p1);
    }

    public static void m61fef9aaf108c7c3(Object p0, long p1) {
        callVoid(NativeOwners.STD_PAIR_LL, "native_delete", p0, p1);
    }

    public static String m397b089ea138cd29(Object p0, long p1) {
        return callString(NativeOwners.STD_PAIR_SL, "native_GetFirst", p0, p1);
    }

    public static long m569e230fc0318c93(Object p0, long p1) {
        return callLong(NativeOwners.STD_PAIR_SL, "native_GetSecond", p0, p1);
    }

    public static void m7ff90b74073040c2(Object p0, long p1) {
        callVoid(NativeOwners.STD_PAIR_SL, "native_delete", p0, p1);
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

    public static long mebf6797e8545e6a5(Object p0, Object p1, Object p2) {
        return callLong(NativeOwners.STD_PAIR_SS, "native_new", p0, p1, p2);
    }

    public static long m06e91d155565a540(Object p0, long p1) {
        return callLong(NativeOwners.STD_TUPLE_LLL, "native_Get0", p0, p1);
    }

    public static long m10a20e26bbd48719(Object p0, long p1) {
        return callLong(NativeOwners.STD_TUPLE_LLL, "native_Get1", p0, p1);
    }

    public static long me710004cdfdf947b(Object p0, long p1) {
        return callLong(NativeOwners.STD_TUPLE_LLL, "native_Get2", p0, p1);
    }

    public static void m1f1006bb63fd3841(Object p0, long p1) {
        callVoid(NativeOwners.STD_TUPLE_LLL, "native_delete", p0, p1);
    }

    public static long ma996701e03132fd1(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SL, "native_GetBegin", p0, p1);
    }

    public static long m98ca91632f85a9aa(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SL, "native_GetEnd", p0, p1);
    }

    public static void m44b710204fdc3bd0(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_MAP_SL, "native_delete", p0, p1);
    }

    public static long m3ebe969c98b73c5c(Object p0) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SL, "native_new", p0);
    }

    public static long m29ebe0b9b1cdb67a(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, "native_Get", p0, p1);
    }

    public static long m7a4f9aaac8c5a3e0(Object p0, long p1) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, "native_GetNext", p0, p1);
    }

    public static boolean m1438279c5c776ffa(Object p0, long p1, long p2) {
        return callBoolean(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, "native_IsEquals", p0, p1, p2);
    }

    public static void m8c65c280b3b3463b(Object p0, long p1) {
        callVoid(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, "native_delete", p0, p1);
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

    public static long m81faf8058d9310fa(Object p0) {
        return callLong(NativeOwners.STD_UNORDERED_MAP_SS, "native_new", p0);
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

    public static long mabb931869d6ef608(Object p0) {
        return callLong(NativeOwners.STD_UNORDERED_SET_S, "native_new", p0);
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

    public static long mb0b16083c158aa96(Object p0, long p1, int p2) {
        return callLong(NativeOwners.STD_VECTOR_LONG, "native_At", p0, p1, p2);
    }

    public static long m5aa5803178466933(Object p0, long p1) {
        return callLong(NativeOwners.STD_VECTOR_LONG, "native_GetSize", p0, p1);
    }

    public static void m936d1d111d75c4de(Object p0, long p1, long p2) {
        callVoid(NativeOwners.STD_VECTOR_LONG, "native_PushBack", p0, p1, p2);
    }

    public static void mfb9cb632a9f77ca1(Object p0, long p1) {
        callVoid(NativeOwners.STD_VECTOR_LONG, "native_delete", p0, p1);
    }

    public static long m9ea6b30cf5d67361(Object p0) {
        return callLong(NativeOwners.STD_VECTOR_LONG, "native_new", p0);
    }
}
