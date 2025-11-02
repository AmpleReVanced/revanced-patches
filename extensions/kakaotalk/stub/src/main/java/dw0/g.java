package dw0;

import java.util.Map;
import java.util.Set;

/* BSONObject */
public interface g {
    Object a(String str, Object obj);

    boolean d(String str);

    Object get(String str);

    Set<String> keySet();

    Map toMap();
}
