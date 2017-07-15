package com.gmail.jl2jej.wor;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
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

import static com.gmail.jl2jej.wor.AlarmBroadcastReceiver.CAMERA_DISABLE;
import static com.gmail.jl2jej.wor.AlarmBroadcastReceiver.RCODE;
import static com.gmail.jl2jej.wor.Globals.dateChange;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;


//@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "CameraDisable";
    private DevicePolicyManager devicePolicyManager;
    private ComponentName tCameraReceiver;
    private Boolean tCameraActive;

    public Globals g;
    public Context mc;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        g = new Globals(this);
        mc = this;

        devicePolicyManager = (DevicePolicyManager)getSystemService(this.DEVICE_POLICY_SERVICE);
        tCameraReceiver = new ComponentName(this, CameraReceiver.class);

        tCameraActive = devicePolicyManager.isAdminActive(tCameraReceiver);

        if (tCameraActive == false) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, tCameraReceiver);
            startActivityForResult(intent, 1);
        }

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
                    g.timeBeforeDisable = Calendar.getInstance();
                    TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
                    textBeforeDisable.setText(g.dateToString(g.timeBeforeDisable));
                    g.rewriteSettingFile(mc);
                    Log.i(TAG, "true");
                } else {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, false);
                    g.timeBeforeEnable = Calendar.getInstance();
                    TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
                    textBeforeEnable.setText(g.dateToString(g.timeBeforeEnable));
                    g.rewriteSettingFile(mc);
                    Log.i(TAG, "false");
                }
            }});

        if ( g.readSettingFile(mc) ) {
            g.rewriteSettingFile(mc);
        }

        rewriteView();

        final CheckBox cbTimer1 = (CheckBox)findViewById(R.id.checkBoxTimer1);
        cbTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer1.setChecked(isChecked);
                        g.timer[1].available = isChecked;
                        if (isChecked) {
                            g.setNormalTimer(mc, 1);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final CheckBox cbTimer2 = (CheckBox)findViewById(R.id.checkBoxTimer2);
        cbTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer2.setChecked(isChecked);
                        g.timer[2].available = isChecked;
                        if (isChecked) {
                            g.setNormalTimer(mc, 2);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final CheckBox cbTimer3 = (CheckBox)findViewById(R.id.checkBoxTimer3);
        cbTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        cbTimer3.setChecked(isChecked);
                        g.timer[3].available = isChecked;
                        if (isChecked) {
                            g.setNormalTimer(mc, 3);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final Switch swTimer1 = (Switch)findViewById(R.id.changeSwitch1);
        swTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer1.setChecked(isChecked);
                        g.timer[1].cameraDisable = isChecked;
                        g.timer[1].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        if (g.timer[1].available == true) {
                            g.setNormalTimer(mc, 1);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final Switch swTimer2 = (Switch)findViewById(R.id.changeSwitch2);
        swTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer2.setChecked(isChecked);
                        g.timer[2].cameraDisable = isChecked;
                        g.timer[2].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        if (g.timer[2].available == true) {
                            g.setNormalTimer(mc, 2);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final Switch swTimer3 = (Switch)findViewById(R.id.changeSwitch3);
        swTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        swTimer3.setChecked(isChecked);
                        g.timer[3].cameraDisable = isChecked;
                        g.timer[3].beforeStart.set(INIT_YEAR, INIT_MONTH, INIT_DAY, INIT_HOUR, INIT_MIN, INIT_SEC);
                        if (g.timer[3].available == true) {
                            g.setNormalTimer(mc, 3);
                        }
                        g.rewriteSettingFile(mc);
                    }
                }
        );

        final TimePickerDialog tpdTimer1 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        g.timer[1].hourOfDay = hourOfDay;
                        g.timer[1].min = min;
                        g.timer[1].int2str();
                        ((TextView)findViewById(R.id.textTimer1)).setText(g.timer[1].timeInDay);
                        g.rewriteSettingFile(mc);
                    }
                }, g.timer[1].hourOfDay, g.timer[1].min, true);

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
                        g.timer[2].hourOfDay = hourOfDay;
                        g.timer[2].min = min;
                        g.timer[2].int2str();
                        ((TextView)findViewById(R.id.textTimer2)).setText(g.timer[2].timeInDay);
                        g.rewriteSettingFile(mc);
                    }
                }, g.timer[2].hourOfDay, g.timer[2].min, true);

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
                        g.timer[3].hourOfDay = hourOfDay;
                        g.timer[3].min = min;
                        g.timer[3].int2str();
                        ((TextView)findViewById(R.id.textTimer3)).setText(g.timer[3].timeInDay);
                        g.rewriteSettingFile(mc);
                    }
                }, g.timer[3].hourOfDay, g.timer[3].min, true);

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
                        g.timer[dateChange].available = isChecked;
                        if (isChecked) {
                            g.timeHolidayModeOn = Calendar.getInstance();
                        }
                        g.rewriteSettingFile(mc);
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
        textBeforeDisable.setText(g.dateToString(g.timeBeforeDisable));

        TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
        textBeforeEnable.setText(g.dateToString(g.timeBeforeEnable));

        ((TextView)findViewById(R.id.textTimer1)).setText(g.timer[1].timeInDay);
        ((TextView)findViewById(R.id.textTimer2)).setText(g.timer[2].timeInDay);
        ((TextView)findViewById(R.id.textTimer3)).setText(g.timer[3].timeInDay);

        ((TextView)findViewById(R.id.textBeforeTimer1)).setText(g.dateToString(g.timer[1].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer2)).setText(g.dateToString(g.timer[2].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer3)).setText(g.dateToString(g.timer[3].beforeStart));

        ((TextView)findViewById(R.id.textAfterTimer1)).setText(g.dateToString(g.timer[1].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer2)).setText(g.dateToString(g.timer[2].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer3)).setText(g.dateToString(g.timer[3].afterStart));

        ((CheckBox)findViewById(R.id.checkBoxTimer1)).setChecked(g.timer[1].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer2)).setChecked(g.timer[2].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer3)).setChecked(g.timer[3].available);

        ((Switch)findViewById(R.id.changeSwitch1)).setChecked(g.timer[1].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch2)).setChecked(g.timer[2].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch3)).setChecked(g.timer[3].cameraDisable);

        ((CheckBox)findViewById(R.id.checkBoxHolidayMode)).setChecked(g.timer[dateChange].available);
        ((TextView)findViewById(R.id.textHolidayOnTime)).setText(g.dateToString(g.timeHolidayModeOn));

    }

    @Override
    protected void onResume() {
        super.onResume();
        g.readSettingFile(mc);
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
