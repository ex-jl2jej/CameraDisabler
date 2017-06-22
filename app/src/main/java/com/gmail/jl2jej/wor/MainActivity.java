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
import java.text.ParseException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.format.DateUtils.*;

class Timer {
    public Boolean available;
    public Boolean cameraDisable;
    public String timeInDay;
    public Date beforeStart;
    public Date afterStart;
}

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private final String TAG = "CameraDisable";
    private final String settingFileName = "setting.dat";
    DevicePolicyManager devicePolicyManager;
    ComponentName tCameraReceiver;
    Boolean tCameraActive;

    private Date timeBeforeDisable;
    private Date timeBeforeEnable;
    private final Integer timerStartIndex = 1;
    private final Integer timerEndIndex = 3;
    private final Integer dateChange = 0;
    private Timer timer[] = new Timer[timerEndIndex+1];
    private final String initialDateString = "1970/01/01 09:00:00";
    private final CharSequence dateFormat = "yyyy/MM/dd hh:mm:ss";

    protected Boolean readSettingFile() {
        BufferedReader reader = null;
        Boolean doRewriteFile = false;
        Date initialDate;
        try {
            initialDate = DateFormat.getDateInstance().parse(initialDateString);
        } catch (ParseException e) {
            e.printStackTrace();
            initialDate = null;
            System.exit(1);
        }

        for (int i = 0 ; i <= timerEndIndex ; i++) {
            timer[i] = new Timer();
        }

        try {
            reader = new BufferedReader(new InputStreamReader(openFileInput(settingFileName)));
            String line;

            if((line = reader.readLine()) != null) {
                try {
                    timeBeforeDisable = DateFormat.getDateInstance().parse(line);
                } catch (ParseException e) {
                    timeBeforeDisable = initialDate;
                    doRewriteFile = true;
                }
            } else {
                timeBeforeDisable = initialDate;
                doRewriteFile = true;
            }
            if((line = reader.readLine()) != null) {
                try {
                    timeBeforeEnable = DateFormat.getDateInstance().parse(line);
                } catch (ParseException e) {
                    timeBeforeEnable = initialDate;
                    doRewriteFile = true;
                }
            } else {
                timeBeforeEnable = initialDate;
                doRewriteFile = true;
            }
            for (int i = 1 ; i <= timerEndIndex ; i++) {
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
                        Integer h = Integer.parseInt(m.group(3));
                        Integer min = Integer.parseInt(m.group(4));
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
                        timer[i].beforeStart = DateFormat.getDateInstance().parse(line);
                    } catch (ParseException e) {
                        timer[i].beforeStart = initialDate;
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].beforeStart = initialDate;
                    doRewriteFile = true;
                }

                if ((line = reader.readLine()) != null) {
                    try {
                        timer[i].afterStart = DateFormat.getDateInstance().parse(line);
                    } catch (ParseException e) {
                        timer[i].afterStart = initialDate;
                        doRewriteFile = true;
                    }
                } else {
                    timer[i].afterStart = initialDate;
                    doRewriteFile = true;
                }

            }
            if ((line = reader.readLine()) != null) {
                if( line.equals("true")) {
                    timer[0].available = true;
                } else if (line.equals("false")) {
                    timer[0].available = false;
                } else {
                    timer[0].available = false;
                    doRewriteFile = true;
                }
            } else {
                timer[0].available = false;
                doRewriteFile = true;
            }
            if ((line = reader.readLine()) != null) {
                try {
                    timer[0].beforeStart = DateFormat.getDateInstance().parse(line);
                } catch (ParseException e) {
                    timer[0].beforeStart = initialDate;
                    doRewriteFile = true;
                }
            } else {
                timer[0].beforeStart = initialDate;
                doRewriteFile = true;
            }
        } catch (IOException e) {
            timeBeforeDisable = initialDate;
            timeBeforeEnable = initialDate;
            for (int i = 1 ; i <= timerEndIndex ; i++) {
                timer[i].available = false;
                timer[i].cameraDisable = true;
                timer[i].timeInDay = "00:00";
                timer[i].beforeStart = initialDate;
                timer[i].afterStart = initialDate;
            }
            timer[0].available = false;
            timer[0].beforeStart = initialDate;
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
            writer.write(android.text.format.DateFormat.format(dateFormat, timeBeforeDisable) + newLine);
            writer.write(android.text.format.DateFormat.format(dateFormat, timeBeforeEnable) + newLine);
            for (int i = 1 ; i <= timerEndIndex ; i++ ) {
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
                writer.write(android.text.format.DateFormat.format(dateFormat, timer[i].beforeStart) + newLine);
                writer.write(android.text.format.DateFormat.format(dateFormat, timer[i].afterStart) + newLine);
            }
            if (timer[0].available) {
                writer.write("true" + newLine);
            } else {
                writer.write("false" + newLine);
            }
            writer.write(android.text.format.DateFormat.format(dateFormat, timer[0].beforeStart) + newLine);
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
                    timeBeforeDisable = Date(System.currentTimeMillis());
                    Log.i(TAG, "true");
                } else {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, false);
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
        textBeforeDisable.setText(android.text.format.DateFormat.format(dateFormat, timeBeforeDisable));

        TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
        textBeforeEnable.setText(android.text.format.DateFormat.format(dateFormat, timeBeforeEnable));

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
