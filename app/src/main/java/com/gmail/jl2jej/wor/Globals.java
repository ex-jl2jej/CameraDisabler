package com.gmail.jl2jej.wor;

import android.app.Application;
import android.app.admin.DevicePolicyManager;

/**
 * Created by kido on 2017/07/10.
 */

public class Globals extends Application {
    DevicePolicyManager dpm;

    public void GlabalsAllInit() {
        dpm = null;
    }
}
