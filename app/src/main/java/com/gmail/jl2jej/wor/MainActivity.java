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

//@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CameraDisable";
    private DevicePolicyManager devicePolicyManager;
    private ComponentName tCameraReceiver;
    private Boolean tCameraActive;

    private UpdateReceiver updateReceiver;
    private IntentFilter intentFilter;

    private static Globals ag = null;
    private static Context mc;

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

            if (ag == null) {
                ag = new Globals();
            }

            ag.setGlobalsFromIntent(bundle);
            switch (bundle.getInt(BackEndService.COMMAND)) {
                case BackEndService.REDRAW:
                    Log.i(TAG, "updateHandler:rewriteView");
                    rewriteView();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初期値を入れた g を作っておく。
        ag = new Globals();
        mc = this;

        //ハンドラーが動くようにする
        updateReceiver = new UpdateReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(BackEndService.REDRAW_ACTION);
        registerReceiver(updateReceiver, intentFilter);

        updateReceiver.registerHandler(updateHandler);

        //初期化のサービスを動かす
        final Intent serviceIntent = new Intent(this, BackEndService.class);
        serviceIntent.putExtra("CALLED", "MainActivity");
        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.START_ACTIVITY);
        startService(serviceIntent);

        //カメラを有効無効できるようにする
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
                    serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_BEFORE_DISABLE);
                    String nt = Globals.dateToString(Calendar.getInstance());
                    serviceIntent.putExtra(BackEndService.NOW_TIME, nt );
                    TextView textBeforeDisable = (TextView)findViewById(R.id.textBeforeDisable);
                    textBeforeDisable.setText(nt);
                    startService(serviceIntent);
                    Log.i(TAG, "true");
                } else {
                    devicePolicyManager.setCameraDisabled(tCameraReceiver, false);
                    serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.TIME_BEFORE_ENABLE);
                    String nt = Globals.dateToString(Calendar.getInstance());
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
                        cbTimer3.setChecked(isChecked);
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
                        tpdTimer1.show();
                    }
                }
        );

        final TimePickerDialog tpdTimer2 = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int min) {
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
                        cbHolidayMode.setChecked(isChecked);
                        serviceIntent.putExtra(BackEndService.COMMAND, BackEndService.CB_HOLIDAY);
                        serviceIntent.putExtra(BackEndService.BOOLEAN, isChecked);
                        startService(serviceIntent);
                    }
                }
        );
        Log.i(TAG, "onCreate out");
    }

    public void rewriteView() {
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

        ((CheckBox)findViewById(R.id.checkBoxTimer1)).setChecked(ag.timer[1].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer2)).setChecked(ag.timer[2].available);
        ((CheckBox)findViewById(R.id.checkBoxTimer3)).setChecked(ag.timer[3].available);

        ((Switch)findViewById(R.id.changeSwitch1)).setChecked(ag.timer[1].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch2)).setChecked(ag.timer[2].cameraDisable);
        ((Switch)findViewById(R.id.changeSwitch3)).setChecked(ag.timer[3].cameraDisable);

        ((CheckBox)findViewById(R.id.checkBoxHolidayMode)).setChecked(ag.timer[Globals.dateChange].available);
        ((TextView)findViewById(R.id.textHolidayOnTime)).setText(ag.dateToString(ag.timeHolidayModeOn));

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
