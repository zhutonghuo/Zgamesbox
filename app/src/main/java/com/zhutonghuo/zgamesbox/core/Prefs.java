package com.zhutonghuo.zgamesbox.core;

import android.content.SharedPreferences;

public final class Prefs {
    private final SharedPreferences sp;

    Prefs(android.content.Context c) {
        sp = c.getSharedPreferences("zgamesbox", android.content.Context.MODE_PRIVATE);
    }

    public int getInt(String key, int def) { return sp.getInt(key, def); }
    public void setInt(String key, int value) { sp.edit().putInt(key, value).apply(); }
    public boolean getBool(String key, boolean def) { return sp.getBoolean(key, def); }
    public void setBool(String key, boolean value) { sp.edit().putBoolean(key, value).apply(); }

    public int best(String gameId) { return getInt("best_" + gameId, 0); }
    public void saveBest(String gameId, int score) {
        if (score > best(gameId)) setInt("best_" + gameId, score);
    }

    public int wins(String gameId) { return getInt("win_" + gameId, 0); }
    public void addWin(String gameId) { setInt("win_" + gameId, wins(gameId) + 1); }

    public boolean muted() { return getBool("muted", false); }
    public void setMuted(boolean v) { setBool("muted", v); }
}
