package app.revanced.extension.samsungkeyboard;

import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;

public final class FeedbackCompat {
    private static volatile boolean clickPrimitiveSupported;
    private static volatile boolean thudPrimitiveSupported;
    private static volatile boolean tickPrimitiveSupported;

    private FeedbackCompat() {
    }

    public static float semGetSituationVolume(AudioManager manager, int situation, int device) {
        float volume = SettingsStore.getFeedbackSoundVolume() / 100.0f;
        try {
            float situationVolume = Float.parseFloat(manager.getParameters(
                    "g_volume_situation_key;type=" + situation + ";device=" + device
            ));
            if (Float.isFinite(situationVolume) && situationVolume >= 0.0f && situationVolume <= 1.0f) {
                return volume * situationVolume;
            }
        } catch (RuntimeException ignored) {
        }
        return volume;
    }

    public static int semGetSupportedVibrationType(Vibrator vibrator) {
        if (!vibrator.hasVibrator()) {
            clickPrimitiveSupported = false;
            thudPrimitiveSupported = false;
            tickPrimitiveSupported = false;
            return 0;
        }

        boolean[] supported = vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_TICK
        );
        clickPrimitiveSupported = supported[0];
        thudPrimitiveSupported = supported[1];
        tickPrimitiveSupported = supported[2];
        return 2;
    }

    public static int semGetVibrationIndex(int index) {
        return switch (index) {
            case 5, 0x6c -> HapticFeedbackConstants.LONG_PRESS;
            case 0x10 -> HapticFeedbackConstants.CONFIRM;
            case 0x26 -> HapticFeedbackConstants.CONTEXT_CLICK;
            case 0x29, 0x6e -> HapticFeedbackConstants.CLOCK_TICK;
            default -> HapticFeedbackConstants.VIRTUAL_KEY;
        };
    }

    public static VibrationEffect semCreateWaveform(int index, int repeat, Object magnitudeType) {
        return createVibrationEffect(index);
    }

    public static void previewVibration(Context context) {
        if (Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
        ) == 0) return;

        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null || semGetSupportedVibrationType(vibrator) == 0) return;
        vibrator.vibrate(createVibrationEffect(HapticFeedbackConstants.VIRTUAL_KEY));
    }

    private static VibrationEffect createVibrationEffect(int index) {
        int strength = Math.max(1, SettingsStore.getFeedbackVibrationStrength());
        int primitive = switch (index) {
            case HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.REJECT ->
                    VibrationEffect.Composition.PRIMITIVE_THUD;
            case HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.CONTEXT_CLICK ->
                    VibrationEffect.Composition.PRIMITIVE_CLICK;
            default -> VibrationEffect.Composition.PRIMITIVE_TICK;
        };
        boolean primitiveSupported = switch (primitive) {
            case VibrationEffect.Composition.PRIMITIVE_CLICK -> clickPrimitiveSupported;
            case VibrationEffect.Composition.PRIMITIVE_THUD -> thudPrimitiveSupported;
            default -> tickPrimitiveSupported;
        };
        if (primitiveSupported) {
            return VibrationEffect.startComposition()
                    .addPrimitive(primitive, strength / 100.0f)
                    .compose();
        }

        int amplitude = Math.max(1, Math.round(strength * 255.0f / 100.0f));
        int duration = switch (primitive) {
            case VibrationEffect.Composition.PRIMITIVE_CLICK -> 15;
            case VibrationEffect.Composition.PRIMITIVE_THUD -> 20;
            default -> 10;
        };
        return VibrationEffect.createOneShot(duration, amplitude);
    }
}