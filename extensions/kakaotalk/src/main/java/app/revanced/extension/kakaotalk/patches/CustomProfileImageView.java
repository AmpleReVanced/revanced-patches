package app.revanced.extension.kakaotalk.patches;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

final class CustomProfileImageView extends View {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private float zoom;
    private float offsetX;
    private float offsetY;
    private float lastX;
    private float lastY;

    CustomProfileImageView(Activity activity, Bitmap bitmap, float zoom, float offsetX, float offsetY) {
        super(activity);
        this.bitmap = bitmap;
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        scaleDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleAt(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });
    }

    float zoom() {
        return zoom;
    }

    float offsetX() {
        return offsetX;
    }

    float offsetY() {
        return offsetY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = getResources().getDisplayMetrics().widthPixels
                - (int) (48 * getResources().getDisplayMetrics().density);
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? maxWidth : Math.min(MeasureSpec.getSize(widthMeasureSpec), maxWidth);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPhoto(canvas, bitmap, new Rect(0, 0, getWidth(), getHeight()),
                zoom, offsetX, offsetY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                lastX = event.getX();
                lastY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    pan(event.getX() - lastX, event.getY() - lastY);
                    lastX = event.getX();
                    lastY = event.getY();
                }
            }
            case MotionEvent.ACTION_POINTER_UP -> {
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                lastX = event.getX(remaining);
                lastY = event.getY(remaining);
            }
        }
        return true;
    }

    private float halfOverflowX(float targetZoom) {
        float scale = Math.max((float) getWidth() / bitmap.getWidth(),
                (float) getHeight() / bitmap.getHeight());
        return Math.max(0, (bitmap.getWidth() * scale * targetZoom - getWidth()) / 2);
    }

    private float halfOverflowY(float targetZoom) {
        float scale = Math.max((float) getWidth() / bitmap.getWidth(),
                (float) getHeight() / bitmap.getHeight());
        return Math.max(0, (bitmap.getHeight() * scale * targetZoom - getHeight()) / 2);
    }

    private static float clamp(float value) {
        return Math.max(-1, Math.min(1, value));
    }

    private void pan(float deltaX, float deltaY) {
        float limitX = halfOverflowX(zoom);
        float limitY = halfOverflowY(zoom);
        if (limitX > 0) offsetX = clamp(offsetX + deltaX / limitX);
        if (limitY > 0) offsetY = clamp(offsetY + deltaY / limitY);
        invalidate();
    }

    private void scaleAt(float factor, float focusX, float focusY) {
        float nextZoom = Math.max(1, Math.min(5, zoom * factor));
        float ratio = nextZoom / zoom;
        float oldX = offsetX * halfOverflowX(zoom);
        float oldY = offsetY * halfOverflowY(zoom);
        float relativeFocusX = focusX - getWidth() / 2f;
        float relativeFocusY = focusY - getHeight() / 2f;
        float limitX = halfOverflowX(nextZoom);
        float limitY = halfOverflowY(nextZoom);
        offsetX = limitX == 0 ? 0
                : clamp((relativeFocusX + (oldX - relativeFocusX) * ratio) / limitX);
        offsetY = limitY == 0 ? 0
                : clamp((relativeFocusY + (oldY - relativeFocusY) * ratio) / limitY);
        zoom = nextZoom;
        invalidate();
    }

    static void drawPhoto(Canvas canvas, Bitmap bitmap, Rect bounds, float zoom,
                          float offsetX, float offsetY, Paint paint) {
        if (bounds.isEmpty() || bitmap.isRecycled()) return;
        float baseScale = Math.max((float) bounds.width() / bitmap.getWidth(),
                (float) bounds.height() / bitmap.getHeight());
        float width = bitmap.getWidth() * baseScale * zoom;
        float height = bitmap.getHeight() * baseScale * zoom;
        float centerX = bounds.exactCenterX() + offsetX * Math.max(0, (width - bounds.width()) / 2);
        float centerY = bounds.exactCenterY() + offsetY * Math.max(0, (height - bounds.height()) / 2);
        int saveCount = canvas.save();
        canvas.clipRect(bounds);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, null,
                new RectF(centerX - width / 2, centerY - height / 2,
                        centerX + width / 2, centerY + height / 2), paint);
        canvas.restoreToCount(saveCount);
    }

    static final class Background extends Drawable {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Bitmap bitmap;
        private final float zoom;
        private final float offsetX;
        private final float offsetY;
        private Drawable original;

        Background(Drawable original, Bitmap bitmap, float zoom, float offsetX, float offsetY) {
            this.original = original;
            this.bitmap = bitmap;
            this.zoom = zoom;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        Drawable original() {
            return original;
        }

        void setOriginal(Drawable original) {
            this.original = original;
        }

        @Override
        public void draw(Canvas canvas) {
            drawPhoto(canvas, bitmap, getBounds(), zoom, offsetX, offsetY, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}