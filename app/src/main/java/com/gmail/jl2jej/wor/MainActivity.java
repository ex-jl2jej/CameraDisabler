package com.gmail.jl2jej.wor;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ComponentName;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;
import android.widget.TextView;

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
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

class Timer {
    public Boolean available;
    public Boolean cameraDisable;
    public String timeInDay;
    public Calendar beforeStart;
    public Calendar afterStart;
}

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private final String TAG = "CameraDisable";
    private final String settingFileName = "setting.dat";
    DevicePolicyManager devicePolicyManager;
    ComponentName tCameraReceiver;
    Boolean tCameraActive;

    private Calendar timeBeforeDisable = Calendar.getInstance();
    private Calendar timeBeforeEnable = Calendar.getInstance();
    private final int timerStartIndex = 1;
    private final int timerEndIndex = 3;
    private final int dateChange = 0;
    private Timer timer[] = new Timer[timerEndIndex+1];
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

    protected Calendar parseDateString(String str) {
        Calendar date = Calendar.getInstance();
        String regex = "(\\d+)/(\\d+)/(\\d+)\\s+(\\d+):(\\d+):(\\d+)$";
        Pattern ptn = Pattern.compile(regex);

        Matcher m = ptn.matcher(str);
        if (m.find()) {
            int year = Integer.parseInt(m.group(YEAR_POS));
            int month = Integer.parseInt(m.group(MONTH_POS));
            int day = Integer.parseInt(m.group(DATE_POS));

            int hour = Integer.parseInt(m.group(HOUR_POS));
            int min = Integer.parseInt(m.group(MIN_POS));
            int sec = Integer.parseInt(m.group(SEC_POS));
            date.set(year, month,day,hour,min,sec);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("日付の形式がおかしい {0}", str.toString()));
        }
        return date;
    }

    protected String dateToString(Calendar date)  {
        String str = String.format("%04d/%02d/%02d %02d:%02d:%02d", date.get(YEAR), date.get(MONTH), date.get(DATE), date.get(HOUR), date.get(MINUTE), date.get(SECOND));

        return str;
    }

    protected Boolean readSettingFile() {
        BufferedReader reader = null;
        Boolean doRewriteFile = false;

        for (int i = 0 ; i <= timerEndIndex ; i++) {
            timer[i] = new Timer();
            timer[i].available = false;
            timer[i].cameraDisable = true;
            timer[i].timeInDay = new String(INITIAL_TIME);
            timer[i].beforeStart = Calendar.getInstance();
            timer[i].afterStart = Calendar.getInstance();
        }

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
            for (int i = timerStartIndex ; i <= timerEndIndex ; i++) {
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
                            timer[i].timeInDay = m.group(3) + ":" + m.group(4);
                        } else {
                            timer[i].timeInDay = "00:00";
                            doRewriteFile = true;
                        }
                    } else {
                        timer[i].available = false;
                        timer[i].cameraDisable = true;
                        timer[i].timeInDay = "00:00";
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].available = false;
                    timer[i].cameraDisable = true;
                    timer[i].timeInDay = "00:00";
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
                if( line.equals("true")) {
                    timer[dateChange].available = true;
                } else if (line.equals("false")) {
                    timer[dateChange].available = false;
                } else {
                    timer[dateChange].available = false;
                    doRewriteFile = true;
                }
            } else {
                timer[dateChange].available = false;
                doRewriteFile = true;
            }
            if ((line = reader.readLine()) != null) {
                try {
                    timer[dateChange].beforeStart = parseDateString(line);
                } catch (IllegalArgumentException e) {
                    timer[dateChange].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                    doRewriteFile = true;
                }
            } else {
                timer[dateChange].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                doRewriteFile = true;
            }
        } catch (IOException e) {
            timeBeforeDisable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            timeBeforeEnable.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            for (int i = timerStartIndex ; i <= timerEndIndex ; i++) {
                timer[i].available = false;
                timer[i].cameraDisable = true;
                timer[i].timeInDay = "00:00";
                timer[i].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                timer[i].afterStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
            }
            timer[dateChange].available = false;
            timer[dateChange].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
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
            for (int i = timerStartIndex ; i <= timerEndIndex ; i++ ) {
                if (timer[i].available) {
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
                writer.write(dateToString(timer[i].afterStart) + newLine);
            }
            if (timer[dateChange].available) {
                writer.write("true" + newLine);
            } else {
                writer.write("false" + newLine);
            }
            writer.write(dateToString(timer[dateChange].beforeStart) + newLine);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager)getSystemService(this.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(this, cameraReceiver.class);

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

        TextView textTimer1 = (TextView)findViewById(R.id.textTimer1);
        textTimer1.setText(timer[1].timeInDay);
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
