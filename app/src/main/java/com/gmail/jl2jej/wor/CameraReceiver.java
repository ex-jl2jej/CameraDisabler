package com.gmail.jl2jej.wor;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by kido on 2017/06/05.
 * 必要なもので、特に実施することはない
 */

public class CameraReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        //
        Log.i("CameraReceiver", "enable");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        //
        Log.i("CameraReceiver", "disable");
    }
}
