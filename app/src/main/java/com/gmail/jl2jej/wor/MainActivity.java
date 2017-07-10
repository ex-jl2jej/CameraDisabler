package com.gmail.jl2jej.wor;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ComponentName;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

class Timer {
    protected Boolean available;
    protected Boolean cameraDisable;
    protected String timeInDay;
    protected int hourOfDay;
    protected int min;
    protected Calendar beforeStart;
    protected Calendar afterStart;
    protected Boolean isSet;

    protected void str2int() {
        String regex = "(\\d\\d):(\\d\\d)$";
        Pattern ptn = Pattern.compile(regex);

        Matcher m = ptn.matcher(this.timeInDay);
        if (m.find()) {
            this.hourOfDay = Integer.parseInt(m.group(1));
            this.min = Integer.parseInt(m.group(2));
        } else {
            this.hourOfDay = 0;
            this.min = 0;
            this.timeInDay = "00:00";
        }
    }

    protected void int2str() {
        this.timeInDay = String.format("%02d:%02d", this.hourOfDay, this.min);
    }
}

//@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private final String TAG = "CameraDisable";
    public final String KEYWORD = "CAMERA_DISABLE";
    public final String RCODE = "RCODE";
    private final String settingFileName = "setting.dat";
    protected DevicePolicyManager devicePolicyManager;
    protected ComponentName tCameraReceiver;
    private Boolean tCameraActive;

    protected Calendar timeBeforeDisable = Calendar.getInstance();
    protected Calendar timeBeforeEnable = Calendar.getInstance();
    private Calendar timeHolidayModeOn = Calendar.getInstance();
    private final int timerStartIndex = 1;
    private final int timerEndIndex = 3;
    private final int dateChange = 0;
    private Timer timer[] = new Timer[timerEndIndex+1];
    private Calendar timerHolidayModeOn = Calendar.getInstance();
    private final String INITIAL_TIME = "00:00";
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

    Globals globals;

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

    protected Boolean readSettingFile() {
        BufferedReader reader = null;
        Boolean doRewriteFile = false;

        try {
            reader = new BufferedReader(new InputStreamReader(openFileInput(settingFileName)));
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
                    timerHolidayModeOn = parseDateString(line);
                } catch (IllegalArgumentException e) {
                    timeHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }
            } else {
                timerHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
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
            timerHolidayModeOn.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
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

    protected void rewriteSettingFile() {
        BufferedWriter writer = null;
        try {
            String newLine = System.getProperty("line.separator");
            writer = new BufferedWriter(new OutputStreamWriter(openFileOutput(settingFileName, Context.MODE_PRIVATE)));
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

    protected void setTimer(Boolean cameraDisable, int requestCode, Calendar calendar) {
        Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
        intent.putExtra(KEYWORD, cameraDisable);
        intent.putExtra(RCODE, requestCode);
        //intent.putExtra("TEXTVIEW", findViewById(R.id.textBeforeDisable));
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    public void cancelTimer(int requestCode) {
        Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    protected Calendar makeTargetTime(int requestCode) {
        Calendar nowTime = Calendar.getInstance();
        Calendar targetTime = Calendar.getInstance();

        if (timer[dateChange].available
                || ( (timer[requestCode].hourOfDay*60+timer[requestCode].min) <= (nowTime.get(Calendar.HOUR_OF_DAY)*60+nowTime.get(MINUTE)))) {
            int nowDay = targetTime.get(DAY_OF_MONTH);
            targetTime.set(DAY_OF_MONTH, nowDay + 1);
        }
        targetTime.set(Calendar.HOUR_OF_DAY, timer[requestCode].hourOfDay);
        targetTime.set(MINUTE, timer[requestCode].min);
        targetTime.set(SECOND, 0);

        return targetTime;
    }

    protected void setNormalTimer(int requestCode) {
        Calendar targetTime = makeTargetTime(requestCode);

        if (timer[requestCode].available == false && timer[requestCode].isSet == true ) {
            cancelTimer(requestCode);
        } else if (timer[requestCode].available == true) {
            if (timer[requestCode].isSet == true) { // 設定されている
                cancelTimer(requestCode);
            }
            setTimer(timer[requestCode].cameraDisable, requestCode, targetTime);
        }
        timer[requestCode].afterStart = targetTime;
        rewriteView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0 ; i <= timerEndIndex ; i++) {
            timer[i] = new Timer();
            timer[i].available = false;
            timer[i].cameraDisable = true;
            timer[i].timeInDay = INITIAL_TIME;
            timer[i].str2int();
            timer[i].beforeStart = Calendar.getInstance();
            timer[i].afterStart = Calendar.getInstance();
            timer[i].isSet = false;
        }

        devicePolicyManager = (DevicePolicyManager)getSystemService(this.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(this, CameraReceiver.class);

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, tCameraReceiver);
        startActivityForResult(intent, 1);

        tCameraActive = devicePolicyManager.isAdminActive(tCameraReceiver);

        Switch directSwitch = (Switch)findViewById(R.id.directSwitch);
        if (devicePolicyManager.getCameraDisabled(tCameraReceiver)) {
            directSwitch.setChecked(true);
        } else {
            directSwitch.setChecked(false);
        }

        directSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked ) {
                if (isChecked) {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, true);
                    timeBeforeDisable = Calendar.getInstance();
                    TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
                    textBeforeDisable.setText(dateToString(timeBeforeDisable));
                    rewriteSettingFile();
                    Log.i(TAG, "true");
                } else {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, false);
                    timeBeforeEnable = Calendar.getInstance();
                    TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
                    textBeforeEnable.setText(dateToString(timeBeforeEnable));
                    rewriteSettingFile();
                    Log.i(TAG, "false");
                }
            }});

        if ( readSettingFile() ) {
            rewriteSettingFile();
        }

        rewriteView();

        final CheckBox cbTimer1 = (CheckBox)findViewById(R.id.checkBoxTimer1);
        cbTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer1.setChecked(isChecked);
                        timer[1].available = isChecked;
                        if (isChecked) {
                            setNormalTimer(1);
                        }
                        rewriteSettingFile();
                    }
                }
        );

        final CheckBox cbTimer2 = (CheckBox)findViewById(R.id.checkBoxTimer2);
        cbTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer2.setChecked(isChecked);
                        timer[2].available = isChecked;
                        rewriteSettingFile();
                    }
                }
        );

        final CheckBox cbTimer3 = (CheckBox)findViewById(R.id.checkBoxTimer3);
        cbTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer3.setChecked(isChecked);
                        timer[3].available = isChecked;
                        rewriteSettingFile();
                    }
                }
        );

        final Switch swTimer1 = (Switch)findViewById(R.id.changeSwitch1);
        swTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer1.setChecked(isChecked);
                        timer[1].cameraDisable = isChecked;
                        timer[1].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        rewriteSettingFile();
                    }
                }
        );

        final Switch swTimer2 = (Switch)findViewById(R.id.changeSwitch2);
        swTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer2.setChecked(isChecked);
                        timer[2].cameraDisable = isChecked;
                        timer[2].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        rewriteSettingFile();
                    }
                }
        );

        final Switch swTimer3 = (Switch)findViewById(R.id.changeSwitch3);
        swTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer3.setChecked(isChecked);
                        timer[3].cameraDisable = isChecked;
                        timer[3].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        rewriteSettingFile();
                    }
                }
        );

        final TimePickerDialog tpdTimer1 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        timer[1].hourOfDay = hourOfDay;
                        timer[1].min = min;
                        timer[1].int2str();
                        ((TextView)findViewById(R.id.textTimer1)).setText(timer[1].timeInDay);
                        rewriteSettingFile();
                    }
                }, timer[1].hourOfDay, timer[1].min, true);

        final TextView tmTimer1 = (TextView)findViewById(R.id.textTimer1);
        tmTimer1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tpdTimer1.show();
                    }
                }
        );

        final TimePickerDialog tpdTimer2 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        timer[2].hourOfDay = hourOfDay;
                        timer[2].min = min;
                        timer[2].int2str();
                        ((TextView)findViewById(R.id.textTimer2)).setText(timer[2].timeInDay);
                        rewriteSettingFile();
                    }
                }, timer[2].hourOfDay, timer[2].min, true);

        final TextView tmTimer2 = (TextView)findViewById(R.id.textTimer2);
        tmTimer2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tpdTimer2.show();
                    }
                }
        );

        final TimePickerDialog tpdTimer3 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        timer[3].hourOfDay = hourOfDay;
                        timer[3].min = min;
                        timer[3].int2str();
                        ((TextView)findViewById(R.id.textTimer3)).setText(timer[3].timeInDay);
                        rewriteSettingFile();
                    }
                }, timer[3].hourOfDay, timer[3].min, true);

        final TextView tmTimer3 = (TextView)findViewById(R.id.textTimer3);
        tmTimer3.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tpdTimer3.show();
                    }
                }
        );

        final CheckBox cbHolidayMode = (CheckBox)findViewById(R.id.checkBoxHolidayMode);
        cbHolidayMode.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbHolidayMode.setChecked(isChecked);
                        timer[dateChange].available = isChecked;
                        if (isChecked) {
                            timeHolidayModeOn = Calendar.getInstance();
                        }
                        rewriteSettingFile();
                        rewriteView();
                    }
                }
        );
    }

    protected void rewriteView() {
        Switch directSwitch = (Switch)findViewById(R.id.directSwitch);
        if (devicePolicyManager.getCameraDisabled(tCameraReceiver)) {
            directSwitch.setChecked(true);
        } else {
            directSwitch.setChecked(false);
        }

        TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
        textBeforeDisable.setText(dateToString(timeBeforeDisable));

        TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
        textBeforeEnable.setText(dateToString(timeBeforeEnable));

        ((TextView)findViewById(R.id.textTimer1)).setText(timer[1].timeInDay);
        ((TextView)findViewById(R.id.textTimer2)).setText(timer[2].timeInDay);
        ((TextView)findViewById(R.id.textTimer3)).setText(timer[3].timeInDay);

        ((TextView)findViewById(R.id.textBeforeTimer1)).setText(dateToString(timer[1].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer2)).setText(dateToString(timer[2].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer3)).setText(dateToString(timer[3].beforeStart));

        ((TextView)findViewById(R.id.textAfterTimer1)).setText(dateToString(timer[1].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer2)).setText(dateToString(timer[2].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer3)).setText(dateToString(timer[3].afterStart));

        ((CheckBox)findViewById(R.id.checkBoxTimer1)).setChecked(timer[1].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer2)).setChecked(timer[2].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer3)).setChecked(timer[3].available);

        ((Switch)findViewById(R.id.changeSwitch1)).setChecked(timer[1].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch2)).setChecked(timer[2].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch3)).setChecked(timer[3].cameraDisable);

        ((CheckBox)findViewById(R.id.checkBoxHolidayMode)).setChecked(timer[dateChange].available);
        ((TextView)findViewById(R.id.textHolidayOnTime)).setText(dateToString(timeHolidayModeOn));

    }

    @Override
    protected void onResume() {
        super.onResume();

        rewriteView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Administration enabled!");
                } else {
                    Log.i(TAG, "Administration enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

}
