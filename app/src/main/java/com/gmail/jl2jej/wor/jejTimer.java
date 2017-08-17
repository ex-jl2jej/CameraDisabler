package com.gmail.jl2jej.wor;

import android.util.Log;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kido on 2017/07/10.
 *
 */

public class jejTimer {
    protected Boolean available;
    protected Boolean cameraDisable;
    protected String timeInDay;
    protected int hourOfDay;
    protected int min;
    protected Calendar beforeStart;
    protected Calendar afterStart;
    protected Boolean isSet;
    private static final String TAG = "jejTimer";

    protected void str2int() { //書き換えたかどうかを返す
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

    protected void int2str() { // 書き換えたかどうかを返す
        this.timeInDay = String.format("%02d:%02d", this.hourOfDay, this.min);
    }
}
