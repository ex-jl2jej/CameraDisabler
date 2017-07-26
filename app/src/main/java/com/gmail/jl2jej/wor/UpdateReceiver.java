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
    public static Handler handler;

    @Override
    public void onReceive(Context context, Intent intent) {
        //　ここで　もらった bundle をそのまま渡すようにする
        Bundle bundle = intent.getExtras();
        Log.i("UpdateReceiver", "_onReceive");
        if (handler != null) {
            Message msg = new Message();

            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    public void registerHandler(Handler locationUpdateHandler) {
        handler = locationUpdateHandler;
    }
}
