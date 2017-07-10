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
    protected DevicePolicyManager devicePolicyManager;
    protected ComponentName tCameraReceiver;

    protected void setTimer(Context context, Boolean cameraDisable, int requestCode, Calendar calendar) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra("KEYWORD", cameraDisable);
        intent.putExtra("RCODE", requestCode);
        PendingIntent sender = PendingIntent.getBroadcast(context.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    protected void cancelTimer(Context context, int requestCode) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        devicePolicyManager = (DevicePolicyManager)context.getSystemService(context.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(context, CameraReceiver.class);
        Boolean cameraDisable = intent.getBooleanExtra("KEYWORD", true);
        int requestCode = intent.getIntExtra("RCODE", 1);

        devicePolicyManager.setCameraDisabled(tCameraReceiver, cameraDisable);

        cancelTimer(context, requestCode);
       
        Toast.makeText(context, "Received ", Toast.LENGTH_LONG).show();
    }
}
