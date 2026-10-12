package app.revanced.extension.kakaotalk.patches;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.helper.ResourceHelper;

@SuppressWarnings({"unused", "deprecation"})
public final class CustomProfileImagePatch {
    private static final String FRAGMENT_TAG = "morphe_custom_profile_image";
    private static final String IMAGE_PATH = "image_path";
    private static final String IMAGE_ZOOM = "image_zoom";
    private static final String IMAGE_OFFSET_X = "image_offset_x";
    private static final String IMAGE_OFFSET_Y = "image_offset_y";
    private static final String PHOTO_ONLY = "photo_only";
    private static final String TEMP_IMAGE_PREFIX = "custom-profile-";
    private static final long STALE_IMAGE_AGE_MILLIS = 24L * 60 * 60 * 1000;
    private static final int PICK_IMAGE = 1;
    private static final int MENU_IMAGE = 0x6d700001;

    private CustomProfileImagePatch() {
    }

    public static void initialize(Activity activity) {
        if (activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            activity.getFragmentManager().beginTransaction()
                    .add(new ImageFragment(), FRAGMENT_TAG).commit();
        }
    }

    public static void addMenu(Activity activity, Menu menu) {
        if (menu.findItem(MENU_IMAGE) != null) return;
        menu.add(Menu.NONE, MENU_IMAGE, Menu.NONE, str("morphe_kakaotalk_custom_profile_image"))
                .setIcon(android.R.drawable.ic_menu_gallery)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(item -> {
                    ImageFragment fragment = (ImageFragment) activity.getFragmentManager()
                            .findFragmentByTag(FRAGMENT_TAG);
                    if (fragment != null) fragment.showOptions();
                    return true;
                });
    }

    public static void setBackgroundColor(View view, int color) {
        if (view.getBackground() instanceof CustomProfileImageView.Background background) {
            background.setOriginal(new ColorDrawable(color));
        } else {
            view.setBackgroundColor(color);
        }
    }

    private static void cleanUpStaleImages(Activity activity, String currentPath) {
        long cutoff = System.currentTimeMillis() - STALE_IMAGE_AGE_MILLIS;
        for (File directory : new File[]{activity.getCacheDir(), activity.getFilesDir()}) {
            File[] files = directory.listFiles((parent, name) ->
                    name.startsWith(TEMP_IMAGE_PREFIX) && name.endsWith(".png"));
            if (files == null) continue;
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoff
                        && !file.getAbsolutePath().equals(currentPath)) {
                    file.delete();
                }
            }
        }
    }

    public static final class ImageFragment extends Fragment {
        private String imagePath;
        private View canvas;
        private View text;
        private View sticker;
        private View saveButton;
        private Bitmap bitmap;
        private float zoom = 1;
        private float offsetX;
        private float offsetY;
        private float textAlpha = 1;
        private float stickerAlpha = 1;
        private boolean textFocusable;
        private boolean textFocusableInTouchMode;
        private boolean photoOnly = true;
        private boolean processingImage;
        private boolean saveButtonEnabled;
        private int generation;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Activity activity = getActivity();
            canvas = activity.findViewById(ResourceHelper.getResourceId("id", "canvas_layout"));
            text = activity.findViewById(ResourceHelper.getResourceId("id", "edit_profile_name"));
            sticker = activity.findViewById(ResourceHelper.getResourceId("id", "sticker_view"));
            saveButton = activity.findViewById(ResourceHelper.getResourceId("id", "btn_profile_download"));
            if (text != null) {
                textFocusable = text.isFocusable();
                textFocusableInTouchMode = text.isFocusableInTouchMode();
            }
            if (savedInstanceState != null) {
                imagePath = savedInstanceState.getString(IMAGE_PATH);
                zoom = savedInstanceState.getFloat(IMAGE_ZOOM, 1);
                offsetX = savedInstanceState.getFloat(IMAGE_OFFSET_X);
                offsetY = savedInstanceState.getFloat(IMAGE_OFFSET_Y);
                photoOnly = savedInstanceState.getBoolean(PHOTO_ONLY, true);
                if (imagePath != null) restoreImage(imagePath);
            }
            cleanUpStaleImages(activity, imagePath);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(IMAGE_PATH, imagePath);
            outState.putFloat(IMAGE_ZOOM, zoom);
            outState.putFloat(IMAGE_OFFSET_X, offsetX);
            outState.putFloat(IMAGE_OFFSET_Y, offsetY);
            outState.putBoolean(PHOTO_ONLY, photoOnly);
        }

        @Override
        public void onDestroyView() {
            generation++;
            setProcessingImage(false);
            canvas = null;
            text = null;
            sticker = null;
            saveButton = null;
            bitmap = null;
            super.onDestroyView();
        }

        private void setProcessingImage(boolean processing) {
            if (processingImage == processing) return;
            processingImage = processing;
            if (saveButton == null) return;
            if (processing) {
                saveButtonEnabled = saveButton.isEnabled();
                saveButton.setEnabled(false);
            } else {
                saveButton.setEnabled(saveButtonEnabled);
            }
            Activity activity = getActivity();
            if (activity != null) activity.invalidateOptionsMenu();
        }

        @Override
        public void onDestroy() {
            Activity activity = getActivity();
            if (activity != null && activity.isFinishing() && imagePath != null) {
                new File(imagePath).delete();
            }
            super.onDestroy();
        }

        private void showOptions() {
            if (canvas == null) return;
            if (bitmap == null) {
                selectImage();
                return;
            }
            String overlayOption = str(photoOnly
                    ? "morphe_kakaotalk_custom_profile_image_show_overlay"
                    : "morphe_kakaotalk_custom_profile_image_hide_overlay");
            new AlertDialog.Builder(getActivity())
                    .setTitle(str("morphe_kakaotalk_custom_profile_image"))
                    .setItems(new String[]{str("morphe_kakaotalk_custom_profile_image_select"),
                            str("morphe_kakaotalk_custom_profile_image_adjust"), overlayOption,
                            str("morphe_kakaotalk_custom_profile_image_remove")}, (dialog, which) -> {
                        switch (which) {
                            case 0 -> selectImage();
                            case 1 -> adjustImage(bitmap, null, zoom, offsetX, offsetY);
                            case 2 -> {
                                photoOnly = !photoOnly;
                                updateOverlay();
                            }
                            case 3 -> removeImage();
                        }
                    }).show();
        }

        private void selectImage() {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 33) {
                intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }
            intent.setType("image/*");
            try {
                startActivityForResult(intent, PICK_IMAGE);
            } catch (Exception exception) {
                if (Build.VERSION.SDK_INT < 33) {
                    showError(exception);
                    return;
                }
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.addCategory(Intent.CATEGORY_OPENABLE);
                fallback.setType("image/*");
                try {
                    startActivityForResult(fallback, PICK_IMAGE);
                } catch (Exception fallbackError) {
                    showError(fallbackError);
                }
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode != PICK_IMAGE || resultCode != Activity.RESULT_OK || data == null) return;
            Uri uri = data.getData();
            if (uri == null) {
                showError(new IllegalStateException("The photo picker returned no image"));
                return;
            }
            int requestGeneration = ++generation;
            Activity activity = getActivity();
            setProcessingImage(true);
            Utils.runOnBackgroundThread(() -> {
                File copy = null;
                Bitmap selected = null;
                try {
                    selected = decode(ImageDecoder.createSource(activity.getContentResolver(), uri));
                    copy = File.createTempFile(TEMP_IMAGE_PREFIX, ".png", activity.getCacheDir());
                    try (FileOutputStream stream = new FileOutputStream(copy)) {
                        if (!selected.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            throw new IllegalStateException("Could not save the selected photo");
                        }
                    }
                    File selectedFile = copy;
                    Bitmap selectedBitmap = selected;
                    activity.runOnUiThread(() -> {
                        if (!isAdded() || canvas == null || requestGeneration != generation) {
                            selectedFile.delete();
                            selectedBitmap.recycle();
                            return;
                        }
                        adjustImage(selectedBitmap, selectedFile, 1, 0, 0);
                        setProcessingImage(false);
                    });
                } catch (Exception | OutOfMemoryError exception) {
                    if (copy != null) copy.delete();
                    if (selected != null) selected.recycle();
                    activity.runOnUiThread(() -> {
                        if (isAdded() && requestGeneration == generation) {
                            setProcessingImage(false);
                            showError(exception);
                        }
                    });
                }
            });
        }

        private static Bitmap decode(ImageDecoder.Source source) throws Exception {
            return ImageDecoder.decodeBitmap(source, (decoder, info, ignored) -> {
                int largest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                decoder.setTargetSampleSize(Math.max(1, (largest + 2047) / 2048));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }

        private void restoreImage(String path) {
            int requestGeneration = ++generation;
            Activity activity = getActivity();
            setProcessingImage(true);
            Utils.runOnBackgroundThread(() -> {
                try {
                    Bitmap restored = decode(ImageDecoder.createSource(new File(path)));
                    activity.runOnUiThread(() -> {
                        if (!isAdded() || canvas == null || requestGeneration != generation) {
                            restored.recycle();
                            return;
                        }
                        bitmap = restored;
                        applyImage();
                        setProcessingImage(false);
                    });
                } catch (Exception | OutOfMemoryError exception) {
                    activity.runOnUiThread(() -> {
                        if (isAdded() && requestGeneration == generation) {
                            imagePath = null;
                            setProcessingImage(false);
                            showError(exception);
                        }
                    });
                }
            });
        }

        private void adjustImage(Bitmap selected, File candidate, float initialZoom,
                                 float initialOffsetX, float initialOffsetY) {
            CustomProfileImageView preview = new CustomProfileImageView(getActivity(), selected,
                    initialZoom, initialOffsetX, initialOffsetY);
            preview.setContentDescription(str("morphe_kakaotalk_custom_profile_image_adjust_hint"));
            new AlertDialog.Builder(getActivity())
                    .setTitle(str("morphe_kakaotalk_custom_profile_image_adjust"))
                    .setMessage(str("morphe_kakaotalk_custom_profile_image_adjust_hint"))
                    .setView(preview)
                    .setPositiveButton(str("morphe_kakaotalk_custom_profile_image_apply"), (dialog, which) -> {
                        String oldPath = imagePath;
                        if (candidate != null) imagePath = candidate.getAbsolutePath();
                        bitmap = selected;
                        zoom = preview.zoom();
                        offsetX = preview.offsetX();
                        offsetY = preview.offsetY();
                        applyImage();
                        if (candidate != null && oldPath != null && !oldPath.equals(imagePath)) {
                            new File(oldPath).delete();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> discard(candidate, selected))
                    .setOnCancelListener(dialog -> discard(candidate, selected))
                    .show();
        }

        private void discard(File candidate, Bitmap selected) {
            if (candidate == null) return;
            candidate.delete();
            selected.recycle();
        }

        private void applyImage() {
            if (canvas == null || bitmap == null) return;
            Drawable original = canvas.getBackground();
            if (original instanceof CustomProfileImageView.Background background) {
                original = background.original();
            }
            canvas.setBackground(new CustomProfileImageView.Background(
                    original, bitmap, zoom, offsetX, offsetY));
            updateOverlay();
        }

        private void updateOverlay() {
            if (text == null || sticker == null) return;
            if (photoOnly && bitmap != null) {
                if (text.getAlpha() != 0) textAlpha = text.getAlpha();
                if (sticker.getAlpha() != 0) stickerAlpha = sticker.getAlpha();
                text.clearFocus();
                text.setFocusable(false);
                text.setAlpha(0);
                sticker.setAlpha(0);
                InputMethodManager keyboard = (InputMethodManager)
                        getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(text.getWindowToken(), 0);
            } else {
                text.setFocusable(textFocusable);
                text.setFocusableInTouchMode(textFocusableInTouchMode);
                text.setAlpha(textAlpha);
                sticker.setAlpha(stickerAlpha);
            }
        }

        private void removeImage() {
            generation++;
            setProcessingImage(false);
            if (canvas.getBackground() instanceof CustomProfileImageView.Background background) {
                canvas.setBackground(background.original());
            }
            String previousPath = imagePath;
            imagePath = null;
            bitmap = null;
            zoom = 1;
            offsetX = 0;
            offsetY = 0;
            updateOverlay();
            if (previousPath != null) new File(previousPath).delete();
        }

        private void showError(Throwable exception) {
            Logger.printException(() -> "Could not load a custom profile image", exception);
            if (isAdded()) {
                Toast.makeText(getActivity(), str("morphe_kakaotalk_custom_profile_image_error"), Toast.LENGTH_LONG).show();
            }
        }
    }
}