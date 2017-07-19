package com.gmail.jl2jej.wor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by kido on 2017/07/15.
 */

public class BackEndService extends Service {
    private final String TAG = "BackEndService";
    public static final String COMMAND = "COMMAND";
    public static final String NOW_TIME = "NOW_TIME";
    public static final String BOOLEAN = "BOOLEAN";
    public static final int REDRAW = 1;
    public static final int ALARM_RECEIVE = 2;
    public static final int START_ACTIVITY = 3;
    public static final int TIME_BEFORE_DISABLE = 4;
    public static final int TIME_BEFORE_ENABLE = 5;
    public static final int CB_TIMER1 = 6;
    public static Globals g = null;

    private Handler handler;
    private BackEndService context;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        g = new Globals(this);
        g.readSettingFile(this);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            Log.i(TAG, "onStartCommand" + intent.getStringExtra("CALLED"));
            switch (intent.getIntExtra(COMMAND, REDRAW)) {
                case START_ACTIVITY:
                    g.readSettingFile(this);
                    sendBroadcast(g.getIntentFromGlobals(intent));
                    break;
                case REDRAW:
                    sendBroadCast(intent);
                    break;
                case ALARM_RECEIVE:
                    break;
            }
        }
        return START_STICKY;
    }

    public void registerHandler(Handler updateHandler) {
        handler = updateHandler;
    }

    protected void sendBroadCast(Intent intent) {
        Intent broadcastIntent = intent;
        //broadcastIntent.putExtra("message", "hogehoge");
        broadcastIntent.setAction("REDRAW_ACTION");
        getBaseContext().sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }
}
