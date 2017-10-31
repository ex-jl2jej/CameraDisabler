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

    private void logOutput(String mes) {
        Log.i(TAG, "====================" + mes + "========================");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        logOutput("created");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        logOutput("destroyed");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        logOutput("resumed");
    }

    @Override
    public void onActivityPaused(Activity activity ) {
        ++paused;
        logOutput("paused");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        logOutput("saveInstanceState");
    }

    public void onActivityRestoreInstanceState(Activity activity, Bundle outState) {
        logOutput("resotreInstanceState");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        logOutput("started");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        logOutput("stopped");
    }

    public static boolean isApplicationVisible(){
        return started > stopped;
    }

    public static boolean isApplicationForeground() {
        return resumed > paused;
    }
}
