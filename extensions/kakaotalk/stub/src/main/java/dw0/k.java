package dw0;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* BasicBSONObject */
public class k extends LinkedHashMap<String, Object> implements g {
    public k() {
    }

    public static Object e(Object obj) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static k f(g interfaceC26931g) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static List g(List<Object> list) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static Map<String, Object> h(Map<String, Object> map) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // dw0.InterfaceC26931g
    public /* bridge */ /* synthetic */ Object a(String str, Object obj) {
        return super.put(str, obj);
    }

    public k c(String str, Object obj) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // dw0.InterfaceC26931g
    public boolean d(String str) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // dw0.InterfaceC26931g
    public Object get(String str) {
        return super.get((Object) str);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int hashCode() {
        throw new UnsupportedOperationException("Stub!");
    }

    public final byte[] l() {
        throw new UnsupportedOperationException("Stub!");
    }

    public final Object m() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
    public void putAll(Map map) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override // dw0.InterfaceC26931g
    public Map toMap() {
        return new LinkedHashMap(this);
    }

    public k(String str, Object obj) {
        put(str, obj);
    }

    public k(Map map) {
        super(map);
    }
}
