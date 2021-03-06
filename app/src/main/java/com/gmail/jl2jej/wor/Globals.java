package com.gmail.jl2jej.wor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Created by kido on 2017/07/10.
 * このアプリの状態変数のクラス　サービスとアクティビティで変数を１つずつ作る
 */

public class Globals {
    private static final String TAG = "Globals";
    // 日付の初期値
    private final int INIT_YEAR = 1970;
    private final int INIT_MONTH = 0;
    private final int INIT_DAY = 1;
    private final int INIT_HOUR = 9;
    private final int INIT_MIN = 0;
    private final int INIT_SEC = 0;
    //出てくる順番
    private final int YEAR_POS = 1;
    private final int MONTH_POS = 2;
    private final int DATE_POS = 3;
    private final int HOUR_POS = 4;
    private final int MIN_POS = 5;
    private final int SEC_POS = 6;

    public static final String settingFileName = "setting.dat";
    public static final String INITIAL_TIME = "00:00";

    //protected Context that;
    protected Calendar timeBeforeDisable;
    protected Calendar timeBeforeEnable;
    protected Calendar timeHolidayModeOn;
    public static final int timerStartIndex = 1;
    public static final int timerEndIndex = 3;
    public static final int dateChange = 0;
    public static final int codeOfIntervalTimer = 100;
    public static final int screenOnCode = 101;
    public static final int ON_DESTROY = 102;
    protected jejTimer timer[];

    protected void setTimer(Context context, Boolean cameraDisable, int requestCode, Calendar calendar) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra(BackEndService.CAMERA_DISABLE, cameraDisable);
        intent.putExtra(BackEndService.REQUEST_CODE, requestCode);
        PendingIntent sender = PendingIntent.getBroadcast(context.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        this.timer[requestCode].isSet = true;

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    protected boolean cancelTimer(Context context, int requestCode) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        boolean isChanged = false;

        if (this.timer[requestCode].isSet) {
            isChanged = true;
        }
        this.timer[requestCode].isSet = false;
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        return isChanged;
    }

    protected Calendar makeTargetTime(int requestCode) {
        Calendar nowTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        if (timer[dateChange].available) {
            if (requestCode == dateChange) {
                if (nowTime.after(this.timer[dateChange].afterStart)) {
                    int nowDay = targetTime.get(DAY_OF_MONTH);
                    targetTime.set(DAY_OF_MONTH, nowDay+1);
                } else {
                    targetTime = (Calendar)this.timer[dateChange].afterStart.clone();
                }
                targetTime.set(Calendar.HOUR_OF_DAY, 0);
                targetTime.set(Calendar.MINUTE, 0);
                targetTime.set(SECOND, 0);
            } else {
                targetTime = (Calendar)timer[dateChange].afterStart.clone();
                targetTime.set(Calendar.HOUR_OF_DAY, timer[requestCode].hourOfDay);
                targetTime.set(MINUTE, timer[requestCode].min);
                targetTime.set(SECOND, 0);
            }
        } else {
            if (requestCode == dateChange) {
                targetTime = (Calendar)timer[dateChange].afterStart.clone();
            } else {
                if ((timer[requestCode].hourOfDay * 60 + timer[requestCode].min) <= (nowTime.get(Calendar.HOUR_OF_DAY) * 60 + nowTime.get(MINUTE))) {
                    int nowDay = targetTime.get(DAY_OF_MONTH);
                    targetTime.set(DAY_OF_MONTH, nowDay + 1);
                }
                targetTime.set(Calendar.HOUR_OF_DAY, timer[requestCode].hourOfDay);
                targetTime.set(MINUTE, timer[requestCode].min);
                targetTime.set(SECOND, 0);
            }
        }

        return targetTime;
    }

    protected boolean setNormalTimer(Context context, int requestCode) {
        Calendar targetTime = makeTargetTime(requestCode);
        boolean isChanged = false;

        if (timer[requestCode].available == false && timer[requestCode].isSet == true ) {
            cancelTimer(context, requestCode);
            isChanged = true;
        } else if (timer[requestCode].available == true) {
            if (timer[requestCode].isSet == true) { // 設定されている
                cancelTimer(context, requestCode);
            }
            setTimer(context, timer[requestCode].cameraDisable, requestCode, targetTime);
            timer[requestCode].isSet = true;
            if (!compareCalendar(targetTime, timer[requestCode].afterStart)) {
                timer[requestCode].afterStart = targetTime;
                isChanged = true;
            }
        }
        return isChanged;
    }

    protected Calendar parseDateString(String str) {
        Calendar date = Calendar.getInstance();
        String regex = "(\\d+)/(\\d+)/(\\d+)\\s+(\\d+):(\\d+):(\\d+)$";
        Pattern ptn = Pattern.compile(regex);

        Matcher m = ptn.matcher(str);
        if (m.find()) {
            int year = Integer.parseInt(m.group(YEAR_POS));
            int month = Integer.parseInt(m.group(MONTH_POS))-1;
            int day = Integer.parseInt(m.group(DATE_POS));

            int hour = Integer.parseInt(m.group(HOUR_POS));
            int min = Integer.parseInt(m.group(MIN_POS));
            int sec = Integer.parseInt(m.group(SEC_POS));
            date.set(year, month,day,hour,min,sec);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("日付の形式がおかしい {0}", str));
        }
        return date;
    }

    protected static String dateToString(Calendar date)  {
        String str = String.format("%04d/%02d/%02d %02d:%02d:%02d", date.get(YEAR), date.get(MONTH)+1, date.get(DAY_OF_MONTH), date.get(Calendar.HOUR_OF_DAY), date.get(MINUTE), date.get(SECOND));

        return str;
    }

    public Calendar initialCalendar() {
        Calendar initCal = Calendar.getInstance();

        initCal.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
        return initCal;
    }

    public boolean compareCalendar(Calendar a, Calendar b) {
        String sa = dateToString(a);
        String sb = dateToString(b);

        if (sa.equals(sb)) {
            return true;
        } else {
            return false;
        }
    }

    protected Boolean readSettingFile(Context context) {
        BufferedReader reader = null;
        Boolean doRewriteFile = false;

        Log.i(TAG, "readSettingFile in");
        try {
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(settingFileName)));
            String line;

            if((line = reader.readLine()) != null) {
                try {
                    Calendar rd = parseDateString(line);
                    if (!compareCalendar(timeBeforeDisable, rd)) {
                        Log.i(TAG, "timerBeforeDisable is changed:"+Globals.dateToString(timeBeforeDisable)+":"+Globals.dateToString(rd));
                        timeBeforeDisable = rd;
                        doRewriteFile = true;

                    }
                } catch(IllegalArgumentException e) {
                    timeBeforeDisable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }
            } else {
                timeBeforeDisable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                doRewriteFile = true;
            }
            if((line = reader.readLine()) != null) {
                try {
                    Calendar rd = parseDateString(line);
                    if (!compareCalendar(timeBeforeEnable, rd)) {
                        timeBeforeEnable = rd;
                        doRewriteFile = true;
                        Log.i(TAG, "timerBeforeEnable is changed");
                    }
                } catch (IllegalArgumentException e) {
                    timeBeforeEnable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }
            } else {
                timeBeforeEnable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                doRewriteFile = true;
            }
            for (int i = 0 ; i <= timerEndIndex ; i++) {
                if ((line = reader.readLine()) != null) {
                    String regex = "(true|false) (disable|enable) (\\d\\d):(\\d\\d)$";
                    Pattern ptn = Pattern.compile(regex);

                    Matcher m = ptn.matcher(line);
                    if (m.find()) {
                        if (m.group(1).equals("true")) {
                            if (!timer[i].available) {
                                timer[i].available = true;
                                doRewriteFile = true;
                                Log.i(TAG, "available of No" + i + " timer is changed");
                            }
                        } else {
                            if (timer[i].available) {
                                timer[i].available = false;
                                doRewriteFile = true;
                                Log.i(TAG, "available of No" + i + " timer is changed");
                            }
                        }
                        if (m.group(2).equals("disable")) {
                            if (!timer[i].cameraDisable) {
                                timer[i].cameraDisable = true;
                                doRewriteFile = true;
                                Log.i(TAG, "cameraDisable of No" + i + " timer is changed");
                            }
                        } else {
                            if (timer[i].cameraDisable) {
                                timer[i].cameraDisable = false;
                                doRewriteFile = true;
                                Log.i(TAG, "cameraDisable of No" + i + " timer is changed");
                            }
                        }
                        int h = Integer.parseInt(m.group(3));
                        int min = Integer.parseInt(m.group(4));
                        if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                            if (timer[i].hourOfDay != h || timer[i].min != min) {
                                timer[i].hourOfDay = h;
                                timer[i].min = min;
                                doRewriteFile = true;
                                Log.i(TAG, "hourOfDay or min of No"+i+" timer is changed");
                            }
                            timer[i].int2str();
                        } else {
                            timer[i].timeInDay = INITIAL_TIME;
                            timer[i].str2int();
                            doRewriteFile = true;
                        }
                    } else {
                        timer[i].available = false;
                        timer[i].cameraDisable = true;
                        timer[i].timeInDay = INITIAL_TIME;
                        timer[i].str2int();
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].available = false;
                    timer[i].cameraDisable = true;
                    timer[i].timeInDay = INITIAL_TIME;
                    timer[i].str2int();
                    doRewriteFile = true;
                }
                if ((line = reader.readLine()) != null) {
                    try {
                        Calendar tmp = parseDateString(line);
                        if (!compareCalendar(tmp, timer[i].beforeStart)) {
                            timer[i].beforeStart = tmp;
                            doRewriteFile = true;
                            Log.i(TAG, "beforeStart of No"+i+" timer is changed");
                        }
                    } catch (IllegalArgumentException e) {
                        timer[i].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }

                if ((line = reader.readLine()) != null) {
                    try {
                        String regex = "(.*)\\s+SET$";
                        Pattern ptn = Pattern.compile(regex);

                        Matcher m = ptn.matcher(line);
                        if (m.find()) {
                            line = m.group(1);
                            if (!timer[i].isSet) {
                                timer[i].isSet = true;
                                doRewriteFile = true;
                                Log.i(TAG, "isSet of No"+i+" timer is changed");
                            }
                        } else {
                            if (timer[i].isSet) {
                                timer[i].isSet = false;
                                doRewriteFile = true;
                                Log.i(TAG, "isSet of No"+i+" timer is changed");
                            }
                        }
                        Calendar tmp = parseDateString(line);
                        if (!compareCalendar(tmp, timer[i].afterStart)) {
                            timer[i].afterStart = tmp;
                            doRewriteFile = true;
                            Log.i(TAG, "afterStart of No"+i+" timer is changed");
                        }
                    } catch (IllegalArgumentException e) {
                        timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        timer[i].isSet = false;
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    timer[i].isSet = false;
                    doRewriteFile = true;
                }

            }

            if ((line = reader.readLine()) != null) {
                try {
                    Calendar tmp = parseDateString(line);
                    if (!compareCalendar(tmp, timeHolidayModeOn)) {
                        timeHolidayModeOn = tmp;
                        doRewriteFile = true;
                        Log.i(TAG, "timerHolidayModeOn is changed");
                    }
                } catch (IllegalArgumentException e) {
                    timeHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }
            } else {
                timeHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                doRewriteFile = true;
            }
        } catch (IOException e) {
            timeBeforeDisable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            timeBeforeEnable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            for (int i = 0 ; i <= timerEndIndex ; i++) {
                timer[i].available = false;
                timer[i].cameraDisable = true;
                timer[i].timeInDay = INITIAL_TIME;
                timer[i].str2int();
                timer[i].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            }
            timeHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            doRewriteFile = true;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "readSettingFile out");
        return doRewriteFile;
    }

    protected void rewriteSettingFile(Context context) {
        BufferedWriter writer = null;
        Log.i(TAG, "rewriteSettingFile in");
        try {
            String newLine = System.getProperty("line.separator");
            writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(settingFileName, Context.MODE_PRIVATE)));
            writer.write(dateToString(timeBeforeDisable) + newLine);
            writer.write(dateToString(timeBeforeEnable) + newLine);
            for (int i = 0 ; i <= timerEndIndex ; i++ ) {
                if (timer[i].available && timer[i].isSet) {
                    writer.write("true ");
                } else {
                    writer.write("false ");
                }
                if( timer[i].cameraDisable) {
                    writer.write("disable ");
                } else {
                    writer.write("enable ");
                }
                writer.write(timer[i].timeInDay + newLine);
                writer.write(dateToString(timer[i].beforeStart) + newLine);
                if (timer[i].isSet) {
                    writer.write(dateToString(timer[i].afterStart) + " SET" + newLine);
                } else {
                    writer.write(dateToString(timer[i].afterStart) + newLine);
                }
            }
            writer.write(dateToString(timeHolidayModeOn) + newLine);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "rewriteSettingFile out");
    }

    public Intent getIntentFromGlobals(Intent globalsIntent) {
        Globals g = this;
        globalsIntent.putExtra("timeBeforeDisable", dateToString(g.timeBeforeDisable));
        globalsIntent.putExtra("timeBeforeEnable", dateToString(g.timeBeforeEnable));
        globalsIntent.putExtra("timeHolidayModeOn", dateToString(g.timeHolidayModeOn));
        for (int i = 0 ; i <= timerEndIndex ; i++ ) {
            globalsIntent.putExtra("cbTimer"+Integer.toString(i), g.timer[i].available );
            globalsIntent.putExtra("swTimer"+Integer.toString(i), g.timer[i].cameraDisable);
            globalsIntent.putExtra("timeTimer"+Integer.toString(i), g.timer[i].timeInDay);
            globalsIntent.putExtra("btTimer"+Integer.toString(i), dateToString(g.timer[i].beforeStart));
            globalsIntent.putExtra("atTimer"+Integer.toString(i), dateToString(g.timer[i].afterStart));
            globalsIntent.putExtra("setTimer"+Integer.toString(i), g.timer[i].isSet);
        }
        return globalsIntent;
    }

    public void setGlobalsFromIntent(Bundle bundle) {
        Globals g = this;
        g.timeBeforeDisable = g.parseDateString(bundle.getString("timeBeforeDisable"));
        g.timeBeforeEnable = g.parseDateString(bundle.getString("timeBeforeEnable"));
        g.timeHolidayModeOn = g.parseDateString(bundle.getString("timeHolidayModeOn"));
        for (int i = 0 ; i <= timerEndIndex ; i++ ) {
            g.timer[i].available = bundle.getBoolean("cbTimer"+Integer.toString(i), false);
            g.timer[i].cameraDisable = bundle.getBoolean("swTimer"+Integer.toString(i), true );
            g.timer[i].timeInDay = bundle.getString("timeTimer"+Integer.toString(i));
            g.timer[i].beforeStart = g.parseDateString(bundle.getString("btTimer"+Integer.toString(i)));
            g.timer[i].afterStart = g.parseDateString(bundle.getString("atTimer"+Integer.toString(i)));
            g.timer[i].isSet = bundle.getBoolean("setTimer"+Integer.toString(i), false);
        }
    }

    public Globals() {
        timeBeforeDisable = Calendar.getInstance();
        timeBeforeDisable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
        timeBeforeEnable = Calendar.getInstance();
        timeBeforeEnable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
        timeHolidayModeOn = Calendar.getInstance();
        timeHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
        timer = new jejTimer[timerEndIndex+1];

        for (int i = 0 ; i <= timerEndIndex ; i++) {
            timer[i] = new jejTimer();
            timer[i].available = false;
            timer[i].cameraDisable = true;
            timer[i].timeInDay = INITIAL_TIME;
            timer[i].str2int();
            timer[i].beforeStart = Calendar.getInstance();
            timer[i].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            timer[i].afterStart = Calendar.getInstance();
            timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            timer[i].isSet = false;
        }
    }
}
