package com.gmail.jl2jej.wor;

import android.app.Application;

/**
 * Created by kido on 2017/08/05.
 */

public class CameraDisablerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new CameraDisablerLifecycleHandler());
    }
}
