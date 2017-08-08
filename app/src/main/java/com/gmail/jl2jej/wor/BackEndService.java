package com.gmail.jl2jej.wor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by kido on 2017/07/15.
 * このサービスは、このアプリのデータを保持し、UIへ引き渡す役割を持っている。
 * Activity から Intent　で情報をもらい、 UpdateReceiver を介して、Activityへ
 * Bundleを送信する。
 */

public class BackEndService extends Service {
    private final String TAG = "BackEndService";
    public static final String COMMAND = "com.gmail.jl2jej.wor.COMMAND";
    public static final String NOW_TIME = "com.gmail.jl2jej.wor.NOW_TIME";
    public static final String BOOLEAN = "com.gmail.jl2jej.wor.BOOLEAN";
    public static final String REQUEST_CODE = "com.gmail.jl2jej.wor.REQUEST_CODE";
    public static final String HOUR_OF_DAY = "com.gmail.jl2jej.wor.HOUR_OF_DAY";
    public static final String MIN = "com.gmail.jl2jej.wor.MIN";
    public static final String CAMERA_DISABLE = "com.gmail.jl2jej.wor.CAMERA_DISABLE";
    public static final String REWRITE_REQUEST = "com.gmail.jl2jej.wor.REWRITE_REQUEST";
    public static final String REDRAW_ACTION = "com.gmail.jl2jej.wor.REDRAW_ACTION";
    public static final int REDRAW = 1;
    public static final int ALARM_RECEIVE = 2;
    public static final int START_ACTIVITY = 3;
    public static final int TIME_BEFORE_DISABLE = 4;
    public static final int TIME_BEFORE_ENABLE = 5;
    public static final int CB_TIMER = 6;
    public static final int TIME_PICK = 7;
    public static final int SW_TIMER = 8;
    public static final int CB_HOLIDAY = 9;
    public static final int STARTUP = 10;
    public static final int REDRAW_TBD = 11;
    public static final int REDRAW_TBE = 12;
    public static final int REDRAW_TP = 13;
    public static final int REDRAW_CBH = 14;
    public static final int REDRAW_BS = 15;

    private static Globals g = null;

    private Handler handler;
    private BackEndService context;

    @Override
    public void onCreate() {
        super.onCreate();

        if (g == null) {
            Log.i(TAG, "onCreate:g == null");
        } else {
            Log.i(TAG, "onCreate:g != null");
        }

     }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int requestCode = 1;
        Boolean cd = true;
        super.onStartCommand(intent, flags, startId);
        Intent sendIntent = new Intent();

        if (g == null) {
            Log.i(TAG, "onStartCommand:g == null");
            g = new Globals();
            if (g.readSettingFile(this)) {
                g.rewriteSettingFile(this);
            }
        } else {
            Log.i(TAG, "onStartCommand:g != null");
        }

        if (intent != null) {
            if (intent.getStringExtra("CALLED") != null) {
                Log.i(TAG, "onStartCommand:" + intent.getStringExtra("CALLED") + ":startID:" + Integer.toString(startId));
            }

            switch (intent.getIntExtra(COMMAND, REDRAW)) {
                case STARTUP:
                    for (int i = 0 ; i <= Globals.timerEndIndex ; i++) {
                        if (g.timer[i].available) {
                            g.setNormalTimer(this, i);
                        }
                    }
                    Log.i(TAG, "StartUp out");
                    break;
                case START_ACTIVITY:
                    g.readSettingFile(this);
                    break;
                case TIME_BEFORE_DISABLE:
                    g.timeBeforeDisable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                    sendIntent.putExtra(COMMAND, REDRAW_TBD);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    Log.i(TAG, "TIME_BEFORE_DISABLE"+intent.getStringExtra(NOW_TIME));
                    sendBroadCast(sendIntent);
                    break;
                case TIME_BEFORE_ENABLE:
                    g.timeBeforeEnable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                    sendIntent.putExtra(COMMAND, REDRAW_TBE);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    Log.i(TAG, "TIME_BEFORE_ENABLE:"+intent.getStringExtra(NOW_TIME));
                    sendBroadCast(sendIntent);
                    break;
                case CB_TIMER:
                    Log.i(TAG, "CB_TIMER");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    if (intent.getBooleanExtra(BOOLEAN, false)) {
                        g.timer[requestCode].available = true;
                        g.setNormalTimer(this, requestCode);
                    } else {
                        g.timer[requestCode].available = false;
                        g.cancelTimer(this, requestCode);
                    }
                    break;
                case TIME_PICK:
                    Log.i(TAG, "TIME_PICK");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    int hourOfDay = intent.getIntExtra(HOUR_OF_DAY, 0);
                    int min = intent.getIntExtra(MIN, 0);
                    g.timer[requestCode].hourOfDay = hourOfDay;
                    g.timer[requestCode].min = min;
                    g.timer[requestCode].int2str();
                    if (g.timer[requestCode].available) {
                        g.setNormalTimer(this, requestCode);
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_TP);
                    sendIntent.putExtra(REQUEST_CODE, requestCode);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    sendBroadCast(sendIntent);
                    break;
                case SW_TIMER:
                    Log.i(TAG, "SW_TIMER");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    cd = intent.getBooleanExtra(CAMERA_DISABLE, true);
                    if (g.timer[requestCode].cameraDisable != cd) {
                        Log.i(TAG, "Switch changed");
                        g.timer[requestCode].beforeStart = g.initialCalendar();
                    } else {
                        Log.i(TAG, "Switch is same");
                    }
                    g.timer[requestCode].cameraDisable = cd;
                    if (g.timer[requestCode].available) {
                        g.setNormalTimer(this, requestCode);
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_BS);
                    sendIntent.putExtra(REQUEST_CODE, requestCode);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    sendBroadCast(sendIntent);
                    break;
                case CB_HOLIDAY:
                    Log.i(TAG, "CB_HOLIDAY");
                    cd = intent.getBooleanExtra(BOOLEAN, false);
                    boolean oldCd = g.timer[Globals.dateChange].available;
                    g.timer[Globals.dateChange].available = cd;
                    Calendar nt = g.parseDateString(intent.getStringExtra(NOW_TIME));

                    if (!oldCd && cd) {
                        g.timeHolidayModeOn = nt;
                    }
                    for (int i = Globals.timerStartIndex ; i <= Globals.timerEndIndex ; i++) {
                        g.setNormalTimer(this, i);
                    }
                    g.setNormalTimer(this, Globals.dateChange);
                    sendIntent.putExtra(COMMAND, REDRAW_CBH);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    sendBroadCast(sendIntent);
                    break;
                case REDRAW:
                    Log.i(TAG, "REDRAW");
                    sendIntent.putExtra(COMMAND, REDRAW);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    sendBroadCast(sendIntent);
                    break;
                case ALARM_RECEIVE:
                    Log.i(TAG, "ALARM_RECEIVE");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    if (requestCode != Globals.dateChange) {
                        Boolean cameraDisable = intent.getBooleanExtra(CAMERA_DISABLE, true);
                        g.timer[requestCode].beforeStart = g.parseDateString(intent.getStringExtra(NOW_TIME));
                        g.setNormalTimer(this, requestCode);
                        if (intent.getBooleanExtra(REWRITE_REQUEST, false)) {
                            if (cameraDisable) {
                                g.timeBeforeDisable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                            } else {
                                g.timeBeforeEnable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                            }
                        }
                    } else {
                        g.timer[Globals.dateChange].available = false;
                        g.cancelTimer(this, Globals.dateChange);
                    }
                    Log.i(TAG, "alarm redraw broadcast");
                    sendIntent.putExtra(COMMAND, REDRAW);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    sendBroadCast(sendIntent);
                    break;
            }
        }
        g.rewriteSettingFile(this);
        stopSelf();
        Log.i(TAG, "onStartCommand out");
        return START_STICKY;
    }

    protected void sendBroadCast(Intent intent) {
        Intent broadcastIntent = new Intent();

        broadcastIntent.putExtra(COMMAND, intent.getIntExtra(COMMAND,REDRAW));
        broadcastIntent.putExtra(REQUEST_CODE, intent.getIntExtra(REQUEST_CODE, 1));
        Log.i(TAG, "sendBroadCast");
        broadcastIntent.setAction(REDRAW_ACTION);
        g.getIntentFromGlobals(broadcastIntent);
        getBaseContext().sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        g = null;
    }
}
