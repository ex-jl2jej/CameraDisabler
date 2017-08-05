package com.gmail.jl2jej.wor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.content.ComponentName;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Calendar;

import static com.gmail.jl2jej.wor.Globals.dateChange;

//@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CameraDisable";
    private DevicePolicyManager devicePolicyManager;
    private ComponentName tCameraReceiver;
    private Boolean tCameraActive;

    private UpdateReceiver updateReceiver;

    private static Globals ag = null;

    //このプログラムの基本構造を示しておく。
    // このプログラムは BackEndService をベースに動く
    //　MainActivity　は、UIを行うが、すべての変数は、BackEndService にある。
    // 構造は以下の通り
    //  MainActivity
    //    onCreate ------------> BackEndService#onStartCommand
    //                                    ↓
    //    updateHandler <----------- UpdateReceiver
    // updateHandler は、MainActivityの中にあるので、UIをいじることができる。

    private Handler updateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            Log.i(TAG, "updateHandler");
            int requestCode = bundle.getInt(BackEndService.REQUEST_CODE, 1);

            if (ag == null) {
                ag = new Globals();
            }

            ag.setGlobalsFromIntent(bundle);
            switch (bundle.getInt(BackEndService.COMMAND)) {
                case BackEndService.REDRAW:
                    Log.i(TAG, "updateHandler:rewriteView");
                    rewriteView();
                    break;
                case BackEndService.REDRAW_TBD:
                    Log.i(TAG, "REDRAW_TBD");
                    rewriteView(BackEndService.REDRAW_TBD);
                    break;
                case BackEndService.REDRAW_TBE:
                    Log.i(TAG, "REDRAW_TBE");
                    rewriteView(BackEndService.REDRAW_TBE);
                    break;
                case BackEndService.REDRAW_TP:
                    Log.i(TAG, "REDRAW_TP");
                    rewriteView(BackEndService.REDRAW_TP, requestCode);
                    break;
                case BackEndService.REDRAW_CBH:
                    Log.i(TAG, "REDRAW_CBH");
                    rewriteView(BackEndService.REDRAW_CBH);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate in");
        //初期値を入れた g を作っておく。
        ag = new Globals();
        ag.readSettingFile(getBaseContext());

        //ハンドラーが動くようにする
        updateReceiver = new UpdateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackEndService.REDRAW_ACTION);
        registerReceiver(updateReceiver, intentFilter);

        updateReceiver.registerHandler(updateHandler);

        //初期化のサービスを動かす
        Intent serviceIntent = new Intent(this, BackEndService.class);
        serviceIntent.putExtra("CALLED", "MainActivity");
        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.START_ACTIVITY);
        startService(serviceIntent);

        //カメラを有効無効できるようにする
        devicePolicyManager = (DevicePolicyManager)getSystemService(MainActivity.DEVICE_POLICY_SERVICE);
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
                Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                Log.i(TAG, "directSwitch changed");
                if (isChecked) {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, true);
                    serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_BEFORE_DISABLE);
                    Calendar nowTime = Calendar.getInstance();
                    String nt = Globals.dateToString(nowTime);
                    serviceIntent.putExtra(BackEndService.NOW_TIME, nt );
                    ag.timeBeforeDisable = nowTime;
                    TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
                    textBeforeDisable.setText(nt);
                    startService(serviceIntent);
                    Log.i(TAG, "true");
                } else {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, false);
                    serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_BEFORE_ENABLE);
                    Calendar nowTime = Calendar.getInstance();
                    String nt = Globals.dateToString(nowTime);
                    ag.timeBeforeEnable = nowTime;
                    serviceIntent.putExtra(BackEndService.NOW_TIME, nt);
                    TextView textBeforeEnable = (TextView) findViewById(R.id.textBeforeEnable);
                    textBeforeEnable.setText(nt);
                    startService(serviceIntent);
                    Log.i(TAG, "false");
                }
            }});

        final CheckBox cbTimer1 = (CheckBox)findViewById(R.id.checkBoxTimer1);
        cbTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "cbTimer1 Listener");
                        ag.timer[1].available = isChecked;
                        cbTimer1.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.CB_TIMER);
                        serviceIntent.putExtra(BackEndService.BOOLEAN, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 1);
                        serviceIntent.putExtra("CALLED", "CBTIMER1");
                        startService(serviceIntent);
                      }
                }
        );

        final CheckBox cbTimer2 = (CheckBox)findViewById(R.id.checkBoxTimer2);
        cbTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "cbTimer2 Listener");
                        ag.timer[2].available = isChecked;
                        cbTimer2.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.CB_TIMER);
                        serviceIntent.putExtra(BackEndService.BOOLEAN, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 2);
                        startService(serviceIntent);
                    }
                }
        );
        final CheckBox cbTimer3 = (CheckBox)findViewById(R.id.checkBoxTimer3);
        cbTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "cbTimer3 Listener");
                        cbTimer3.setChecked(isChecked);
                        ag.timer[3].available = isChecked;
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.CB_TIMER);
                        serviceIntent.putExtra(BackEndService.BOOLEAN, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 3);
                        startService(serviceIntent);
                    }
                }
        );


        final Switch swTimer1 = (Switch)findViewById(R.id.changeSwitch1);
        swTimer1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "swTimer1 Listener");
                        ag.timer[1].cameraDisable = isChecked;
                        swTimer1.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.SW_TIMER);
                        serviceIntent.putExtra(BackEndService.CAMERA_DISABLE, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 1);
                        serviceIntent.putExtra("CALLED", "swTimer1");
                        startService(serviceIntent);
                    }
                }
        );

        final Switch swTimer2 = (Switch)findViewById(R.id.changeSwitch2);
        swTimer2.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "swTimer2 Listener");
                        ag.timer[2].cameraDisable = isChecked;
                        swTimer2.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.SW_TIMER);
                        serviceIntent.putExtra(BackEndService.CAMERA_DISABLE, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 2);
                        startService(serviceIntent);
                    }
                }
        );

        final Switch swTimer3 = (Switch)findViewById(R.id.changeSwitch3);
        swTimer3.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "swTimer3 Listener");
                        ag.timer[3].cameraDisable = isChecked;
                        swTimer3.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.SW_TIMER);
                        serviceIntent.putExtra(BackEndService.CAMERA_DISABLE, isChecked);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 3);
                        startService(serviceIntent);
                    }
                }
        );

        final TimePickerDialog tpdTimer1 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "timePicker1 Listener");
                        ag.timer[1].hourOfDay = hourOfDay;
                        ag.timer[1].min = min;
                        ag.timer[1].int2str();
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_PICK );
                        serviceIntent.putExtra(BackEndService.HOUR_OF_DAY, hourOfDay);
                        serviceIntent.putExtra(BackEndService.MIN, min);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 1);
                        startService(serviceIntent);
                    }
                }, ag.timer[1].hourOfDay, ag.timer[1].min, true );
        final TextView tmTimer1 = (TextView)findViewById(R.id.textTimer1);
        tmTimer1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "tpdTimer1.show");
                        tpdTimer1.show();
                    }
                }
        );

        final TimePickerDialog tpdTimer2 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "timePicker2 Listener");
                        ag.timer[2].hourOfDay = hourOfDay;
                        ag.timer[2].min = min;
                        ag.timer[2].int2str();
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_PICK );
                        serviceIntent.putExtra(BackEndService.HOUR_OF_DAY, hourOfDay);
                        serviceIntent.putExtra(BackEndService.MIN, min);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 2);
                        startService(serviceIntent);
                    }
                }, ag.timer[2].hourOfDay, ag.timer[2].min, true);

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
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "timePicker3 Listener");
                        ag.timer[3].hourOfDay = hourOfDay;
                        ag.timer[3].min = min;
                        ag.timer[3].int2str();
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_PICK );
                        serviceIntent.putExtra(BackEndService.HOUR_OF_DAY, hourOfDay);
                        serviceIntent.putExtra(BackEndService.MIN, min);
                        serviceIntent.putExtra(BackEndService.REQUEST_CODE, 3);
                        startService(serviceIntent);
                    }
                }, ag.timer[3].hourOfDay, ag.timer[3].min, true);

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
                        Intent serviceIntent = new Intent(getBaseContext(), BackEndService.class);
                        Log.i(TAG, "cbHolidayMode Listener");
                        cbHolidayMode.setChecked(isChecked);
                        ag.timer[dateChange].available = isChecked;
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.CB_HOLIDAY);
                        serviceIntent.putExtra(BackEndService.BOOLEAN, isChecked);
                        startService(serviceIntent);
                    }
                }
        );
        Log.i(TAG, "onCreate out");
    }


    public void rewriteView() {
        if (ag == null) {
            Log.i(TAG, "rewriteView:ag == null");
        }
        Switch directSwitch = (Switch)findViewById(R.id.directSwitch);
        if (devicePolicyManager.getCameraDisabled(tCameraReceiver)) {
            directSwitch.setChecked(true);
        } else {
            directSwitch.setChecked(false);
        }

        TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
        textBeforeDisable.setText(ag.dateToString(ag.timeBeforeDisable));
        Log.i(TAG, "rewriteView:timeBeforeDisable:"+ag.dateToString(ag.timeBeforeDisable));

        TextView textBeforeEnable = (TextView)findViewById(R.id.textBeforeEnable);
        textBeforeEnable.setText(ag.dateToString(ag.timeBeforeEnable));
        Log.i(TAG, "rewriteView:timeBeforeEnable:"+ag.dateToString(ag.timeBeforeEnable));

        ((TextView)findViewById(R.id.textTimer1)).setText(ag.timer[1].timeInDay);
        ((TextView)findViewById(R.id.textTimer2)).setText(ag.timer[2].timeInDay);
        ((TextView)findViewById(R.id.textTimer3)).setText(ag.timer[3].timeInDay);

        ((TextView)findViewById(R.id.textBeforeTimer1)).setText(ag.dateToString(ag.timer[1].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer2)).setText(ag.dateToString(ag.timer[2].beforeStart));
        ((TextView)findViewById(R.id.textBeforeTimer3)).setText(ag.dateToString(ag.timer[3].beforeStart));

        ((TextView)findViewById(R.id.textAfterTimer1)).setText(ag.dateToString(ag.timer[1].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer2)).setText(ag.dateToString(ag.timer[2].afterStart));
        ((TextView)findViewById(R.id.textAfterTimer3)).setText(ag.dateToString(ag.timer[3].afterStart));

        CheckBox cb = (CheckBox)findViewById(R.id.checkBoxTimer1);
        if (cb.isChecked() != ag.timer[1].available) {
            cb.setChecked(ag.timer[1].available);
        }
        cb = (CheckBox)findViewById(R.id.checkBoxTimer2);
        if (cb.isChecked() != ag.timer[2].available) {
            cb.setChecked(ag.timer[2].available);
        }
        cb = (CheckBox)findViewById(R.id.checkBoxTimer3);
        if (cb.isChecked() != ag.timer[3].available) {
            cb.setChecked(ag.timer[3].available);
        }

        Switch sw = (Switch)findViewById(R.id.changeSwitch1);
        if (sw.isChecked() != ag.timer[1].cameraDisable) {
            Log.i(TAG, "swTimer1 different");
            sw.setChecked(ag.timer[1].cameraDisable);
        }
        sw = (Switch)findViewById(R.id.changeSwitch2);
        if (sw.isChecked() != ag.timer[2].cameraDisable) {
            Log.i(TAG, "swTimer2 different");
            sw.setChecked(ag.timer[2].cameraDisable);
        }
        sw = (Switch)findViewById(R.id.changeSwitch3);
        if (sw.isChecked() != ag.timer[3].cameraDisable) {
            Log.i(TAG, "swTimer3 different");
            sw.setChecked(ag.timer[3].cameraDisable);
        }

        cb = (CheckBox)findViewById(R.id.checkBoxHolidayMode);
        if (cb.isChecked() != ag.timer[dateChange].available) {
            cb.setChecked(ag.timer[dateChange].available);
        }
        ((TextView)findViewById(R.id.textHolidayOnTime)).setText(ag.dateToString(ag.timeHolidayModeOn));

    }
    public void rewriteView(int command, int requestCode) {
        Log.i(TAG, "rewriteView part with request code");

        switch (command) {
            case BackEndService.REDRAW_TP:
                switch (requestCode) {
                    case 1:
                        ((TextView) findViewById(R.id.textTimer1)).setText(ag.timer[1].timeInDay);
                        ((TextView)findViewById(R.id.textAfterTimer1)).setText(ag.dateToString(ag.timer[1].afterStart));
                        ((TextView)findViewById(R.id.textBeforeTimer1)).setText(ag.dateToString(ag.timer[1].beforeStart));
                        break;
                    case 2:
                        ((TextView) findViewById(R.id.textTimer2)).setText(ag.timer[2].timeInDay);
                        ((TextView)findViewById(R.id.textAfterTimer2)).setText(ag.dateToString(ag.timer[2].afterStart));
                        ((TextView)findViewById(R.id.textBeforeTimer2)).setText(ag.dateToString(ag.timer[2].beforeStart));
                        break;
                    case 3:
                        ((TextView) findViewById(R.id.textTimer3)).setText(ag.timer[3].timeInDay);
                        ((TextView)findViewById(R.id.textAfterTimer3)).setText(ag.dateToString(ag.timer[3].afterStart));
                        ((TextView)findViewById(R.id.textBeforeTimer3)).setText(ag.dateToString(ag.timer[3].beforeStart));
                        break;
                }
        }
    }

    public void rewriteView(int command) {
        Log.i(TAG, "rewriteView part");
        switch (command) {
            case BackEndService.REDRAW_TBD:
                TextView textBeforeDisable = (TextView) findViewById(R.id.textBeforeDisable);
                textBeforeDisable.setText(ag.dateToString(ag.timeBeforeDisable));
                Log.i(TAG, "rewriteViewPart:timeBeforeDisable:" + ag.dateToString(ag.timeBeforeDisable));
                break;
            case BackEndService.REDRAW_TBE:
                TextView textBeforeEnable = (TextView) findViewById(R.id.textBeforeEnable);
                textBeforeEnable.setText(ag.dateToString(ag.timeBeforeEnable));
                Log.i(TAG, "rewriteViewPart:timeBeforeEnable:" + ag.dateToString(ag.timeBeforeEnable));
                break;
            case BackEndService.REDRAW_CBH:
                ((TextView) findViewById(R.id.textHolidayOnTime)).setText(ag.dateToString(ag.timeHolidayModeOn));
                ((TextView)findViewById(R.id.textAfterTimer1)).setText(ag.dateToString(ag.timer[1].afterStart));
                ((TextView)findViewById(R.id.textAfterTimer2)).setText(ag.dateToString(ag.timer[2].afterStart));
                ((TextView)findViewById(R.id.textAfterTimer3)).setText(ag.dateToString(ag.timer[3].afterStart));
                Log.i(TAG, "rewiteView part CBH");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        rewriteView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        unregisterReceiver(updateReceiver);
        ag = null;
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
