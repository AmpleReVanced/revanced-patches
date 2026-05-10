package app.revanced.extension.unicorn.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class StdNative {
    private StdNative() {
    }

    static Object call(String owner, String method, Object[] args) {
        if (owner.endsWith("/ExceptionPtr$Companion;")) {
            long handle = NativeRuntime.longValue(args[0]);
            if ("native_GetTypeId".equals(method)) {
                Throwable throwable = NativeRuntime.throwable(handle);
                return Long.valueOf(NativeRuntime.typeId(throwable.getClass().getName()));
            }
            NativeRuntime.delete(handle);
            return Long.valueOf(0L);
        }

        if (owner.endsWith("/NativeString$Companion;")) {
            if ("native_new".equals(method)) {
                return Long.valueOf(NativeRuntime.put(NativeRuntime.string(args[0])));
            }
            if ("native_ToJString".equals(method)) {
                return NativeRuntime.get(NativeRuntime.longValue(args[0]), String.class);
            }
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return Long.valueOf(0L);
        }

        if (owner.endsWith("/PairSS$Companion;")) {
            if ("native_new".equals(method)) {
                return Long.valueOf(NativeRuntime.put(new PairSS(NativeRuntime.string(args[0]), NativeRuntime.string(args[1]))));
            }
            PairSS pair = NativeRuntime.get(NativeRuntime.longValue(args[0]), PairSS.class);
            if ("native_GetFirst".equals(method)) return pair.first;
            if ("native_GetSecond".equals(method)) return pair.second;
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/PairSL$Companion;")) {
            PairSL pair = NativeRuntime.get(NativeRuntime.longValue(args[0]), PairSL.class);
            if ("native_GetFirst".equals(method)) return pair.first;
            if ("native_GetSecond".equals(method)) return Long.valueOf(pair.second);
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/PairLL$Companion;")) {
            PairLL pair = NativeRuntime.get(NativeRuntime.longValue(args[0]), PairLL.class);
            if ("native_GetFirst".equals(method)) return Long.valueOf(pair.first);
            if ("native_GetSecond".equals(method)) return Long.valueOf(pair.second);
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/TupleLLL$Companion;")) {
            TupleLLL tuple = NativeRuntime.get(NativeRuntime.longValue(args[0]), TupleLLL.class);
            if ("native_Get0".equals(method)) return Long.valueOf(tuple.v0);
            if ("native_Get1".equals(method)) return Long.valueOf(tuple.v1);
            if ("native_Get2".equals(method)) return Long.valueOf(tuple.v2);
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/VectorLong$Companion;")) {
            if ("native_new".equals(method)) {
                return Long.valueOf(NativeRuntime.put(new ArrayList<Long>()));
            }

            @SuppressWarnings("unchecked")
            ArrayList<Long> vector = NativeRuntime.get(NativeRuntime.longValue(args[0]), ArrayList.class);
            if ("native_At".equals(method)) return vector.get(NativeRuntime.intValue(args[1]));
            if ("native_GetSize".equals(method)) return Long.valueOf(vector.size());
            if ("native_PushBack".equals(method)) {
                vector.add(NativeRuntime.longValue(args[1]));
                return null;
            }
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.contains("/UnorderedSetS$Iterator$Companion;")) {
            Iter it = NativeRuntime.get(NativeRuntime.longValue(args[0]), Iter.class);
            if ("native_Get".equals(method)) return it.values.get(it.index);
            if ("native_GetNext".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(it.values, it.index + 1)));
            if ("native_IsEquals".equals(method)) {
                Iter that = NativeRuntime.get(NativeRuntime.longValue(args[1]), Iter.class);
                return Boolean.valueOf(Objects.equals(it.values, that.values) && it.index == that.index);
            }
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/UnorderedSetS$Companion;")) {
            if ("native_new".equals(method)) return Long.valueOf(NativeRuntime.put(new LinkedHashSet<String>()));
            @SuppressWarnings("unchecked")
            Set<String> set = NativeRuntime.get(NativeRuntime.longValue(args[0]), Set.class);
            if ("native_GetBegin".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(set), 0)));
            if ("native_GetEnd".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(set), set.size())));
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.contains("/UnorderedMapSS$Iterator$Companion;")) {
            Iter it = NativeRuntime.get(NativeRuntime.longValue(args[0]), Iter.class);
            if ("native_Get".equals(method)) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, String> entry = (Map.Entry<String, String>) it.values.get(it.index);
                return Long.valueOf(NativeRuntime.put(new PairSS(entry.getKey(), entry.getValue())));
            }
            if ("native_GetNext".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(it.values, it.index + 1)));
            if ("native_IsEquals".equals(method)) {
                Iter that = NativeRuntime.get(NativeRuntime.longValue(args[1]), Iter.class);
                return Boolean.valueOf(Objects.equals(it.values, that.values) && it.index == that.index);
            }
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/UnorderedMapSS$Companion;")) {
            if ("native_new".equals(method)) return Long.valueOf(NativeRuntime.put(new LinkedHashMap<String, String>()));
            @SuppressWarnings("unchecked")
            Map<String, String> map = NativeRuntime.get(NativeRuntime.longValue(args[0]), Map.class);
            if ("native_GetBegin".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(map.entrySet()), 0)));
            if ("native_GetEnd".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(map.entrySet()), map.size())));
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.contains("/UnorderedMapSL$Iterator$Companion;")) {
            Iter it = NativeRuntime.get(NativeRuntime.longValue(args[0]), Iter.class);
            if ("native_Get".equals(method)) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Long> entry = (Map.Entry<String, Long>) it.values.get(it.index);
                return Long.valueOf(NativeRuntime.put(new PairSL(entry.getKey(), entry.getValue())));
            }
            if ("native_GetNext".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(it.values, it.index + 1)));
            if ("native_IsEquals".equals(method)) {
                Iter that = NativeRuntime.get(NativeRuntime.longValue(args[1]), Iter.class);
                return Boolean.valueOf(Objects.equals(it.values, that.values) && it.index == that.index);
            }
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        if (owner.endsWith("/UnorderedMapSL$Companion;")) {
            if ("native_new".equals(method)) return Long.valueOf(NativeRuntime.put(new LinkedHashMap<String, Long>()));
            @SuppressWarnings("unchecked")
            Map<String, Long> map = NativeRuntime.get(NativeRuntime.longValue(args[0]), Map.class);
            if ("native_GetBegin".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(map.entrySet()), 0)));
            if ("native_GetEnd".equals(method)) return Long.valueOf(NativeRuntime.put(new Iter(new ArrayList<>(map.entrySet()), map.size())));
            NativeRuntime.delete(NativeRuntime.longValue(args[0]));
            return null;
        }

        throw NativeRuntime.unsupported(owner + "." + method);
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

    static final class Iter {
        final List<?> values;
        final int index;

        Iter(List<?> values, int index) {
            this.values = values;
            this.index = index;
        }
    }
}
