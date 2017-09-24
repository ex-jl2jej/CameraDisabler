package com.gmail.jl2jej.wor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

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
    public static final String NOW_STATE = "com.gmail.jl2jej.wor.NOW_STATE";
    public static final String BOOLEAN = "com.gmail.jl2jej.wor.BOOLEAN";
    public static final String REQUEST_CODE = "com.gmail.jl2jej.wor.REQUEST_CODE";
    public static final String HOUR_OF_DAY = "com.gmail.jl2jej.wor.HOUR_OF_DAY";
    public static final String MIN = "com.gmail.jl2jej.wor.MIN";
    public static final String CAMERA_DISABLE = "com.gmail.jl2jej.wor.CAMERA_DISABLE";
    public static final String REWRITE_REQUEST = "com.gmail.jl2jej.wor.REWRITE_REQUEST";
    public static final String REDRAW_ACTION = "com.gmail.jl2jej.wor.REDRAW_ACTION";
    public static final String TARGET_DATE="com.gmail.jl2jej.wor.TARGET_DATE";
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
    public static final int DATE_PICK = 16;
    public static final int REDRAW_DP = 17;
    public static final int SCREEN_ON = 18;
    public static final String MAGIC_TIME = "07:01";

    private static Globals g = null;
    private static int numOfPowerSwitchScreenOn = 0;
    public static int numOfOnDestroy = 0;
    public static int numOfIntervalTimerOn = 0;
    private static int numOfToasts = 0;

    private Handler handler;
    private static BroadcastReceiver screenOnReceiver = null;
    private int sid;

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

    private void setNotificationTitle(Notification.Builder builder, boolean cameraDisabled, Calendar nowTime) {
        builder.setContentTitle("CameraDisabler:" + (cameraDisabled ? "Disable" : "Enable")
                + String.format(":%02d/%02d %02d:%02d", nowTime.get(Calendar.MONTH) + 1, nowTime.get(Calendar.DAY_OF_MONTH), nowTime.get(Calendar.HOUR_OF_DAY), nowTime.get(Calendar.MINUTE)));
    }

    private void notificationPrint(int requestCode, String message, boolean cameraDisabled) {
        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
        Notification.Builder builder = new Notification.Builder(getBaseContext());
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        builder.setSmallIcon(R.drawable.ic_stat_name);
        Calendar nowTime = Calendar.getInstance();

        setNotificationTitle(builder, cameraDisabled, nowTime);
        builder.setContentText(message);
        builder.setDefaults(Notification.PRIORITY_DEFAULT);
        builder.setContentIntent(contentIntent);
        if (Build.VERSION.SDK_INT >= 16) {
            manager.notify(requestCode, builder.build());
        } else {
            manager.notify(requestCode, builder.getNotification());
        }

    }

    private boolean lateTimerActivate() { // 遅延しているタイマーがあれば是正 是正したかどうかを返す
        DevicePolicyManager devicePolicyManager = null;
        ComponentName tCameraReceiver = null;
        Calendar nowTime = Calendar.getInstance();
        int[] order = new int[Globals.timerEndIndex+1]; // タイマーを時刻の順序に実行するための配列
        boolean isChanged = false;
        boolean oldCameraDisabled;

        devicePolicyManager = (DevicePolicyManager)getBaseContext().getSystemService(MainActivity.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(getBaseContext(), CameraReceiver.class);
        oldCameraDisabled = devicePolicyManager.getCameraDisabled(tCameraReceiver);

        for (int i = 0 ; i <= Globals.timerEndIndex ; i++) { // まずは、番号通りとしておく
            order[i] = i;
        }
        for (int i = 0 ; i < Globals.timerEndIndex ; i++) { //　単純なバブルソート
            for (int j = i+1 ; j <= Globals.timerEndIndex ; j++) {
                if (g.timer[order[i]].afterStart.after(g.timer[order[j]].afterStart)) {
                    int k;
                    k = order[i];
                    order[i] = order[j];
                    order[j] = k;
                }
            }
        }

        String txt = new String("");

        for (int j = 0 ; j <= Globals.timerEndIndex; j++) {
            int i = order[j];
            if (g.timer[i].available && nowTime.after(g.timer[i].afterStart)) { // タイマーが有効　かつ　遅延
                if (i == Globals.dateChange) {
                    Log.i(TAG, "late check: holiday mode cancel");
                    g.timer[Globals.dateChange].available = false;
                    g.cancelTimer(getBaseContext(), i);
                    txt = txt + "タイマー遅延:ホリデーモード解除:" + Globals.dateToString(nowTime);
                } else {
                    Log.i(TAG, "late check: change camera disable:" + g.timer[i].cameraDisable);
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, g.timer[i].cameraDisable);
                    if (g.timer[i].cameraDisable != oldCameraDisabled) {
                        if (g.timer[i].cameraDisable) {
                            g.timeBeforeDisable = nowTime;
                        } else {
                            g.timeBeforeEnable = nowTime;
                        }
                    }
                    g.timer[i].beforeStart = nowTime;
                    g.setNormalTimer(getBaseContext(), i);
                    txt = txt + "タイマー遅延:No." + i + ":" + Globals.dateToString(nowTime);
                }
                isChanged |= true;
                Log.i(TAG, "isChanged = "+isChanged);
            } else {
                if (g.timer[i].available) {
                    isChanged |= g.setNormalTimer(getBaseContext(), i);
                } else {
                    isChanged |= g.cancelTimer(getBaseContext(), i);
                }
                Log.i(TAG, "timer is not late:" + i);
            }
        }
        oldCameraDisabled = devicePolicyManager.getCameraDisabled(tCameraReceiver);
        if (g.timer[1].timeInDay.equals(MAGIC_TIME)) {
            notificationPrint(2, txt, oldCameraDisabled);
        }
        Log.i(TAG, "return isChanged = "+isChanged);
        return isChanged;
    }

    /*
     * ユーザーや、システムからサービスが停止されることがあり、そうなると、タイマーの遅延が
     * 見逃され、希望した時刻にカメラが無効にできない可能性があるため、
     * １０分
     * ごとに起動されるタイマーを用意する。
     * requestCode を numOfIntervalTimer == 100 としてアラームを起動
     */
    private void intervalTimerSet() {
        final long intervalTimeMillis = 10 * 60 * 1000;
        Intent intent = new Intent(getBaseContext().getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra(BackEndService.REQUEST_CODE, Globals.numOfIntervalTimer);

        PendingIntent sender = PendingIntent.getBroadcast(getBaseContext().getApplicationContext(), Globals.numOfIntervalTimer, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)getBaseContext().getSystemService(getBaseContext().ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), intervalTimeMillis, sender);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int requestCode = 1;
        Boolean cd = true;          // CameraDisable のテンポラリ変数
        Boolean isChanged = false;  // 処理をする中で状態が変わったかどうか
        Calendar tmp;                   //比較用
        super.onStartCommand(intent, flags, startId);
        Intent sendIntent = new Intent();
        sid = startId;
        Boolean redrawFlag = false;

        if (g == null) {
            Log.i(TAG, "onStartCommand:g == null:startID:" + Integer.toString(startId));
            g = new Globals();
            g.readSettingFile(getBaseContext());
            if (g.readSettingFile(getBaseContext())) { // 2度読みしてメモリとの相違があったということは、設定ファイルが壊れている
                Log.i(TAG, "Setting file is broken");
                isChanged = true;
            }
        } else {
            Log.i(TAG, "onStartCommand:g != null:startID:" + Integer.toString(startId));
        }

        if (intent != null) {
            if (intent.getStringExtra("CALLED") != null) {
                Log.i(TAG, "onStartCommand:" + intent.getStringExtra("CALLED") + ":startID:" + Integer.toString(startId));
            }

            switch (intent.getIntExtra(COMMAND, REDRAW)) {
                case STARTUP: // アプリ再起動時
                    Log.i(TAG, "StartUp out:startID:" + Integer.toString(startId));
                    isChanged = lateTimerActivate();
                    break;
                case START_ACTIVITY: // アクティビティが起動されて最初のサービス呼び出し
                    Log.i(TAG, "START_ACTIVITY:startID:" + Integer.toString(startId));
                    isChanged = g.readSettingFile(getBaseContext());
                    isChanged |= lateTimerActivate();
                    intervalTimerSet();
                    break;
                case TIME_BEFORE_DISABLE: // カメラ有効無効スイッチが　カメラ無効に操作された
                    tmp =  g.parseDateString(intent.getStringExtra(NOW_TIME));
                    if (!g.compareCalendar(tmp, g.timeBeforeDisable)) {
                        g.timeBeforeDisable = tmp;
                        isChanged = true;
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_TBD);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    Log.i(TAG, "TIME_BEFORE_DISABLE" + intent.getStringExtra(NOW_TIME));
                    redrawFlag = true;
                    break;
                case TIME_BEFORE_ENABLE: // カメラ有効無効スイッチが　カメラ有効に操作された
                    tmp = g.parseDateString(intent.getStringExtra(NOW_TIME));
                    if (!g.compareCalendar(tmp, g.timeBeforeEnable)) {
                        g.timeBeforeEnable = tmp;
                        isChanged = true;
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_TBE);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    Log.i(TAG, "TIME_BEFORE_ENABLE:" + intent.getStringExtra(NOW_TIME));
                    redrawFlag = true;
                    break;
                case CB_TIMER: // タイマーのチェックボックスが操作された
                    Log.i(TAG, "CB_TIMER");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    if (intent.getBooleanExtra(BOOLEAN, false)) {
                        if (!g.timer[requestCode].available) {
                            g.timer[requestCode].available = true;
                            isChanged = true;
                        }
                        isChanged |= g.setNormalTimer(getBaseContext(), requestCode);
                    } else {
                        if (g.timer[requestCode].available) {
                            g.timer[requestCode].available = false;
                            isChanged = true;
                        }
                        isChanged |= g.cancelTimer(getBaseContext(), requestCode);
                    }
                    break;
                case TIME_PICK: // タイマーの起動時間が変更された
                    Log.i(TAG, "TIME_PICK");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    int hourOfDay = intent.getIntExtra(HOUR_OF_DAY, 0);
                    int min = intent.getIntExtra(MIN, 0);
                    if (g.timer[requestCode].hourOfDay != hourOfDay
                        || g.timer[requestCode].min != min) {
                        g.timer[requestCode].hourOfDay = hourOfDay;
                        g.timer[requestCode].min = min;
                        isChanged = true;
                    }
                    g.timer[requestCode].int2str();
                    if (g.timer[requestCode].available) {
                        isChanged |= g.setNormalTimer(getBaseContext(), requestCode);
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_TP);
                    sendIntent.putExtra(REQUEST_CODE, requestCode);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    break;
                case SW_TIMER: // タイマーの　カメラ有効無効スイッチが操作された
                    Log.i(TAG, "SW_TIMER");
                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    cd = intent.getBooleanExtra(CAMERA_DISABLE, true);
                    if (g.timer[requestCode].cameraDisable != cd) {
                        Log.i(TAG, "Switch changed");
                        g.timer[requestCode].beforeStart = g.initialCalendar();
                        g.timer[requestCode].cameraDisable = cd;
                        isChanged = true;
                    } else {
                        Log.i(TAG, "Switch is same");
                    }

                    if (g.timer[requestCode].available) {
                        isChanged |= g.setNormalTimer(getBaseContext(), requestCode);
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_BS);
                    sendIntent.putExtra(REQUEST_CODE, requestCode);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    break;
                case DATE_PICK: // ホリデーモードの日付が変更された
                    Log.i(TAG, "DATE_PICK");
                    requestCode = intent.getIntExtra(REQUEST_CODE, Globals.dateChange);
                    tmp = g.parseDateString(intent.getStringExtra(TARGET_DATE));
                    if (!g.compareCalendar(tmp, g.timer[Globals.dateChange].afterStart)) {
                        g.timer[Globals.dateChange].afterStart = tmp;
                        isChanged = true;
                    }
                    if (g.timer[Globals.dateChange].available) {
                        isChanged |= lateTimerActivate();
                    }
                    sendIntent.putExtra(COMMAND, REDRAW_DP);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    break;
                case CB_HOLIDAY: // ホリデーモードのチェックボックスが操作された
                    cd = intent.getBooleanExtra(BOOLEAN, false);
                    g.timer[Globals.dateChange].available = cd;
                    Calendar nt = g.parseDateString(intent.getStringExtra(NOW_TIME));

                    if (cd) {
                        g.timeHolidayModeOn = nt;
                        isChanged = true;
                        if (g.timeHolidayModeOn.after(g.timer[Globals.dateChange].afterStart)) {
                            g.timer[Globals.dateChange].afterStart.set(nt.get(Calendar.YEAR), nt.get(Calendar.MONTH), nt.get(Calendar.DAY_OF_MONTH)+1, 0, 0, 0);
                        }
                        Log.i(TAG, "set timeHolidayModeOn");
                    }
                    isChanged |= lateTimerActivate();
                    sendIntent.putExtra(COMMAND, REDRAW_CBH);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    Log.i(TAG, "CB_HOLIDAY:"+Globals.dateToString(g.timeHolidayModeOn ));
                    break;
                case REDRAW: // 全体のリドロー要求
                    Log.i(TAG, "REDRAW");
                    sendIntent.putExtra(COMMAND, REDRAW);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    break;
                case ALARM_RECEIVE: // 通常のタイマーが起動した
                    Log.i(TAG, "ALARM_RECEIVE");
                    String txt = new String("");

                    requestCode = intent.getIntExtra(REQUEST_CODE, 1);
                    if (requestCode != Globals.dateChange) {
                        txt = txt + "Alarm No." + requestCode + "が";
                        Boolean cameraDisable = intent.getBooleanExtra(CAMERA_DISABLE, true);
                        g.timer[requestCode].beforeStart = g.parseDateString(intent.getStringExtra(NOW_TIME));
                        txt = txt + Globals.dateToString(g.timer[requestCode].beforeStart) + "に起動";
                        g.setNormalTimer(getBaseContext(), requestCode);
                        if (intent.getBooleanExtra(REWRITE_REQUEST, false)) {
                            if (cameraDisable) {
                                txt = txt + ":無効化";
                                g.timeBeforeDisable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                            } else {
                                txt = txt + ":有効化";
                                g.timeBeforeEnable = g.parseDateString(intent.getStringExtra(NOW_TIME));
                            }
                        }
                    } else {
                        txt = txt + "ホリデーモードが解除";
                        g.timer[Globals.dateChange].available = false;
                        g.cancelTimer(getBaseContext(), Globals.dateChange);
                    }
                    isChanged = true;
                    if (g.timer[1].timeInDay.equals(MAGIC_TIME)) {
                        notificationPrint(1, txt, intent.getBooleanExtra(NOW_STATE, true));
                    }

                    Log.i(TAG, "alarm redraw broadcast");
                    sendIntent.putExtra(COMMAND, REDRAW);
                    sendIntent.setAction(BackEndService.REDRAW_ACTION);
                    redrawFlag = true;
                    break;
                case SCREEN_ON: // インターバルタイマーが起動したとき、および SCREEN_ON になったとき
                    Log.i(TAG, "case SCREEN_ON");
                    isChanged = lateTimerActivate();
                    Log.i(TAG, "isChanged = "+isChanged);
                    if (isChanged) {
                        sendIntent.putExtra(COMMAND, REDRAW);
                        sendIntent.setAction(BackEndService.REDRAW_ACTION);
                        redrawFlag = true;
                    }
                    numOfToasts++;
                    switch (intent.getIntExtra(REQUEST_CODE,1)) {
                        case Globals.numOfIntervalTimer:
                            numOfIntervalTimerOn++;
                            break;
                        case Globals.screenOnCode:
                            numOfPowerSwitchScreenOn++;
                            break;
                        case Globals.ON_DESTROY:
                            numOfOnDestroy++;
                            break;
                    }
                    if (g.timer[1].timeInDay.equals(MAGIC_TIME)) {
                        Toast.makeText(getBaseContext(), "CameraDisabler:check timers\n合計:"
                                +numOfToasts+":sw:" +numOfPowerSwitchScreenOn+":int:"+numOfIntervalTimerOn+":des:"+numOfOnDestroy
                                +"\n" + g.timer[Globals.dateChange].timeInDay, Toast.LENGTH_LONG).show();
                    }
                    Log.i(TAG, "break SCREEN_ON");
                    break;
            }
        } else {
            Log.i(TAG, "************* intent == null ***************");
        }
        Log.i(TAG, "isChanged = "+isChanged);
        if (isChanged) {
            Log.i(TAG, "rewriteSettingFile");
            g.rewriteSettingFile(getBaseContext());
            redrawFlag = true;
        } else {
            Log.i(TAG, "not rewriteSettingFile");
        }

        if (screenOnReceiver == null) { // SCREEN_ONを捕まえるレシーバを登録
            //この登録は、サービスを stopSelf させても生きているが、
            // 明にアクティビティを削除する等すると、働かなくなる
            // そこで、インターバルタイマーを使って、サービスを生かす作戦に出ることにする
            Log.i(TAG, "ScreenOnReceiver:Register:sid=" + sid);
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            screenOnReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "screenOnReceiver:received");
                    if (intent == null) {
                        Log.i(TAG, "******** intent == null **********");
                    } else {
                        // 受信したら、SCREEN_ON をCOMMANDにして、サービスを起動するだけにする。
                        Log.i(TAG, "startService comannd is SCREEN_ON");
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        serviceIntent.putExtra(COMMAND, SCREEN_ON);
                        serviceIntent.putExtra(REQUEST_CODE, Globals.screenOnCode);
                        startService(serviceIntent);
                    }
                }
            };
            getBaseContext().getApplicationContext().registerReceiver(screenOnReceiver, intentFilter);
        }
        if (redrawFlag) {
            sendBroadCast(sendIntent);
        }
        Log.i(TAG, "stopSelf:sid=" + sid);
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
        Log.i(TAG, "onDestroy:sid =" + Integer.toString(sid));
        g = null;
    }
}
