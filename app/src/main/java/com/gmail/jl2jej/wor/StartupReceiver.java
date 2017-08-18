package com.gmail.jl2jej.wor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by kido on 2017/07/31.
 * 再起動時の処理を実装
 */

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive in");
        if (intent == null) {
            Log.i(TAG, "********* intent == null *********");
        } else {
            Log.i(TAG, "onReceive:" + intent.getAction());
            Intent intentService = new Intent(context, BackEndService.class);
            intentService.putExtra(BackEndService.COMMAND, BackEndService.STARTUP);
            context.startService(intentService);
        }
        Log.i(TAG, "onReceive out");
    }
}
