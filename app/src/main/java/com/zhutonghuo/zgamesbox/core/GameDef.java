package com.zhutonghuo.zgamesbox.core;

import android.content.Context;

public final class GameDef {
    public interface Factory {
        BaseGameView create(Context c);
    }

    public final String id;
    public final String name;
    public final String desc;
    public final int accent;
    public final Factory factory;

    public GameDef(String id, String name, String desc, int accent, Factory factory) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.accent = accent;
        this.factory = factory;
    }
}
