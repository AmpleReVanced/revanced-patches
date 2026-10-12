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
import com.kakao.map.route.pubtrans.model.PubtransNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings({"unused", "deprecation"})
public final class LiveUpdatePatch {
    private static final String EXTRA_CONTAINS_CUSTOM_VIEW = "android.contains.customView";
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";
    private static final int MAX_PROGRESS_POINTS = 4;
    private static final int MAX_PROGRESS_SEGMENTS = 10;
    private static final int PROGRESS_MAX = 1000;
    private static final long STALE_LOCATION_THRESHOLD_MILLIS = 120_000;
    private static final Map<Object, JourneyDetails> DETAILS_BY_WRAPPER = new WeakHashMap<>();
    private static final ThreadLocal<JourneyDetails> JOURNEY_DETAILS = new ThreadLocal<>();
    private static final ThreadLocal<JourneyProgress> NOTIFICATION_PROGRESS = new ThreadLocal<>();
    private static volatile int activeJourneyToken;
    private static volatile JourneyProgress latestJourneyProgress;
    private static volatile long lastLocationUpdateElapsedRealtime;
    private static volatile double sectionDistanceRatio;

    private LiveUpdatePatch() {
    }

    public static void captureText(Object wrapper, int viewId, CharSequence text) {
        synchronized (DETAILS_BY_WRAPPER) {
            detailsFor(wrapper).put(viewId, text);
        }
    }

    public static void captureContentIntent(Object wrapper, int viewId, PendingIntent intent) {
        Context context = Utils.getContext();
        if (context != null && viewId == context.getResources().getIdentifier(
                "wrap_layout", "id", context.getPackageName())) {
            synchronized (DETAILS_BY_WRAPPER) {
                detailsFor(wrapper).contentIntent = intent;
            }
        }
    }

    public static void captureNavigationActions(
            Object wrapper,
            Context context,
            int parentIndex,
            int childIndex,
            Integer state,
            Integer previousViewId,
            Integer nextViewId
    ) {
        synchronized (DETAILS_BY_WRAPPER) {
            JourneyDetails details = detailsFor(wrapper);
            details.previousAction = previousViewId == null ? null : navigationAction(
                    context, "NOTIFICATION_ACTION_PREV", parentIndex, childIndex, state);
            details.nextAction = nextViewId == null ? null : navigationAction(
                    context, "NOTIFICATION_ACTION_NEXT", parentIndex, childIndex, state);
        }
    }

    private static PendingIntent navigationAction(
            Context context, String action, int parentIndex, int childIndex, Integer state
    ) {
        Intent intent = new Intent(action).setPackage(context.getPackageName())
                .putExtra("CURRENT_PARENT", parentIndex)
                .putExtra("CURRENT_CHILD", childIndex);
        if (state != null) {
            intent.putExtra("CURRENT_STATE", state);
        }
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static JourneyDetails detailsFor(Object wrapper) {
        JourneyDetails details = DETAILS_BY_WRAPPER.get(wrapper);
        if (details == null) {
            details = new JourneyDetails();
            DETAILS_BY_WRAPPER.put(wrapper, details);
        }
        return details;
    }

    public static void beginJourney(List<?> steps) {
        sectionDistanceRatio = 0;
        lastLocationUpdateElapsedRealtime = SystemClock.elapsedRealtime();
        activeJourneyToken = System.identityHashCode(steps);
        JourneyProgress progress = JourneyProgress.create(steps, 0, 0, 0, 0, true);
        latestJourneyProgress = progress;
        NOTIFICATION_PROGRESS.set(progress);
    }

    public static void resetJourney() {
        synchronized (LiveUpdatePatch.class) {
            activeJourneyToken = 0;
            latestJourneyProgress = null;
            sectionDistanceRatio = 0;
            lastLocationUpdateElapsedRealtime = 0;
        }
        synchronized (DETAILS_BY_WRAPPER) {
            DETAILS_BY_WRAPPER.clear();
        }
        discardNotification();
    }

    public static void discardNotification() {
        JOURNEY_DETAILS.remove();
        NOTIFICATION_PROGRESS.remove();
    }

    public static void captureProgress(
            List<?> steps,
            int parentIndex,
            int childIndex,
            int state,
            Object wrapper
    ) {
        synchronized (DETAILS_BY_WRAPPER) {
            JOURNEY_DETAILS.set(detailsFor(wrapper));
        }
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
        if (steps == null || index < 0 || index >= steps.size()) {
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
        JourneyDetails details;
        synchronized (DETAILS_BY_WRAPPER) {
            JourneyDetails captured = JOURNEY_DETAILS.get();
            details = captured == null ? null : new JourneyDetails(captured);
        }
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
        final List<String> stopNames = new ArrayList<>();

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
                if (stops != null) {
                    for (Object stop : stops) {
                        stopNames.add(stop instanceof PubtransNode ? ((PubtransNode) stop).name : null);
                    }
                }
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

        String stopName(int index) {
            return index < 0 || index >= stopNames.size() ? null : stopNames.get(index);
        }
    }

    private static final class JourneyProgress {
        final boolean completed;
        final StepProgress current;
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
            this.current = current;
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
                double completedWeight = 0;
                long totalWeight = 0;
                for (StepProgress step : travelSteps) {
                    totalWeight += step.weight;
                    if (!starting && step.originalIndex < parentIndex) {
                        completedWeight += step.weight;
                    }
                    if (!starting && step.originalIndex <= parentIndex) {
                        current = step;
                    }
                }

                if (!starting && current.originalIndex == parentIndex) {
                    completedWeight += current.weight * stageFraction(
                            current.childCount,
                            childIndex,
                            state,
                            current.type,
                            distanceRatio
                    );
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
                if ("WALKING".equals(type) && state == 1) {
                    withinSection = 1;
                } else if (state == 0 && !"WALKING".equals(type)) {
                    withinSection = 0;
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

        boolean isTransit() {
            return "BUS".equals(currentType) || "SUBWAY".equals(currentType);
        }

        boolean isBoarding() {
            return isTransit() && currentChildIndex == 0 && eventState == 0;
        }

        int remainingStops() {
            return !isTransit() || currentChildCount < 2 ||
                    currentChildIndex < 0 || currentChildIndex >= currentChildCount ? -1
                    : Math.max(0, currentChildCount - 1 - currentChildIndex);
        }

        boolean isGettingOff() {
            int remaining = remainingStops();
            return remaining == 0 || remaining == 1 && eventState == 2;
        }
    }

    private static final class JourneyDetails {
        private final Map<Integer, CharSequence> textByViewId = new HashMap<>();
        PendingIntent contentIntent;
        PendingIntent previousAction;
        PendingIntent nextAction;

        JourneyDetails() {
        }

        JourneyDetails(JourneyDetails source) {
            textByViewId.putAll(source.textByViewId);
            contentIntent = source.contentIntent;
            previousAction = source.previousAction;
            nextAction = source.nextAction;
        }

        void put(int viewId, CharSequence text) {
            String value = text == null ? null : text.toString().trim();
            if (TextUtils.isEmpty(value)) {
                textByViewId.remove(viewId);
            } else {
                textByViewId.put(viewId, value);
            }
        }

        ResolvedJourneyDetails resolve(Context context, JourneyProgress progress) {
            CharSequence title = join(" ", value(context, "title"), value(context, "title_type"));
            List<CharSequence> lines = new ArrayList<>();
            if (progress != null && !progress.starting && !progress.completed && progress.isTransit()) {
                if (!progress.isBoarding() && !progress.isGettingOff() && progress.remainingStops() >= 0) {
                    addLine(lines, Api36Impl.stringResource(context,
                            "SUBWAY".equals(progress.currentType)
                                    ? "revanced_live_update_remaining_stations"
                                    : "revanced_live_update_remaining_stops",
                            progress.remainingStops()));
                }
                if (progress.current != null && !progress.isGettingOff()) {
                    String destination = progress.current.stopName(progress.currentChildCount - 1);
                    if (!TextUtils.isEmpty(destination)) {
                        addLine(lines, Api36Impl.stringResource(context,
                                "SUBWAY".equals(progress.currentType)
                                        ? "alarm_subway_last_station" : "alarm_bus_last_busstop",
                                destination));
                    }
                    if (!progress.isBoarding() && progress.eventState == 0) {
                        String nextStop = progress.current.stopName(progress.currentChildIndex + 1);
                        if (!TextUtils.isEmpty(nextStop)) {
                            addLine(lines, Api36Impl.stringResource(context,
                                    "revanced_live_update_next_stop", nextStop));
                        }
                    }
                }
                if (progress.isBoarding()) {
                    addLine(lines, value(context, "bus_info"));
                    addLine(lines, value(context, "subway_info1"));
                    addLine(lines, value(context, "subway_info2"));
                    addLine(lines, value(context, "etc_info"));
                } else if (progress.isGettingOff()) {
                    addLine(lines, value(context, "etc_info"));
                    addLine(lines, value(context, "etc_info2"));
                }
            } else {
                addLine(lines, value(context, "etc_info"));
                addLine(lines, value(context, "etc_info2"));
            }
            return new ResolvedJourneyDetails(title, join("\n", lines));
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
        ResolvedJourneyDetails(CharSequence title, CharSequence text) {
            this.title = title;
            this.text = text;
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
                    : capturedDetails.resolve(context, capturedProgress);
            boolean hasJourneyDetails = details != null && !details.isEmpty();
            boolean locationStale = hasJourneyDetails && capturedProgress != null &&
                    !capturedProgress.starting && !capturedProgress.completed && !capturedProgress.isBoarding() &&
                    lastLocationUpdateElapsedRealtime > 0 &&
                    SystemClock.elapsedRealtime() - lastLocationUpdateElapsedRealtime >=
                            STALE_LOCATION_THRESHOLD_MILLIS;

            if (hasJourneyDetails) {
                if (!TextUtils.isEmpty(details.title)) {
                    title = details.title;
                }
                text = details.text;
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
                subText = join(" · ", capturedProgress == null ? status : capturedProgress.currentTitle,
                        status, subText);
            }

            if (TextUtils.isEmpty(title)) {
                title = navigationTitle(context);
            }
            if (TextUtils.isEmpty(text) && !TextUtils.isEmpty(subText)) {
                text = subText;
            }

            Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification.clone())
                    .setCustomContentView(null)
                    .setCustomBigContentView(null)
                    .setCustomHeadsUpContentView(null)
                    .setGroupSummary(false)
                    .setColorized(false)
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_NAVIGATION)
                    .setContentTitle(title)
                    .setContentText(text);

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
            if (capturedDetails != null && capturedDetails.contentIntent != null) {
                builder.setContentIntent(capturedDetails.contentIntent);
            } else if (notification.contentIntent == null) {
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

            CharSequence criticalText = hasJourneyDetails
                    ? criticalText(context, capturedProgress, locationStale)
                    : sourceExtras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (!TextUtils.isEmpty(criticalText) && criticalText.length() <= 7) {
                builder.setShortCriticalText(criticalText.toString());
            }
            if (hasJourneyDetails) {
                replaceEndGuidanceAction(context, notification, builder);
                addNavigationActions(context, builder, capturedDetails);
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

        private static CharSequence criticalText(Context context, JourneyProgress progress, boolean stale) {
            if (stale || progress != null && progress.starting) {
                return stringResource(context, "revanced_live_update_locating");
            }
            if (progress != null && !progress.completed && progress.isTransit() &&
                    !progress.isBoarding() && !progress.isGettingOff() && progress.remainingStops() >= 0) {
                String text = stringResource(context, "revanced_live_update_remaining_chip", progress.remainingStops());
                return text != null && text.length() <= 7 ? text : Integer.toString(progress.remainingStops());
            }
            return phaseLabel(context, progress);
        }

        private static CharSequence phaseLabel(Context context, JourneyProgress progress) {
            if (progress == null) {
                return null;
            }
            if (progress.completed) {
                return stringResource(context, "arrival");
            }
            if (progress.starting) {
                return stringResource(context, "revanced_live_update_locating");
            }
            if (progress.isBoarding()) {
                return stringResource(context, progress.currentTransfer
                        ? "transfer" : "revanced_live_update_boarding");
            }
            if (progress.isGettingOff()) {
                return stringResource(context, "revanced_live_update_get_off");
            }
            if (progress.isTransit()) {
                return stringResource(context, "revanced_live_update_moving");
            }
            return modeLabel(context, progress.currentType);
        }

        private static void addNavigationActions(Context context, Notification.Builder builder, JourneyDetails details) {
            if (details.previousAction != null) {
                builder.addAction(new Notification.Action.Builder((Icon) null,
                        stringResource(context, "revanced_live_update_previous"), details.previousAction).build());
            }
            if (details.nextAction != null) {
                builder.addAction(new Notification.Action.Builder((Icon) null,
                        stringResource(context, "next"), details.nextAction).build());
            }
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

        private static String stringResource(Context context, String resourceName, Object... arguments) {
            int identifier = context.getResources().getIdentifier(
                    resourceName,
                    "string",
                    context.getPackageName()
            );
            return identifier == 0 ? null : context.getString(identifier, arguments);
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