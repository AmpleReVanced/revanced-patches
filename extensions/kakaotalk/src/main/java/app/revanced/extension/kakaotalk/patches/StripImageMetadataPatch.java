package app.revanced.extension.kakaotalk.patches;

import androidx.exifinterface.media.ExifInterface;

import app.morphe.extension.shared.Utils;
import app.revanced.extension.kakaotalk.settings.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unused")
public final class StripImageMetadataPatch {
    private StripImageMetadataPatch() {
    }

    // Every EXIF tag that identifies the device, lens, capture time, settings or location. The
    // orientation tag is deliberately left in place so the picture is not shown rotated.
    private static final String[] STRIP_TAGS = {
            // Device / author identity
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT,
            ExifInterface.TAG_IMAGE_UNIQUE_ID,
            ExifInterface.TAG_CAMERA_OWNER_NAME,
            ExifInterface.TAG_BODY_SERIAL_NUMBER,
            ExifInterface.TAG_LENS_MAKE,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_LENS_SERIAL_NUMBER,
            ExifInterface.TAG_LENS_SPECIFICATION,
            // Capture time
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            // Capture settings
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_MAX_APERTURE_VALUE,
            ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_EXPOSURE_MODE,
            ExifInterface.TAG_METERING_MODE,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_SCENE_CAPTURE_TYPE,
            ExifInterface.TAG_SCENE_TYPE,
            ExifInterface.TAG_SENSING_METHOD,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
            ExifInterface.TAG_BRIGHTNESS_VALUE,
            ExifInterface.TAG_LIGHT_SOURCE,
            ExifInterface.TAG_GAIN_CONTROL,
            ExifInterface.TAG_CONTRAST,
            ExifInterface.TAG_SATURATION,
            ExifInterface.TAG_SHARPNESS,
            ExifInterface.TAG_SUBJECT_DISTANCE,
            ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
            ExifInterface.TAG_SUBJECT_AREA,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_SENSITIVITY_TYPE,
            // Location
            ExifInterface.TAG_GPS_VERSION_ID,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_AREA_INFORMATION,
            ExifInterface.TAG_GPS_DOP,
            ExifInterface.TAG_GPS_SPEED,
            ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_TRACK,
            ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_IMG_DIRECTION,
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_MAP_DATUM,
            ExifInterface.TAG_GPS_DEST_LATITUDE,
            ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LONGITUDE,
            ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
            ExifInterface.TAG_GPS_DEST_BEARING,
            ExifInterface.TAG_GPS_DEST_BEARING_REF,
            ExifInterface.TAG_GPS_DEST_DISTANCE,
            ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
            ExifInterface.TAG_GPS_STATUS,
            ExifInterface.TAG_GPS_MEASURE_MODE,
            ExifInterface.TAG_GPS_SATELLITES,
            ExifInterface.TAG_GPS_DIFFERENTIAL,
    };

    /**
     * Returns a copy of the image with its identifying EXIF metadata removed via AndroidX
     * {@link ExifInterface} (only the orientation is kept), or the original file when stripping is
     * disabled, unnecessary, or unsupported for the format. The source file is never modified.
     */
    public static File sanitize(File file) {
        try {
            if (!Settings.stripImageMetadata() || file == null || !file.isFile()) {
                return file;
            }

            if (!hasMetadata(new ExifInterface(file.getAbsolutePath()))) {
                return file; // re-encoded temp files, screenshots without EXIF, ...
            }

            File output = cacheFile(file);
            copy(file, output);

            ExifInterface exif = new ExifInterface(output.getAbsolutePath());
            for (String tag : STRIP_TAGS) {
                exif.setAttribute(tag, null);
            }
            exif.saveAttributes(); // rewrites only the metadata; pixels and orientation stay intact

            return output;
        } catch (Throwable ignored) {
            return file; // fail open: a failed strip must never block a send
        }
    }

    private static boolean hasMetadata(ExifInterface exif) {
        return exif.getAttribute(ExifInterface.TAG_MAKE) != null
                || exif.getAttribute(ExifInterface.TAG_MODEL) != null
                || exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null
                || exif.getAttribute(ExifInterface.TAG_DATETIME) != null
                || exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null
                || exif.getAttribute(ExifInterface.TAG_SOFTWARE) != null;
    }

    private static File cacheFile(File source) {
        return new File(Utils.getContext().getCacheDir(),
                "morphe_stripped_" + System.nanoTime() + "_" + source.getName());
    }

    private static void copy(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * @return If this patch was included during patching.
     */
    public static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }
}