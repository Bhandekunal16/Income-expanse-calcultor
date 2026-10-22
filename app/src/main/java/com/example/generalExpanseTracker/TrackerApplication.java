package com.example.generalExpanseTracker;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.WindowManager;

public class TrackerApplication extends Application implements Application.ActivityLifecycleCallbacks {
    static final long RELOCK_TIMEOUT_MS = 5 * 60 * 1000L;

    private int startedActivityCount;
    private long backgroundedAt;
    private boolean hasBeenBackgrounded;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (startedActivityCount == 0
                && hasBeenBackgrounded
                && SystemClock.elapsedRealtime() - backgroundedAt >= RELOCK_TIMEOUT_MS
                && SessionManager.hasStoredSession(this)
                && SessionManager.isUnlocked()
                && !(activity instanceof LoginActivity)) {
            SessionManager.lock();
            if (!(activity instanceof AppLockActivity)) {
                Intent lockIntent = new Intent(activity, AppLockActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(lockIntent);
                activity.finish();
            }
        }
        startedActivityCount++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivityCount = Math.max(0, startedActivityCount - 1);
        if (startedActivityCount == 0) {
            backgroundedAt = SystemClock.elapsedRealtime();
            hasBeenBackgrounded = true;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
