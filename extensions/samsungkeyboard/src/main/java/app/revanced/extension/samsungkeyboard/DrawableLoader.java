package app.revanced.extension.samsungkeyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.samsung.android.spr.drawable.Spr;

import java.util.function.Supplier;

public final class DrawableLoader {
    private DrawableLoader() {
    }

    public static Drawable getDrawable(Context context, int id) {
        return getDrawable(
                () -> context.getDrawable(id),
                context.getResources(),
                id,
                context.getTheme()
        );
    }

    public static Drawable getDrawable(Resources resources, int id) {
        return getDrawable(() -> resources.getDrawable(id, null), resources, id, null);
    }

    public static Drawable getDrawable(Resources resources, int id, Resources.Theme theme) {
        return getDrawable(() -> resources.getDrawable(id, theme), resources, id, theme);
    }

    public static Drawable getDrawable(TypedArray array, int index) {
        int id = array.getResourceId(index, 0);
        return getDrawable(() -> array.getDrawable(index), array.getResources(), id, null);
    }

    public static Bitmap decodeResource(Resources resources, int id) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(resources, id);
            if (bitmap != null) return bitmap;
        } catch (Resources.NotFoundException ignored) {
        }

        Drawable drawable = getDrawable(resources, id, null);
        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(result));
        return result;
    }

    private static Drawable getDrawable(
            Supplier<Drawable> loader,
            Resources resources,
            int id,
            Resources.Theme theme
    ) {
        try {
            Drawable drawable = loader.get();
            if (drawable != null) return drawable;
        } catch (Resources.NotFoundException ignored) {
        }
        return id == 0 ? null : getSprDrawable(resources, id, theme);
    }

    private static Drawable getSprDrawable(Resources resources, int id, Resources.Theme theme) {
        try {
            Drawable drawable = Spr.getDrawable(resources, id, theme);
            return drawable != null ? drawable : new ColorDrawable();
        } catch (RuntimeException | LinkageError ignored) {
            return new ColorDrawable();
        }
    }
}