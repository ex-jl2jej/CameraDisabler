package com.gmail.jl2jej.wor;

import android.app.admin.DevicePolicyManager;
import android.app.backup.BackupAgent;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by kido on 2017/07/07.
 * タイマーアラームを受け取り
 * カメラ機能のON/OFFを実行し
 * 表示切替の情報をIntentに入れて、BackEndServiceを呼び出す
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmBroadcastReceiver";
    protected DevicePolicyManager devicePolicyManager;
    protected ComponentName tCameraReceiver;

    @Override
    public void onReceive(Context context, Intent intent) {
        Boolean tCameraActive;
        Log.i(TAG, "onReceive in");
        if (intent == null) {
            Log.i(TAG, "*********** intent == null ************");
        } else {
            devicePolicyManager = (DevicePolicyManager) context.getSystemService(MainActivity.DEVICE_POLICY_SERVICE);
            tCameraReceiver = new ComponentName(context, CameraReceiver.class);
            Boolean cameraDisable = intent.getBooleanExtra(BackEndService.CAMERA_DISABLE, true);
            int requestCode = intent.getIntExtra(BackEndService.REQUEST_CODE, 1);
            Boolean oldCameraDisable = devicePolicyManager.getCameraDisabled(tCameraReceiver);

            //カメラを有効無効できるようにする
            tCameraActive = devicePolicyManager.isAdminActive(tCameraReceiver);

            if (tCameraActive == false) {
                Intent intentCamera = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intentCamera.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, tCameraReceiver);
                //startActivityForResult(intentCamera, 1);
            }


            if (requestCode != Globals.dateChange && requestCode != Globals.codeOfIntervalTimer) {
                devicePolicyManager.setCameraDisabled(tCameraReceiver, cameraDisable);
            }

            Intent serviceIntent = new Intent(context, BackEndService.class);


            if (requestCode == Globals.codeOfIntervalTimer) {
                Log.i(TAG, "SCREEN_ON");
                serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.SCREEN_ON);
                serviceIntent.putExtra(BackEndService.REQUEST_CODE, requestCode);
            } else {
                Log.i(TAG, "Alarm");
                serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.ALARM_RECEIVE);
                serviceIntent.putExtra("CALLED", "AlarmBroadCast");
                serviceIntent.putExtra(BackEndService.CAMERA_DISABLE, cameraDisable);
                serviceIntent.putExtra(BackEndService.REQUEST_CODE, requestCode);
                serviceIntent.putExtra(BackEndService.NOW_TIME, Globals.dateToString(Calendar.getInstance()));
                serviceIntent.putExtra(BackEndService.NOW_STATE, cameraDisable);
                if (oldCameraDisable != cameraDisable && requestCode != Globals.dateChange) {
                    serviceIntent.putExtra(BackEndService.REWRITE_REQUEST, true);
                    Log.i(TAG, "REWRITE REQUEST true");
                } else {
                    serviceIntent.putExtra(BackEndService.REWRITE_REQUEST, false);
                    Log.i(TAG, "REWRITE REQUEST false");
                }
            }
            context.startService(serviceIntent);
        }
        Log.i(TAG, "onReceive out");
    }
}
