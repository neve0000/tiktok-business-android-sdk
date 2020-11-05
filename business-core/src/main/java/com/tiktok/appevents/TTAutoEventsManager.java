package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTKeyValueStore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static com.tiktok.util.TTConst.*;

class TTAutoEventsManager {

    private static final String TAG = TTAutoEventsManager.class.getCanonicalName();

    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat timeFormat;

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
    }

    private final TTAppEventLogger appEventLogger;
    private TTKeyValueStore store;

    public TTAutoEventsManager(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
        store = new TTKeyValueStore(TikTokBusinessSdk.getApplicationContext());
    }

    private boolean shouldTrackAppLifecycleEvents(String event) {
        return appEventLogger.lifecycleTrackEnable
                && !appEventLogger.disabledEvents.contains(event);
    }

    /**
     * the events to be tracked when the app was just activated
     * 1. firstInstall
     * 2. 2DRetention
     * 3. launchApp
     */
    public void trackOnAppOpenEvents() {
        trackFirstInstallEvent();
        track2DayRetentionEvent();
        trackLaunchEvent();
    }

    private void trackFirstInstallEvent() {
        /* get install trigger time, set only on InstallApp trigger */
        String installTime = store.get(TTSDK_APP_FIRST_INSTALL);
        if (installTime != null) return;

        Date now = new Date();
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(TTSDK_APP_FIRST_INSTALL, timeFormat.format(now));

        /* check and track InstallApp. */
        if (shouldTrackAppLifecycleEvents("InstallApp")) {
            appEventLogger.track("InstallApp", null);
        }

        store.set(hm);
    }

    /**
     * 2DRetention should be called at 2 places
     * 1. when the app is opened
     * 2. when the user switches to the background, and then switch back after some while,
     * since most users click "home" button rather than kill the process most of the time.
     */
    void track2DayRetentionEvent() {
        String is2DayLogged = store.get(TTSDK_APP_2DR_TIME);
        if (is2DayLogged != null) return;

        String firstInstall = store.get(TTSDK_APP_FIRST_INSTALL);
        if (firstInstall == null) return;// should not happen

        try {
            Date firstLaunchTime = timeFormat.parse(firstInstall);
            Date now = new Date();
            if (shouldTrackAppLifecycleEvents("2DRetention")
                    && isSatisfyRetention(firstLaunchTime, now)) {
                appEventLogger.track("2DRetention", null);
                store.set(TTSDK_APP_2DR_TIME, timeFormat.format(now));
            }
        } catch (ParseException ignored) {
        }
    }

    private void trackLaunchEvent() {
        if (shouldTrackAppLifecycleEvents("LaunchApp")) {
            appEventLogger.track("LaunchApp", null);
            store.set(TTSDK_APP_LAST_LAUNCH, timeFormat.format(new Date()));
        }
    }

    // extract into a single method to simplify writing unit test
    private boolean isSatisfyRetention(Date firstLaunch, Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(firstLaunch);
        c.add(Calendar.DATE, 1);
        String nextDayFromFirst = dateFormat.format(c.getTime());
        String todayDate = dateFormat.format(now);
        return nextDayFromFirst.equals(todayDate);
    }

}
