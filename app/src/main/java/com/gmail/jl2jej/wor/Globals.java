package com.gmail.jl2jej.wor;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gmail.jl2jej.wor.AlarmBroadcastReceiver.CAMERA_DISABLE;
import static com.gmail.jl2jej.wor.AlarmBroadcastReceiver.RCODE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Created by kido on 2017/07/10.
 */

public class Globals extends Application {
    // 日付の初期値
    private final int INIT_YEAR = 1970;
    private final int INIT_MONTH = 1;
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

    protected Context that;
    protected Calendar timeBeforeDisable;
    protected Calendar timeBeforeEnable;
    protected Calendar timeHolidayModeOn;
    protected final int timerStartIndex = 1;
    protected final int timerEndIndex = 3;
    public static final int dateChange = 0;
    protected jejTimer timer[];


    protected void setTimer(Context context, Boolean cameraDisable, int requestCode, Calendar calendar) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra(CAMERA_DISABLE, cameraDisable);
        intent.putExtra(RCODE, requestCode);
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

    protected Calendar makeTargetTime(int requestCode) {
        Calendar nowTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        if (timer[dateChange].available
                || ( (timer[requestCode].hourOfDay*60+timer[requestCode].min) <= (nowTime.get(Calendar.HOUR_OF_DAY)*60+nowTime.get(MINUTE)))) {
            int nowDay = targetTime.get(DAY_OF_MONTH);
            targetTime.set(DAY_OF_MONTH, nowDay + 1);
            //Toast.makeText(that, String.format("%d:%d %d:%d", timer[requestCode].hourOfDay, timer[requestCode].min,
            //        nowTime.get(Calendar.HOUR_OF_DAY), nowTime.get(MINUTE)), Toast.LENGTH_LONG).show();

        }
        targetTime.set(Calendar.HOUR_OF_DAY, timer[requestCode].hourOfDay);
        targetTime.set(MINUTE, timer[requestCode].min);
        targetTime.set(SECOND, 0);

        return targetTime;
    }

    protected void setNormalTimer(Context context, int requestCode) {
        Calendar targetTime = makeTargetTime(requestCode);

        if (timer[requestCode].available == false && timer[requestCode].isSet == true ) {
            cancelTimer(context, requestCode);
        } else if (timer[requestCode].available == true) {
            if (timer[requestCode].isSet == true) { // 設定されている
                cancelTimer(context, requestCode);
            }
            setTimer(context, timer[requestCode].cameraDisable, requestCode, targetTime);
            timer[requestCode].isSet = true;
        }
        timer[requestCode].afterStart = targetTime;
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

    protected Boolean readSettingFile(Context context) {
        BufferedReader reader = null;
        Boolean doRewriteFile = false;

        try {
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(settingFileName)));
            String line;

            if((line = reader.readLine()) != null) {
                try {
                    timeBeforeDisable = parseDateString(line);
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
                    timeBeforeEnable = parseDateString(line);
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
                            timer[i].available = true;
                        } else {
                            timer[i].available = false;
                        }
                        if (m.group(2).equals("disable")) {
                            timer[i].cameraDisable = true;
                        } else {
                            timer[i].cameraDisable = false;
                        }
                        int h = Integer.parseInt(m.group(3));
                        int min = Integer.parseInt(m.group(4));
                        if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                            timer[i].hourOfDay = h;
                            timer[i].min = min;
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
                        timer[i].beforeStart = parseDateString(line);
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
                            timer[i].isSet = true;
                        } else {
                            timer[i].isSet = false;
                        }
                        timer[i].afterStart = parseDateString(line);
                    } catch (IllegalArgumentException e) {
                        timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }

            }

            if ((line = reader.readLine()) != null) {
                try {
                    timeHolidayModeOn = parseDateString(line);
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
        return doRewriteFile;
    }

    protected void rewriteSettingFile(Context context) {
        BufferedWriter writer = null;
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
                if( timer[1].cameraDisable) {
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
    }

    public Intent getIntentFromGlobals(Intent globalsIntent) {
        globalsIntent.putExtra("timeBeforeDisable", dateToString(timeBeforeDisable));
        return globalsIntent;
    }

    public Globals(Context context) {
        timeBeforeDisable = Calendar.getInstance();
        timeBeforeEnable = Calendar.getInstance();
        timeHolidayModeOn = Calendar.getInstance();
        timer = new jejTimer[timerEndIndex+1];
        that = context;
        for (int i = 0 ; i <= timerEndIndex ; i++) {
            timer[i] = new jejTimer();
            timer[i].available = false;
            timer[i].cameraDisable = true;
            timer[i].timeInDay = INITIAL_TIME;
            timer[i].str2int();
            timer[i].beforeStart = Calendar.getInstance();
            timer[i].afterStart = Calendar.getInstance();
            timer[i].isSet = false;
        }
    }

    public Globals() {

    }
}
