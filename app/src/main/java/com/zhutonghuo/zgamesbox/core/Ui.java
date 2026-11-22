package com.zhutonghuo.zgamesbox.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public final class Ui {
    public static final int BG = 0xFF0A0E14;
    public static final int PANEL = 0xFF151B26;
    public static final int PANEL2 = 0xFF1E2635;
    public static final int PRIMARY = 0xFF6C5CE7;
    public static final int ACCENT = 0xFF06B6D4;
    public static final int TEXT = 0xFFE8EDF5;
    public static final int MUTED = 0xFF8B96A8;
    public static final int OK = 0xFF22C55E;
    public static final int BAD = 0xFFEF4444;
    public static final int GOLD = 0xFFF2A900;

    private Ui() {}

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    public static int dpi(Context c, float v) {
        return Math.round(dp(c, v));
    }

    public static Paint paint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    public static Paint text(Context c, float sp, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(dp(c, sp));
        p.setFakeBoldText(bold);
        return p;
    }

    public static void roundRect(Canvas c, RectF r, float radius, Paint p) {
        c.drawRoundRect(r, radius, radius, p);
    }
}
