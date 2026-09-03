package app.revanced.extension.kakaomap.liveupdate;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import app.morphe.extension.shared.Utils;

import com.kakao.map.route.pubtrans.model.PubtransStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
public final class LiveUpdatePatch {
    private static final String EXTRA_CONTAINS_CUSTOM_VIEW = "android.contains.customView";
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";
    private static final int MAX_PROGRESS_POINTS = 4;
    private static final int MAX_PROGRESS_SEGMENTS = 10;
    private static final int PROGRESS_MAX = 1000;
    private static final long STALE_LOCATION_THRESHOLD_MILLIS = 120_000;
    private static final ThreadLocal<JourneyDetails> JOURNEY_DETAILS = new ThreadLocal<>();
    private static final ThreadLocal<JourneyProgress> NOTIFICATION_PROGRESS = new ThreadLocal<>();
    private static volatile int activeJourneyToken;
    private static volatile JourneyProgress latestJourneyProgress;
    private static volatile long lastLocationUpdateElapsedRealtime;
    private static volatile double sectionDistanceRatio;

    private LiveUpdatePatch() {
    }

    public static void captureText(int viewId, CharSequence text) {
        JourneyDetails details = JOURNEY_DETAILS.get();
        if (details == null) {
            details = new JourneyDetails();
            JOURNEY_DETAILS.set(details);
        }
        details.put(viewId, text);
    }

    public static void beginJourney(List<?> steps) {
        lastLocationUpdateElapsedRealtime = SystemClock.elapsedRealtime();
        sectionDistanceRatio = 0;
        activeJourneyToken = System.identityHashCode(steps);
        JourneyProgress progress = JourneyProgress.create(steps, 0, 0, 0, 0, true);
        latestJourneyProgress = progress;
        NOTIFICATION_PROGRESS.set(progress);
    }

    public static void captureProgress(
            List<?> steps,
            int parentIndex,
            int childIndex,
            int state
    ) {
        JourneyProgress progress = JourneyProgress.create(
                steps,
                parentIndex,
                childIndex,
                state,
                sectionDistanceRatio,
                false
        );
        if (progress == null) {
            NOTIFICATION_PROGRESS.remove();
            return;
        }
        synchronized (LiveUpdatePatch.class) {
            if (activeJourneyToken == 0) {
                activeJourneyToken = progress.journeyToken;
            } else if (activeJourneyToken != progress.journeyToken) {
                NOTIFICATION_PROGRESS.remove();
                return;
            }

            JourneyProgress previous = latestJourneyProgress;
            if (previous != null && previous.journeyToken == progress.journeyToken &&
                    (previous.starting || previous.completed) &&
                    !isTravelStep(steps, parentIndex)) {
                NOTIFICATION_PROGRESS.set(previous);
                return;
            }
            if (previous != null && previous.journeyToken == progress.journeyToken &&
                    !previous.starting && !previous.completed &&
                    progress.progress < previous.progress) {
                progress = progress.withProgress(previous.progress);
            }
            if (previous == null || previous.parentIndex != progress.parentIndex ||
                    previous.currentChildIndex != progress.currentChildIndex ||
                    previous.eventState != progress.eventState ||
                    previous.progress != progress.progress) {
                lastLocationUpdateElapsedRealtime = SystemClock.elapsedRealtime();
            }
            latestJourneyProgress = progress;
            NOTIFICATION_PROGRESS.set(progress);
        }
    }

    private static boolean isTravelStep(List<?> steps, int index) {
        if (index < 0 || index >= steps.size()) {
            return false;
        }
        Object value = steps.get(index);
        if (!(value instanceof PubtransStep)) {
            return false;
        }
        try {
            String type = ((PubtransStep) value).getType();
            return "WALKING".equals(type) || "BUS".equals(type) || "SUBWAY".equals(type);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void completeJourney() {
        JourneyProgress progress = latestJourneyProgress;
        if (progress != null) {
            JourneyProgress completed = progress.complete();
            latestJourneyProgress = completed;
            NOTIFICATION_PROGRESS.set(completed);
        }
    }

    public static void markLocationUpdated() {
        JourneyProgress progress = latestJourneyProgress;
        if (progress == null || progress.starting || "SUBWAY".equals(progress.currentType)) {
            lastLocationUpdateElapsedRealtime = SystemClock.elapsedRealtime();
        }
    }

    public static void markLocationMatched(boolean matched) {
        if (matched) {
            lastLocationUpdateElapsedRealtime = SystemClock.elapsedRealtime();
        }
    }

    public static void captureSectionDistanceRatio(double ratio) {
        if (!Double.isNaN(ratio) && !Double.isInfinite(ratio)) {
            sectionDistanceRatio = Math.max(0, Math.min(ratio, 1));
        }
    }

    public static Notification promote(Notification notification) {
        JourneyDetails details = JOURNEY_DETAILS.get();
        JOURNEY_DETAILS.remove();
        JourneyProgress progress = NOTIFICATION_PROGRESS.get();
        NOTIFICATION_PROGRESS.remove();

        if (notification == null
                || Build.VERSION.SDK_INT < 36
                || (notification.flags & Notification.FLAG_ONGOING_EVENT) == 0) {
            return notification;
        }

        try {
            return Api36Impl.promote(Utils.getContext(), notification, details, progress);
        } catch (Throwable ignored) {
            return notification;
        }
    }

    private static final class JourneySegment {
        final int colorResource;
        final int length;

        JourneySegment(int length, int colorResource) {
            this.length = length;
            this.colorResource = colorResource;
        }
    }

    private static final class StepProgress {
        final int childCount;
        final int colorResource;
        final int iconResource;
        final int originalIndex;
        final CharSequence title;
        final boolean transfer;
        final String type;
        final int weight;

        StepProgress(PubtransStep step, int originalIndex, String type) {
            this.originalIndex = originalIndex;
            int resolvedChildCount = 0;
            int accurateTime = 0;
            int totalTime = 0;
            int resolvedColor = -1;
            int resolvedIcon = -1;
            CharSequence resolvedTitle = null;
            boolean resolvedTransfer = false;
            try {
                List<?> stops = step.getStops();
                resolvedChildCount = stops == null ? 0 : stops.size();
            } catch (Throwable ignored) {
            }
            try {
                accurateTime = step.getAccurateTotalTime();
                totalTime = step.getTotalTime();
            } catch (Throwable ignored) {
            }
            try {
                resolvedColor = step.getSymbolColor();
            } catch (Throwable ignored) {
            }
            try {
                resolvedIcon = step.getIcon();
            } catch (Throwable ignored) {
            }
            try {
                resolvedTitle = step.getTitle();
            } catch (Throwable ignored) {
            }
            try {
                resolvedTransfer = step.getTransfer();
            } catch (Throwable ignored) {
            }
            this.type = type;
            childCount = resolvedChildCount;
            weight = Math.max(accurateTime > 0 ? accurateTime : totalTime, Math.max(childCount - 1, 1));
            colorResource = resolvedColor;
            iconResource = resolvedIcon;
            title = resolvedTitle;
            transfer = resolvedTransfer;
        }

        StepProgress(
                String type,
                int originalIndex,
                int childCount,
                int colorResource,
                int iconResource,
                CharSequence title,
                boolean transfer
        ) {
            this.type = type;
            this.originalIndex = originalIndex;
            this.childCount = childCount;
            this.colorResource = colorResource;
            this.iconResource = iconResource;
            this.title = title;
            this.transfer = transfer;
            weight = 1;
        }

    }

    private static final class JourneyProgress {
        final boolean completed;
        final int currentChildIndex;
        final int currentChildCount;
        final int currentColorResource;
        final int currentIconResource;
        final CharSequence currentTitle;
        final boolean currentTransfer;
        final String currentType;
        final int eventState;
        final int journeyToken;
        final int parentIndex;
        final int progress;
        final List<JourneySegment> segments;
        final boolean starting;

        private JourneyProgress(
                List<JourneySegment> segments,
                int progress,
                StepProgress current,
                int journeyToken,
                int parentIndex,
                int childIndex,
                int state,
                boolean starting,
                boolean completed
        ) {
            this.segments = segments;
            this.progress = progress;
            currentType = current == null ? null : current.type;
            currentTitle = current == null ? null : current.title;
            currentColorResource = current == null ? -1 : current.colorResource;
            currentIconResource = current == null ? -1 : current.iconResource;
            currentTransfer = current != null && current.transfer;
            currentChildCount = current == null ? 0 : current.childCount;
            currentChildIndex = childIndex;
            eventState = state;
            this.journeyToken = journeyToken;
            this.parentIndex = parentIndex;
            this.starting = starting;
            this.completed = completed;
        }

        static JourneyProgress create(
                List<?> steps,
                int parentIndex,
                int childIndex,
                int state,
                double distanceRatio,
                boolean starting
        ) {
            if (steps == null || steps.isEmpty()) {
                return null;
            }

            try {
                List<StepProgress> travelSteps = new ArrayList<>();
                for (int index = 0; index < steps.size(); index++) {
                    Object value = steps.get(index);
                    if (!(value instanceof PubtransStep)) {
                        continue;
                    }
                    PubtransStep sourceStep = (PubtransStep) value;
                    String type;
                    try {
                        type = sourceStep.getType();
                    } catch (Throwable ignored) {
                        continue;
                    }
                    if ("WALKING".equals(type) || "BUS".equals(type) || "SUBWAY".equals(type)) {
                        travelSteps.add(new StepProgress(sourceStep, index, type));
                    }
                }
                if (travelSteps.isEmpty()) {
                    return null;
                }

                StepProgress current = travelSteps.get(0);
                long completedWeight = 0;
                long totalWeight = 0;
                for (StepProgress step : travelSteps) {
                    totalWeight += step.weight;
                    if (!starting && step.originalIndex < parentIndex) {
                        completedWeight += step.weight;
                    }
                    if (!starting && step.originalIndex <= parentIndex) {
                        current = step;
                    }
                    if (!starting && step.originalIndex == parentIndex) {
                        current = step;
                    }
                }

                if (!starting && current.originalIndex == parentIndex) {
                    completedWeight += Math.round(current.weight * stageFraction(
                            current.childCount,
                            childIndex,
                            state,
                            current.type,
                            distanceRatio
                    ));
                }
                int progress = starting || totalWeight <= 0
                        ? 0
                        : (int) Math.max(0, Math.min(
                                Math.round((completedWeight * (double) PROGRESS_MAX) / totalWeight),
                                PROGRESS_MAX
                        ));
                return new JourneyProgress(
                        createSegments(travelSteps),
                        progress,
                        current,
                        System.identityHashCode(steps),
                        starting ? current.originalIndex : parentIndex,
                        childIndex,
                        state,
                        starting,
                        false
                );
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static List<JourneySegment> createSegments(List<StepProgress> steps) {
            int segmentCount = Math.min(steps.size(), MAX_PROGRESS_SEGMENTS);
            long[] weights = new long[segmentCount];
            int[] dominantWeights = new int[segmentCount];
            int[] colors = new int[segmentCount];
            for (int index = 0; index < steps.size(); index++) {
                int bucket = (index * segmentCount) / steps.size();
                StepProgress step = steps.get(index);
                weights[bucket] += step.weight;
                if (colors[bucket] <= 0 || step.weight > dominantWeights[bucket]) {
                    colors[bucket] = step.colorResource;
                    dominantWeights[bucket] = step.weight;
                }
            }

            long remainingWeight = 0;
            for (long weight : weights) {
                remainingWeight += weight;
            }
            int remainingLength = PROGRESS_MAX;
            List<JourneySegment> segments = new ArrayList<>(segmentCount);
            for (int index = 0; index < segmentCount; index++) {
                int remainingSegments = segmentCount - index;
                int length = index == segmentCount - 1
                        ? remainingLength
                        : Math.max(1, Math.min(
                                (int) Math.round((weights[index] * (double) remainingLength) /
                                        remainingWeight),
                                remainingLength - remainingSegments + 1
                        ));
                segments.add(new JourneySegment(length, colors[index]));
                remainingLength -= length;
                remainingWeight -= weights[index];
            }
            return segments;
        }

        private static double stageFraction(
                int childCount,
                int childIndex,
                int state,
                String type,
                double distanceRatio
        ) {
            if (childCount > 1) {
                int position = Math.max(0, Math.min(childIndex, childCount - 1));
                double withinSection = "SUBWAY".equals(type) ? 0 : distanceRatio;
                if (state == 1) {
                    withinSection = 1;
                } else if (state == 2) {
                    withinSection = Math.max(withinSection, 0.75);
                }
                return Math.min((position + withinSection) / (childCount - 1), 1);
            }
            if (!"SUBWAY".equals(type) && distanceRatio > 0) {
                return distanceRatio;
            }
            if (state == 1) {
                return 1;
            }
            return state == 2 ? 0.85 : 0;
        }

        JourneyProgress complete() {
            return new JourneyProgress(
                    segments,
                    PROGRESS_MAX,
                    null,
                    journeyToken,
                    parentIndex,
                    0,
                    0,
                    false,
                    true
            );
        }

        JourneyProgress withProgress(int progress) {
            return new JourneyProgress(
                    segments,
                    progress,
                    new StepProgress(
                            currentType,
                            parentIndex,
                            currentChildCount,
                            currentColorResource,
                            currentIconResource,
                            currentTitle,
                            currentTransfer
                    ),
                    journeyToken,
                    parentIndex,
                    currentChildIndex,
                    eventState,
                    starting,
                    completed
            );
        }
    }

    private static final class JourneyDetails {
        private final Map<Integer, CharSequence> textByViewId = new HashMap<>();

        void put(int viewId, CharSequence text) {
            String value = text == null ? null : text.toString().trim();
            if (TextUtils.isEmpty(value)) {
                textByViewId.remove(viewId);
            } else {
                textByViewId.put(viewId, value);
            }
        }

        ResolvedJourneyDetails resolve(Context context) {
            CharSequence title = join(" ", value(context, "title"), value(context, "title_type"));
            CharSequence busTime = value(context, "bus_timeinfo");
            CharSequence subwayArrival = value(context, "subway_arrival1");
            List<CharSequence> lines = new ArrayList<>();

            addLine(lines, join(" · ", value(context, "bus_info"), busTime));
            addLine(lines, join(
                    " · ",
                    value(context, "subway_info1"),
                    subwayArrival
            ));
            addLine(lines, join(
                    " · ",
                    value(context, "subway_info2"),
                    value(context, "subway_arrival2")
            ));
            addLine(lines, value(context, "etc_info"));
            addLine(lines, value(context, "etc_info2"));

            CharSequence criticalText = !TextUtils.isEmpty(busTime) ? busTime : subwayArrival;
            return new ResolvedJourneyDetails(title, join("\n", lines), criticalText);
        }

        private CharSequence value(Context context, String resourceName) {
            int identifier = context.getResources().getIdentifier(
                    resourceName,
                    "id",
                    context.getPackageName()
            );
            return identifier == 0 ? null : textByViewId.get(identifier);
        }
    }

    private static final class ResolvedJourneyDetails {
        final CharSequence title;
        final CharSequence text;
        final CharSequence criticalText;

        ResolvedJourneyDetails(CharSequence title, CharSequence text, CharSequence criticalText) {
            this.title = title;
            this.text = text;
            this.criticalText = criticalText;
        }

        boolean isEmpty() {
            return TextUtils.isEmpty(title) && TextUtils.isEmpty(text);
        }
    }

    @RequiresApi(36)
    private static final class Api36Impl {
        private Api36Impl() {
        }

        static Notification promote(
                Context context,
                Notification notification,
                JourneyDetails capturedDetails,
                JourneyProgress capturedProgress
        ) {
            Bundle sourceExtras = notification.extras == null ? Bundle.EMPTY : notification.extras;
            CharSequence title = sourceExtras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = sourceExtras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence subText = sourceExtras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            boolean hasCustomViews = notification.contentView != null
                    || notification.bigContentView != null
                    || notification.headsUpContentView != null;
            ResolvedJourneyDetails details = capturedDetails == null
                    ? null
                    : capturedDetails.resolve(context);
            boolean hasJourneyDetails = details != null && !details.isEmpty();
            boolean locationStale = hasJourneyDetails && capturedProgress != null &&
                    !capturedProgress.starting && !capturedProgress.completed &&
                    lastLocationUpdateElapsedRealtime > 0 &&
                    SystemClock.elapsedRealtime() - lastLocationUpdateElapsedRealtime >=
                            STALE_LOCATION_THRESHOLD_MILLIS;

            if (hasJourneyDetails) {
                if (!TextUtils.isEmpty(details.title)) {
                    title = details.title;
                }
                if (!TextUtils.isEmpty(details.text)) {
                    text = details.text;
                }
            }
            if (hasJourneyDetails && capturedProgress != null && capturedProgress.starting) {
                CharSequence nextStep = join(
                        " · ",
                        modeLabel(context, capturedProgress.currentType),
                        capturedProgress.currentTitle
                );
                text = join(
                        "\n",
                        stringResource(context, "alarm_tracking_message"),
                        nextStep,
                        text
                );
            } else if (locationStale) {
                text = join("\n", text, stringResource(context, "alarm_tracking_message"));
            }

            CharSequence status = locationStale
                    ? stringResource(context, "alarm_tracking_title")
                    : phaseLabel(context, capturedProgress);
            if (!TextUtils.isEmpty(status)) {
                subText = join(" · ", status, subText);
            }

            if (TextUtils.isEmpty(title)) {
                title = navigationTitle(context);
            }
            if (TextUtils.isEmpty(text) && !TextUtils.isEmpty(subText)) {
                text = subText;
            }

            Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification)
                    .setCustomContentView(null)
                    .setCustomBigContentView(null)
                    .setCustomHeadsUpContentView(null)
                    .setGroupSummary(false)
                    .setColorized(false)
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_NAVIGATION)
                    .setContentTitle(title);

            Integer currentColor = hasJourneyDetails && capturedProgress != null
                    ? resolveColor(context, capturedProgress.currentColorResource)
                    : null;
            if (currentColor != null) {
                builder.setColor(currentColor);
            }

            if (hasCustomViews) {
                builder.setStyle(progressStyle(
                        context,
                        notification,
                        hasJourneyDetails ? capturedProgress : null
                ));
            }
            if (notification.contentIntent == null) {
                Intent launchIntent = context.getPackageManager()
                        .getLaunchIntentForPackage(context.getPackageName());
                if (launchIntent != null) {
                    builder.setContentIntent(PendingIntent.getActivity(
                            context,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    ));
                }
            }
            if (!TextUtils.isEmpty(text)) {
                builder.setContentText(text);
            }
            if (!TextUtils.isEmpty(subText)) {
                builder.setSubText(subText);
            }

            CharSequence criticalText = details == null || locationStale
                    ? null
                    : details.criticalText;
            if (TextUtils.isEmpty(criticalText)) {
                criticalText = locationStale ? null : sourceExtras.getCharSequence(
                        Notification.EXTRA_SUB_TEXT
                );
            }
            if (!TextUtils.isEmpty(criticalText) && criticalText.length() <= 7) {
                builder.setShortCriticalText(criticalText.toString());
            }
            if (hasJourneyDetails) {
                replaceEndGuidanceAction(context, notification, builder);
            }

            builder.getExtras().remove(EXTRA_CONTAINS_CUSTOM_VIEW);
            builder.getExtras().putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true);
            Notification notificationWithoutColorization = builder.build();
            if (notificationWithoutColorization.hasPromotableCharacteristics()) {
                return notificationWithoutColorization;
            }

            builder.setColorized(true);
            Notification notificationWithColorization = builder.build();
            return notificationWithColorization.hasPromotableCharacteristics()
                    ? notificationWithColorization
                    : notificationWithoutColorization;
        }

        private static Notification.ProgressStyle progressStyle(
                Context context,
                Notification notification,
                JourneyProgress progress
        ) {
            Notification.ProgressStyle style = new Notification.ProgressStyle();
            if (progress == null) {
                return style.setProgressIndeterminate(true);
            }

            for (JourneySegment segment : progress.segments) {
                Notification.ProgressStyle.Segment styledSegment =
                        new Notification.ProgressStyle.Segment(segment.length);
                Integer segmentColor = resolveColor(context, segment.colorResource);
                if (segmentColor != null) {
                    styledSegment.setColor(segmentColor);
                }
                style.addProgressSegment(styledSegment);
            }

            int segmentCount = progress.segments.size();
            int pointCount = Math.min(segmentCount - 1, MAX_PROGRESS_POINTS);
            for (int index = 1; index <= pointCount; index++) {
                int boundary = Math.max(1, Math.min(
                        Math.round((index * segmentCount) / (float) (pointCount + 1)),
                        segmentCount - 1
                ));
                int position = 0;
                for (int segmentIndex = 0; segmentIndex < boundary; segmentIndex++) {
                    position += progress.segments.get(segmentIndex).length;
                }
                style.addProgressPoint(new Notification.ProgressStyle.Point(position));
            }

            Icon trackerIcon = trackerIcon(context, progress);
            if (trackerIcon == null) {
                trackerIcon = notification.getSmallIcon();
            }
            if (trackerIcon != null) {
                style.setProgressTrackerIcon(trackerIcon);
            }
            return style
                    .setProgress(progress.progress)
                    .setStyledByProgress(false);
        }

        private static Integer resolveColor(Context context, int resource) {
            if (resource <= 0) {
                return null;
            }
            try {
                return context.getColor(resource);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static CharSequence phaseLabel(Context context, JourneyProgress progress) {
            if (progress == null) {
                return null;
            }
            if (progress.completed) {
                return stringResource(context, "arrival");
            }
            if (progress.starting) {
                return stringResource(context, "alarm_tracking_title");
            }
            if (progress.currentTransfer) {
                return stringResource(context, "transfer");
            }

            boolean finalStop = progress.currentChildCount > 1 &&
                    (progress.currentChildIndex >= progress.currentChildCount - 1 ||
                            progress.eventState == 2 &&
                                    progress.currentChildIndex >= progress.currentChildCount - 2);
            if ("BUS".equals(progress.currentType)) {
                if (finalStop) {
                    return stringResource(context, "alarm_bus_get_off");
                }
                CharSequence phase = progress.currentChildIndex == 0 && progress.eventState == 0
                        ? stringResource(context, "alarm_bus_get_on")
                        : stringResource(context, "alarm_bus_moving");
                return TextUtils.isEmpty(phase) ? modeLabel(context, progress.currentType) : phase;
            }
            if ("SUBWAY".equals(progress.currentType) && finalStop) {
                return stringResource(context, "alarm_subway_get_off");
            }
            return modeLabel(context, progress.currentType);
        }

        private static CharSequence modeLabel(Context context, String type) {
            if ("BUS".equals(type)) {
                return stringResource(context, "bus");
            }
            if ("SUBWAY".equals(type)) {
                return stringResource(context, "subway");
            }
            if ("WALKING".equals(type)) {
                return stringResource(context, "trans_walk");
            }
            return null;
        }

        private static Icon trackerIcon(Context context, JourneyProgress progress) {
            int identifier = progress.currentIconResource;
            if (identifier <= 0) {
                String resourceName = "BUS".equals(progress.currentType)
                        ? "icon_bus_solid"
                        : "SUBWAY".equals(progress.currentType)
                        ? "icon_metro_solid"
                        : "WALKING".equals(progress.currentType)
                        ? "icon_walk_solid"
                        : null;
                if (resourceName != null) {
                    identifier = context.getResources().getIdentifier(
                            resourceName,
                            "drawable",
                            context.getPackageName()
                    );
                }
            }
            if (identifier <= 0) {
                return null;
            }
            try {
                return Icon.createWithResource(context, identifier);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static void replaceEndGuidanceAction(
                Context context,
                Notification notification,
                Notification.Builder builder
        ) {
            Notification.Action[] sourceActions = notification.actions;
            if (sourceActions == null || sourceActions.length == 0) {
                return;
            }

            CharSequence alarmLabel = stringResource(context, "pubtrans_alarm_terminate_alarm");
            CharSequence guidanceLabel = stringResource(context, "urban_route_end_guidance");
            if (TextUtils.isEmpty(alarmLabel) || TextUtils.isEmpty(guidanceLabel)) {
                return;
            }

            Notification.Action[] actions = sourceActions.clone();
            boolean changed = false;
            for (int index = 0; index < actions.length; index++) {
                Notification.Action action = actions[index];
                if (action == null || !TextUtils.equals(action.title, alarmLabel)) {
                    continue;
                }

                Notification.Action replacement = action.clone();
                replacement.title = guidanceLabel;
                actions[index] = replacement;
                changed = true;
            }
            if (changed) {
                builder.setActions(actions);
            }
        }

        private static CharSequence stringResource(Context context, String resourceName) {
            int identifier = context.getResources().getIdentifier(
                    resourceName,
                    "string",
                    context.getPackageName()
            );
            return identifier == 0 ? null : context.getText(identifier);
        }

        private static CharSequence navigationTitle(Context context) {
            int identifier = context.getResources().getIdentifier(
                    "navigation_notification_title",
                    "string",
                    context.getPackageName()
            );
            if (identifier != 0) {
                return context.getString(identifier);
            }
            return context.getApplicationInfo().loadLabel(context.getPackageManager());
        }
    }

    private static void addLine(List<CharSequence> lines, CharSequence line) {
        if (!TextUtils.isEmpty(line)) {
            lines.add(line);
        }
    }

    private static CharSequence join(String separator, CharSequence... parts) {
        List<CharSequence> values = new ArrayList<>();
        for (CharSequence part : parts) {
            if (!TextUtils.isEmpty(part)) {
                values.add(part);
            }
        }
        return join(separator, values);
    }

    private static CharSequence join(String separator, List<CharSequence> parts) {
        if (parts.isEmpty()) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        for (CharSequence part : parts) {
            if (value.length() > 0) {
                value.append(separator);
            }
            value.append(part);
        }
        return value;
    }
}