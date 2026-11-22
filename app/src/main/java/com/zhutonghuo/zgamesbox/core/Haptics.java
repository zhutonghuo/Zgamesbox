package com.zhutonghuo.zgamesbox.core;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class Haptics {
    private final Vibrator v;

    Haptics(Context c) {
        v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void buzz(int ms) {
        if (v == null || !v.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (SecurityException ignored) {
        }
    }
}
