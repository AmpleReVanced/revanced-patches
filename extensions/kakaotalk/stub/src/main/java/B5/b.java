package B5;

import android.content.Context;
import java.util.List;

/* Initializer */
public interface b<T> {
    T create(Context context);

    List<Class<? extends b<?>>> dependencies();
}
