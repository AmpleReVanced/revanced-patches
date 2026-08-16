package app.revanced.extension.samsungkeyboard;

import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

public final class FeedbackCompat {
    private static volatile boolean tickPrimitiveSupported;

    private FeedbackCompat() {
    }

    public static float semGetSituationVolume(AudioManager manager, int situation, int device) {
        return SettingsStore.getFeedbackSoundVolume() / 100.0f;
    }

    public static int semGetSupportedVibrationType(Vibrator vibrator) {
        boolean supported = vibrator.hasVibrator();
        tickPrimitiveSupported = supported && vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK
        );
        return supported ? 2 : 0;
    }

    public static int semGetVibrationIndex(int index) {
        return index;
    }

    public static VibrationEffect semCreateWaveform(int index, int repeat, Object magnitudeType) {
        return createVibrationEffect();
    }

    public static void previewVibration(Context context) {
        if (Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
        ) == 0) return;

        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null || semGetSupportedVibrationType(vibrator) == 0) return;
        vibrator.vibrate(createVibrationEffect());
    }

    private static VibrationEffect createVibrationEffect() {
        int strength = Math.max(1, SettingsStore.getFeedbackVibrationStrength());
        if (tickPrimitiveSupported) {
            return VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, strength / 100.0f)
                    .compose();
        }

        int amplitude = Math.max(1, Math.round(strength * 255.0f / 100.0f));
        return VibrationEffect.createOneShot(10, amplitude);
    }
}