package app.revanced.extension.kakaotalk.settings;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class MorpheSettingsIconDynamicDrawable extends Drawable {
    private Drawable icon;
    private Boolean lastKnownDarkMode;
    private int alpha = 0xFF;
    private ColorFilter colorFilter;
    private ColorStateList tintList;
    private PorterDuff.Mode tintMode;

    public MorpheSettingsIconDynamicDrawable() {
        updateIcon();
    }

    private void updateIcon() {
        boolean isDarkMode = isDarkMode();
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
        newIcon.setState(getState());
        newIcon.setAlpha(alpha);
        if (colorFilter != null) {
            newIcon.setColorFilter(colorFilter);
        }
        if (tintList != null) {
            newIcon.setTintList(tintList);
        }
        if (tintMode != null) {
            newIcon.setTintMode(tintMode);
        }
        icon = newIcon;
    }

    private boolean isDarkMode() {
        Boolean darkModeFromTint = getDarkModeFromTint();
        if (darkModeFromTint != null) {
            return darkModeFromTint;
        }

        Boolean darkModeFromThemeTitleColor = getDarkModeFromThemeTitleColor();
        if (darkModeFromThemeTitleColor != null) {
            return darkModeFromThemeTitleColor;
        }

        return Utils.isDarkModeEnabled();
    }

    @Nullable
    private Boolean getDarkModeFromTint() {
        if (tintList == null) {
            return null;
        }

        int color = tintList.getColorForState(getState(), tintList.getDefaultColor());
        return isLightColor(color);
    }

    @Nullable
    private Boolean getDarkModeFromThemeTitleColor() {
        try {
            int colorResId = ResourceHelper.getResourceId("color", "theme_title_color");
            if (colorResId == 0) {
                return null;
            }

            int color = Utils.getContext().getResources().getColor(colorResId);
            return isLightColor(color);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isLightColor(int color) {
        return Color.alpha(color) != 0
                && ((Color.red(color) * 299) + (Color.green(color) * 587) + (Color.blue(color) * 114)) >= 128000;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        updateIcon();
        icon.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        if (icon != null) {
            icon.setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
        if (icon != null) {
            icon.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    @Override
    public void setTint(int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        tintList = tint;
        lastKnownDarkMode = null;
        updateIcon();
        if (icon != null) {
            icon.setTintList(tint);
        }
        invalidateSelf();
    }

    @Override
    public void setTintMode(@Nullable PorterDuff.Mode tintMode) {
        this.tintMode = tintMode;
        if (icon != null) {
            icon.setTintMode(tintMode);
        }
        invalidateSelf();
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
    public boolean isStateful() {
        return (tintList != null && tintList.isStateful()) || (icon != null && icon.isStateful());
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        lastKnownDarkMode = null;
        updateIcon();
        boolean changed = icon != null && icon.setState(state);
        if (changed) {
            invalidateSelf();
        }
        return changed;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        if (icon != null) {
            icon.setBounds(bounds);
        }
    }
}
