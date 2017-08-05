package com.gmail.jl2jej.wor;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by kido on 2017/08/05.
 * アプリ状態取得用
 */

public class CameraDisablerLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "LifecycleHandler";
    private static int resumed = 0;
    private static int paused = 0;
    private static int started = 0;
    private static int stopped = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        Log.i(TAG, "resumed");
    }

    @Override
    public void onActivityPaused(Activity activity ) {
        ++paused;
        Log.i(TAG, "paused");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        Log.i(TAG, "started");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        Log.i(TAG, "stopped");
    }

    public static boolean isApplicationVisible(){
        return started > stopped;
    }

    public static boolean isApplicationForeground() {
        return resumed > paused;
    }
}
