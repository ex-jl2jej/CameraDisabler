package com.gmail.jl2jej.wor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * Created by kido on 2017/07/17.
 *
 */

public class UpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "UpdateReceiver";
    public static Handler handler;

    @Override
    public void onReceive(Context context, Intent intent) {
        //　ここで　もらった bundle をそのまま渡すようにする
        Bundle bundle = intent.getExtras();
        Log.i(TAG, "onReceive in");
        if (handler != null) {
            Message msg = new Message();

            msg.setData(bundle);
            Log.i(TAG, "sendMessage");
            handler.sendMessage(msg);
        }
        Log.i(TAG, "onReceive out");
    }

    public void registerHandler(Handler locationUpdateHandler) {
        handler = locationUpdateHandler;
    }
}
