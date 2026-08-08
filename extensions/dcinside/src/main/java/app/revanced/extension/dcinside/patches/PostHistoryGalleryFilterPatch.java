package app.revanced.extension.dcinside.patches;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.dcinside.helper.ResourceHelper;

@SuppressWarnings("unused")
public final class PostHistoryGalleryFilterPatch {
    /**
     * Implemented by the recently viewed posts activity during patching.
     */
    public interface Host {
        void patch_reloadPostHistory();

        /**
         * Reads every stored record, regardless of how far the list has been paged.
         */
        void patch_collectGalleries();
    }

    private static final String FILTER_VIEW_ID = "morphe_post_history_gallery_filter";
    private static final String LIKE_FILTER_VIEW_ID = "post_history_like_title";

    /**
     * Only ever touched on the main thread, as the app runs every history query there
     * to keep the realm records on the thread that opened them.
     */
    private static final Map<String, Gallery> galleries = new LinkedHashMap<>();

    @Nullable
    private static String selectedGalleryId;
    @Nullable
    private static String selectedGalleryName;
    private static boolean collecting;

    private PostHistoryGalleryFilterPatch() {
    }

    public static void initialize(Host host) {
        Activity activity = (Activity) host;

        TextView view = activity.findViewById(ResourceHelper.getResourceId("id", FILTER_VIEW_ID));
        if (view == null) {
            return;
        }

        TextView likeFilter = activity.findViewById(
                ResourceHelper.getResourceId("id", LIKE_FILTER_VIEW_ID));
        if (likeFilter != null && likeFilter.getTextColors() != null) {
            view.setTextColor(likeFilter.getTextColors());
        }

        view.setOnClickListener(clicked -> showGalleryPicker(activity, host, view));
        updateFilterView(view);
    }

    /**
     * Called with every stored record, so that the picker lists galleries and counts of the
     * whole history instead of only the pages the list has loaded so far.
     */
    public static void collectGalleries(List<?> records) {
        galleries.clear();
        if (records == null) {
            return;
        }

        for (Object record : records) {
            String galleryId = getGalleryId(record);
            if (galleryId == null) {
                continue;
            }

            Gallery gallery = galleries.get(galleryId);
            if (gallery == null) {
                galleries.put(galleryId, new Gallery(galleryId, getGalleryName(record)));
            } else {
                gallery.count++;
                gallery.nameIfUnknown(getGalleryName(record));
            }
        }
    }

    /**
     * Read by every recently viewed posts query, so that the displayed list and the query
     * the delete path re-runs can never disagree on what the filter selected.
     */
    @Nullable
    public static String[] activeGalleryFilter() {
        String selected = selectedGalleryId;
        return collecting || selected == null ? null : new String[] { selected };
    }

    private static void showGalleryPicker(Activity activity, Host host, TextView filterView) {
        collecting = true;
        try {
            host.patch_collectGalleries();
        } finally {
            collecting = false;
        }

        List<Gallery> options = new ArrayList<>(galleries.values());
        Collections.sort(options, (first, second) -> {
            if (first.count != second.count) {
                return second.count - first.count;
            }
            return first.label().compareTo(second.label());
        });

        CharSequence[] labels = new CharSequence[options.size() + 1];
        labels[0] = string(activity, "morphe_post_history_gallery_filter_all", "All");
        for (int index = 0; index < options.size(); index++) {
            Gallery gallery = options.get(index);
            labels[index + 1] = gallery.label() + " (" + gallery.count + ")";
        }

        AlertDialog dialog = themedDialogBuilder(activity)
                .setTitle(string(activity, "morphe_post_history_gallery_filter_title", "Filter by gallery"))
                .setAdapter(itemAdapter(activity, labels), (picker, which) -> {
                    if (which == 0) {
                        selectedGalleryId = null;
                        selectedGalleryName = null;
                    } else {
                        Gallery gallery = options.get(which - 1);
                        selectedGalleryId = gallery.id;
                        selectedGalleryName = gallery.label();
                    }

                    updateFilterView(filterView);
                    host.patch_reloadPostHistory();
                })
                .create();

        Utils.showDialog(activity, dialog);
    }

    private static void updateFilterView(TextView view) {
        boolean selected = selectedGalleryId != null;
        String label = selected
                ? selectedGalleryName
                : string(view.getContext(), "morphe_post_history_gallery_filter", "Gallery");
        view.setText(label + " ▾");
        view.setSelected(selected);
    }

    /**
     * The support library dialog is obfuscated in the app, so the framework dialog is
     * built with the dialog theme the app styles its own dialogs with.
     */
    private static AlertDialog.Builder themedDialogBuilder(Activity activity) {
        int themeAttribute = ResourceHelper.getResourceId("attr", "alertDialogTheme");
        if (themeAttribute != 0) {
            TypedValue theme = new TypedValue();
            if (activity.getTheme().resolveAttribute(themeAttribute, theme, true)
                    && theme.resourceId != 0) {
                return new AlertDialog.Builder(activity, theme.resourceId);
            }
        }

        return new AlertDialog.Builder(activity);
    }

    /**
     * The dialog theme leaves the item text unreadable in dark mode, so the items are
     * drawn with the text color of the screen behind the dialog.
     */
    private static ListAdapter itemAdapter(Activity activity, CharSequence[] labels) {
        TypedArray attributes = activity.getTheme()
                .obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary });
        ColorStateList textColor;
        try {
            textColor = attributes.getColorStateList(0);
        } finally {
            attributes.recycle();
        }

        return new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (textColor != null && view instanceof TextView) {
                    ((TextView) view).setTextColor(textColor);
                }
                return view;
            }
        };
    }

    private static String string(Context context, String name, String fallback) {
        int stringId = ResourceHelper.getResourceId("string", name);
        return stringId == 0 ? fallback : context.getString(stringId);
    }

    @Nullable
    private static String getGalleryId(Object record) {
        return null;  // Modified during patching.
    }

    @Nullable
    private static String getGalleryName(Object record) {
        return null;  // Modified during patching.
    }

    private static final class Gallery {
        final String id;
        @Nullable
        String name;
        int count = 1;

        Gallery(String id, @Nullable String name) {
            this.id = id;
            this.name = name;
        }

        void nameIfUnknown(@Nullable String candidate) {
            if (isBlank(name)) {
                name = candidate;
            }
        }

        String label() {
            return isBlank(name) ? id : name.trim();
        }

        private static boolean isBlank(@Nullable String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
