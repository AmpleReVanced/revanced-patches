package app.revanced.extension.unicorn.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class StdNative {
    private static final Class<?>[] NO_ARGS = {};
    private static final Class<?>[] STRING_ARGS = { String.class };
    private static final Class<?>[] LONG_ARGS = { long.class };
    private static final Class<?>[] LONG_INT_ARGS = { long.class, int.class };
    private static final Class<?>[] LONG_LONG_ARGS = { long.class, long.class };
    private static final Class<?>[] STRING_STRING_ARGS = { String.class, String.class };

    private StdNative() {
    }

    static Object call(String owner, String method, Object[] args) {
        switch (owner) {
            case NativeOwners.STD_EXCEPTION_PTR:
                return callExceptionPtr(method, args);
            case NativeOwners.STD_NATIVE_STRING:
                return callNativeString(method, args);
            case NativeOwners.STD_PAIR_SS:
                return callPairSS(method, args);
            case NativeOwners.STD_PAIR_SL:
                return callPairSL(method, args);
            case NativeOwners.STD_PAIR_LL:
                return callPairLL(method, args);
            case NativeOwners.STD_TUPLE_LLL:
                return callTupleLLL(method, args);
            case NativeOwners.STD_VECTOR_LONG:
                return callVectorLong(method, args);
            case NativeOwners.STD_UNORDERED_SET_S:
                return callUnorderedSetS(method, args);
            case NativeOwners.STD_UNORDERED_SET_S_ITERATOR:
                return callUnorderedSetSIterator(method, args);
            case NativeOwners.STD_UNORDERED_MAP_SS:
                return callUnorderedMapSS(method, args);
            case NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR:
                return callUnorderedMapSSIterator(method, args);
            case NativeOwners.STD_UNORDERED_MAP_SL:
                return callUnorderedMapSL(method, args);
            case NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR:
                return callUnorderedMapSLIterator(method, args);
            default:
                throw NativeRuntime.unsupported(owner + "." + method);
        }
    }

    private static Object callExceptionPtr(String method, Object[] args) {
        switch (method) {
            case "native_GetTypeId": {
                if (isOriginalHandle(args)) {
                    return original(NativeOwners.STD_EXCEPTION_PTR, method, LONG_ARGS, Long.valueOf(handle(args)));
                }
                Throwable throwable = NativeRuntime.throwable(handle(args));
                return NativeRuntime.longObject(throwable == null ? 0L : NativeRuntime.typeId(throwable.getClass().getName()));
            }
            case "native_delete":
                if (isOriginalHandle(args)) {
                    return original(NativeOwners.STD_EXCEPTION_PTR, method, LONG_ARGS, Long.valueOf(handle(args)));
                }
                return deleteAndReturnZero(args);
            default:
                throw unsupported(NativeOwners.STD_EXCEPTION_PTR, method);
        }
    }

    private static Object callNativeString(String method, Object[] args) {
        switch (method) {
            case "native_new":
                return original(NativeOwners.STD_NATIVE_STRING, method, STRING_ARGS, NativeRuntime.string(args[0]));
            case "native_ToJString":
                if (isOriginalHandle(args)) {
                    return original(NativeOwners.STD_NATIVE_STRING, method, LONG_ARGS, Long.valueOf(handle(args)));
                }
                return NativeRuntime.get(handle(args), String.class);
            case "native_delete":
                if (isOriginalHandle(args)) {
                    return original(NativeOwners.STD_NATIVE_STRING, method, LONG_ARGS, Long.valueOf(handle(args)));
                }
                return deleteAndReturnZero(args);
            default:
                throw unsupported(NativeOwners.STD_NATIVE_STRING, method);
        }
    }

    private static Object callPairSS(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return original(NativeOwners.STD_PAIR_SS, method, STRING_STRING_ARGS,
                    NativeRuntime.string(args[0]), NativeRuntime.string(args[1]));
        }

        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_PAIR_SS, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        PairSS pair = NativeRuntime.get(handle(args), PairSS.class);
        switch (method) {
            case "native_GetFirst":
                return pair.first;
            case "native_GetSecond":
                return pair.second;
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_PAIR_SS, method);
        }
    }

    private static Object callPairSL(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_PAIR_SL, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        PairSL pair = NativeRuntime.get(handle(args), PairSL.class);
        switch (method) {
            case "native_GetFirst":
                return pair.first;
            case "native_GetSecond":
                return NativeRuntime.longObject(pair.second);
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_PAIR_SL, method);
        }
    }

    private static Object callPairLL(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_PAIR_LL, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        PairLL pair = NativeRuntime.get(handle(args), PairLL.class);
        switch (method) {
            case "native_GetFirst":
                return NativeRuntime.longObject(pair.first);
            case "native_GetSecond":
                return NativeRuntime.longObject(pair.second);
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_PAIR_LL, method);
        }
    }

    private static Object callTupleLLL(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_TUPLE_LLL, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        TupleLLL tuple = NativeRuntime.get(handle(args), TupleLLL.class);
        switch (method) {
            case "native_Get0":
                return NativeRuntime.longObject(tuple.v0);
            case "native_Get1":
                return NativeRuntime.longObject(tuple.v1);
            case "native_Get2":
                return NativeRuntime.longObject(tuple.v2);
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_TUPLE_LLL, method);
        }
    }

    private static Object callVectorLong(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return original(NativeOwners.STD_VECTOR_LONG, method, NO_ARGS);
        }

        if (isOriginalHandle(args)) {
            switch (method) {
                case "native_At":
                    return original(NativeOwners.STD_VECTOR_LONG, method, LONG_INT_ARGS,
                            Long.valueOf(handle(args)), Integer.valueOf(NativeRuntime.intValue(args[1])));
                case "native_GetSize":
                case "native_delete":
                    return original(NativeOwners.STD_VECTOR_LONG, method, LONG_ARGS, Long.valueOf(handle(args)));
                case "native_PushBack":
                    return original(NativeOwners.STD_VECTOR_LONG, method, LONG_LONG_ARGS,
                            Long.valueOf(handle(args)), Long.valueOf(NativeRuntime.longValue(args[1])));
                default:
                    throw unsupported(NativeOwners.STD_VECTOR_LONG, method);
            }
        }

        LongVector vector = NativeRuntime.get(handle(args), LongVector.class);
        switch (method) {
            case "native_At":
                return vector.values.get(NativeRuntime.intValue(args[1]));
            case "native_GetSize":
                return NativeRuntime.longObject(vector.values.size());
            case "native_PushBack":
                vector.values.add(NativeRuntime.longValue(args[1]));
                return null;
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_VECTOR_LONG, method);
        }
    }

    private static Object callUnorderedSetS(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return original(NativeOwners.STD_UNORDERED_SET_S, method, NO_ARGS);
        }

        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_UNORDERED_SET_S, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        switch (method) {
            case "native_GetBegin": {
                Set<?> set = set(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(set), 0));
            }
            case "native_GetEnd": {
                Set<?> set = set(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(set), set.size()));
            }
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_SET_S, method);
        }
    }

    private static Object callUnorderedSetSIterator(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return originalUnorderedSetSIterator(method, args);
        }
        if ("native_IsEquals".equals(method) && isOriginalHandle(args, 1)) {
            return Boolean.FALSE;
        }

        NativeIterator iterator = iterator(args);
        switch (method) {
            case "native_Get":
                return NativeRuntime.string(iterator.value());
            case "native_GetNext":
                return NativeRuntime.handle(iterator.next());
            case "native_IsEquals":
                return Boolean.valueOf(iterator.isSamePosition(iterator(args, 1)));
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, method);
        }
    }

    private static Object callUnorderedMapSS(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return original(NativeOwners.STD_UNORDERED_MAP_SS, method, NO_ARGS);
        }

        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_UNORDERED_MAP_SS, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        switch (method) {
            case "native_GetBegin": {
                Map<?, ?> map = map(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(map.entrySet()), 0));
            }
            case "native_GetEnd": {
                Map<?, ?> map = map(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(map.entrySet()), map.size()));
            }
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SS, method);
        }
    }

    private static Object callUnorderedMapSSIterator(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return originalUnorderedMapSSIterator(method, args);
        }
        if ("native_IsEquals".equals(method) && isOriginalHandle(args, 1)) {
            return Boolean.FALSE;
        }

        NativeIterator iterator = iterator(args);
        switch (method) {
            case "native_Get": {
                Map.Entry<?, ?> entry = entry(iterator);
                return NativeRuntime.handle(new PairSS(
                        NativeRuntime.string(entry.getKey()),
                        NativeRuntime.string(entry.getValue())));
            }
            case "native_GetNext":
                return NativeRuntime.handle(iterator.next());
            case "native_IsEquals":
                return Boolean.valueOf(iterator.isSamePosition(iterator(args, 1)));
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, method);
        }
    }

    private static Object callUnorderedMapSL(String method, Object[] args) {
        if ("native_new".equals(method)) {
            return original(NativeOwners.STD_UNORDERED_MAP_SL, method, NO_ARGS);
        }

        if (isOriginalHandle(args)) {
            return original(NativeOwners.STD_UNORDERED_MAP_SL, method, LONG_ARGS, Long.valueOf(handle(args)));
        }

        switch (method) {
            case "native_GetBegin": {
                Map<?, ?> map = map(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(map.entrySet()), 0));
            }
            case "native_GetEnd": {
                Map<?, ?> map = map(args);
                return NativeRuntime.handle(new NativeIterator(new ArrayList<>(map.entrySet()), map.size()));
            }
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SL, method);
        }
    }

    private static Object callUnorderedMapSLIterator(String method, Object[] args) {
        if (isOriginalHandle(args)) {
            return originalUnorderedMapSLIterator(method, args);
        }
        if ("native_IsEquals".equals(method) && isOriginalHandle(args, 1)) {
            return Boolean.FALSE;
        }

        NativeIterator iterator = iterator(args);
        switch (method) {
            case "native_Get": {
                Map.Entry<?, ?> entry = entry(iterator);
                return NativeRuntime.handle(new PairSL(
                        NativeRuntime.string(entry.getKey()),
                        NativeRuntime.longValue(entry.getValue())));
            }
            case "native_GetNext":
                return NativeRuntime.handle(iterator.next());
            case "native_IsEquals":
                return Boolean.valueOf(iterator.isSamePosition(iterator(args, 1)));
            case "native_delete":
                return delete(args);
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, method);
        }
    }

    private static Object originalUnorderedSetSIterator(String method, Object[] args) {
        switch (method) {
            case "native_Get":
            case "native_GetNext":
            case "native_delete":
                return original(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, method, LONG_ARGS,
                        Long.valueOf(handle(args)));
            case "native_IsEquals":
                return original(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, method, LONG_LONG_ARGS,
                        Long.valueOf(handle(args)), Long.valueOf(NativeRuntime.longValue(args[1])));
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_SET_S_ITERATOR, method);
        }
    }

    private static Object originalUnorderedMapSSIterator(String method, Object[] args) {
        switch (method) {
            case "native_Get":
            case "native_GetNext":
            case "native_delete":
                return original(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, method, LONG_ARGS,
                        Long.valueOf(handle(args)));
            case "native_IsEquals":
                return original(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, method, LONG_LONG_ARGS,
                        Long.valueOf(handle(args)), Long.valueOf(NativeRuntime.longValue(args[1])));
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SS_ITERATOR, method);
        }
    }

    private static Object originalUnorderedMapSLIterator(String method, Object[] args) {
        switch (method) {
            case "native_Get":
            case "native_GetNext":
            case "native_delete":
                return original(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, method, LONG_ARGS,
                        Long.valueOf(handle(args)));
            case "native_IsEquals":
                return original(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, method, LONG_LONG_ARGS,
                        Long.valueOf(handle(args)), Long.valueOf(NativeRuntime.longValue(args[1])));
            default:
                throw unsupported(NativeOwners.STD_UNORDERED_MAP_SL_ITERATOR, method);
        }
    }

    private static long handle(Object[] args) {
        return NativeRuntime.longValue(args[0]);
    }

    private static boolean isOriginalHandle(Object[] args) {
        return isOriginalHandle(args, 0);
    }

    private static boolean isOriginalHandle(Object[] args, int index) {
        long handle = NativeRuntime.longValue(args[index]);
        return handle != 0L && NativeRuntime.getOrNull(handle) == null;
    }

    private static Object original(String owner, String method, Class<?>[] parameterTypes, Object... args) {
        return NativeFallback.call(owner, method, parameterTypes, args);
    }

    private static Object delete(Object[] args) {
        NativeRuntime.delete(handle(args));
        return null;
    }

    private static Long deleteAndReturnZero(Object[] args) {
        NativeRuntime.delete(handle(args));
        return NativeRuntime.longObject(0L);
    }

    private static NativeIterator iterator(Object[] args) {
        return iterator(args, 0);
    }

    private static NativeIterator iterator(Object[] args, int index) {
        return NativeRuntime.get(NativeRuntime.longValue(args[index]), NativeIterator.class);
    }

    private static Set<?> set(Object[] args) {
        Object value = NativeRuntime.get(handle(args));
        if (value instanceof Set) {
            return (Set<?>) value;
        }
        throw new IllegalArgumentException("Handle " + handle(args) + " is not a Set");
    }

    private static Map<?, ?> map(Object[] args) {
        Object value = NativeRuntime.get(handle(args));
        if (value instanceof Map) {
            return (Map<?, ?>) value;
        }
        throw new IllegalArgumentException("Handle " + handle(args) + " is not a Map");
    }

    private static Map.Entry<?, ?> entry(NativeIterator iterator) {
        Object value = iterator.value();
        if (value instanceof Map.Entry) {
            return (Map.Entry<?, ?>) value;
        }
        throw new IllegalStateException("Iterator value is not a map entry: " + value);
    }

    private static RuntimeException unsupported(String owner, String method) {
        return NativeRuntime.unsupported(owner + "." + method);
    }

    static final class PairSS {
        final String first;
        final String second;

        PairSS(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class PairSL {
        final String first;
        final long second;

        PairSL(String first, long second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class PairLL {
        final long first;
        final long second;

        PairLL(long first, long second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class TupleLLL {
        final long v0;
        final long v1;
        final long v2;

        TupleLLL(long v0, long v1, long v2) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    private static final class LongVector {
        final List<Long> values = new ArrayList<>();
    }

    private static final class NativeIterator {
        private final List<?> values;
        private final int index;

        NativeIterator(List<?> values, int index) {
            this.values = values;
            this.index = index;
        }

        Object value() {
            return values.get(index);
        }

        NativeIterator next() {
            return new NativeIterator(values, index + 1);
        }

        boolean isSamePosition(NativeIterator that) {
            return Objects.equals(values, that.values) && index == that.index;
        }
    }
}
