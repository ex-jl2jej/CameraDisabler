package com.gmail.jl2jej.wor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
/**
 * Created by kido on 2017/07/07.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Received ", Toast.LENGTH_LONG).show();
    }
}
