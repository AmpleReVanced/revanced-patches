package app.revanced.extension.kakaotalk.settings;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class MorpheSettingsIconDynamicDrawable extends Drawable {
    private Drawable icon;
    private Boolean lastKnownDarkMode;

    public MorpheSettingsIconDynamicDrawable() {
        updateIcon();
    }

    private void updateIcon() {
        boolean isDarkMode = Utils.isDarkModeEnabled();
        if (lastKnownDarkMode != null && lastKnownDarkMode == isDarkMode) {
            return;
        }

        lastKnownDarkMode = isDarkMode;

        String iconName = isDarkMode
                ? "morphe_settings_icon_dark"
                : "morphe_settings_icon_light";
        Drawable newIcon = ResourceUtils.getDrawableOrThrow(iconName);

        if (newIcon == null) {
            throw new IllegalStateException("Failed to load icon: " + iconName);
        }

        newIcon.setBounds(getBounds());
        icon = newIcon;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        updateIcon();
        icon.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        icon.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        icon.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return icon == null ? PixelFormat.TRANSLUCENT : icon.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
        return icon.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return icon.getIntrinsicHeight();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        if (icon != null) {
            icon.setBounds(left, top, right, bottom);
        }
    }

    @Override
    public void setBounds(@NonNull Rect bounds) {
        super.setBounds(bounds);
        if (icon != null) {
            icon.setBounds(bounds);
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        if (icon != null) {
            icon.setBounds(bounds);
        }
    }
}
