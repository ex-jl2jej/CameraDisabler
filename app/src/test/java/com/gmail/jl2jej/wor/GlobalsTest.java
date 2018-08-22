package com.gmail.jl2jej.wor;

import android.icu.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by kido on 2018/04/22.
 */
public class GlobalsTest extends Globals {
    private GlobalsTest g;
    private java.util.Calendar ret;
    private java.util.Calendar ex;

    @Test
    public void testmakeTargetTime() throws Exception {
        ret = g.makeTargetTime(dateChange);
        ex  = java.util.Calendar.getInstance();
        assertNotEquals(ret.get(java.util.Calendar.MINUTE), ex.get(java.util.Calendar.MINUTE));
    }

}