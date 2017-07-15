package com.gmail.jl2jej.wor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import java.util.Calendar;

/**
 * Created by kido on 2017/07/07.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    public static final String CAMERA_DISABLE = "CAMERA_DISABLE";
    public static final String RCODE = "RCODE";
    protected DevicePolicyManager devicePolicyManager;
    protected ComponentName tCameraReceiver;

    @Override
    public void onReceive(Context context, Intent intent) {
        devicePolicyManager = (DevicePolicyManager)context.getSystemService(context.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(context, CameraReceiver.class);
        Globals g = new Globals(context);
        Boolean cameraDisable = intent.getBooleanExtra(CAMERA_DISABLE, true);
        int requestCode = intent.getIntExtra(RCODE, 1);

        devicePolicyManager.setCameraDisabled(tCameraReceiver, cameraDisable);
        if (cameraDisable) {
            g.timeBeforeDisable = Calendar.getInstance();
        } else {
            g.timeBeforeEnable = Calendar.getInstance();
        }

        g.cancelTimer(context, requestCode);
        g.timer[requestCode].beforeStart = Calendar.getInstance();
        g.timer[requestCode].isSet = false;

        g.setNormalTimer(context, requestCode);

        g.rewriteSettingFile(context);

        //Toast.makeText(context, "Received ", Toast.LENGTH_LONG).show();
    }
}
