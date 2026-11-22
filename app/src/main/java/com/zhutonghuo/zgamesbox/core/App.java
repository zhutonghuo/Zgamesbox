package com.zhutonghuo.zgamesbox.core;

import android.content.Context;

public final class App {
    private static Prefs prefs;
    private static GameSfx sfx;
    private static Haptics haptics;

    private App() {}

    public static void init(Context c) {
        Context app = c.getApplicationContext();
        if (prefs == null) prefs = new Prefs(app);
        if (sfx == null) sfx = new GameSfx(app);
        if (haptics == null) haptics = new Haptics(app);
    }

    public static Prefs prefs() { return prefs; }
    public static GameSfx sfx() { return sfx; }
    public static Haptics haptics() { return haptics; }

    public static void dispose() {
        if (sfx != null) sfx.release();
        sfx = null;
    }
}
