package com.zhutonghuo.zgamesbox.core;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;

public final class GameSfx {
    public static final int CLICK = 0;
    public static final int SCORE = 1;
    public static final int FAIL = 2;
    public static final int WIN = 3;
    public static final int TAP = 4;

    private final SoundPool pool;
    private final int[] ids = new int[5];
    private final boolean[] loaded = new boolean[5];
    private volatile boolean released;

    GameSfx(Context c) {
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(16).setAudioAttributes(attr).build();
        pool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status != 0) return;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == sampleId) loaded[i] = true;
            }
        });
        for (int i = 0; i < ids.length; i++) {
            File f = new File(c.getCacheDir(), "zg_sfx_" + i + ".wav");
            if (!prepare(f, i)) {
                ids[i] = 0;
                continue;
            }
            try {
                ids[i] = pool.load(f.getAbsolutePath(), 1);
            } catch (Exception e) {
                ids[i] = 0;
            }
        }
    }

    private static boolean prepare(File f, int type) {
        int n = 22050 * (type == WIN ? 440 : 130) / 1000;
        int expected = 44 + n * 2;
        if (!f.exists() || f.length() != expected) {
            if (f.exists()) f.delete();
            if (!writeWav(f, type) || f.length() != expected) {
                f.delete();
                return false;
            }
        }
        return true;
    }

    public void play(int which) {
        if (released) return;
        if (which < 0 || which >= ids.length) return;
        if (ids[which] == 0 || !loaded[which]) return;
        if (App.prefs() != null && App.prefs().muted()) return;
        try {
            pool.play(ids[which], 0.55f, 0.55f, 1, 0, 1f);
        } catch (IllegalStateException ignored) {
        }
    }

    public void release() {
        released = true;
        try {
            pool.release();
        } catch (Exception ignored) {
        }
    }

    private static boolean writeWav(File f, int type) {
        int rate = 22050;
        int ms = type == WIN ? 440 : 130;
        int n = rate * ms / 1000;
        byte[] pcm = new byte[n * 2];
        int[] notes = {523, 659, 784, 1047};
        for (int i = 0; i < n; i++) {
            double t = i / (double) rate;
            double env = Math.min(1.0, t / 0.008) * (1.0 - i / (double) n);
            double freq;
            if (type == WIN) {
                freq = notes[Math.min(notes.length - 1, (int) (t / 0.11))];
            } else if (type == SCORE) {
                freq = 620 + 700 * (i / (double) n);
            } else if (type == FAIL) {
                freq = 320 - 170 * (i / (double) n);
            } else if (type == TAP) {
                freq = 480;
            } else {
                freq = 880;
            }
            short s = (short) (Math.sin(2 * Math.PI * freq * t) * env * 11000);
            pcm[i * 2] = (byte) (s & 0xff);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        try (FileOutputStream out = new FileOutputStream(f)) {
            byte[] h = new byte[44];
            put(h, 0, "RIFF");
            le(h, 4, 36 + pcm.length);
            put(h, 8, "WAVEfmt ");
            le(h, 16, 16);
            h[20] = 1;
            h[22] = 1;
            le(h, 24, rate);
            le(h, 28, rate * 2);
            h[32] = 2;
            h[34] = 16;
            put(h, 36, "data");
            le(h, 40, pcm.length);
            out.write(h);
            out.write(pcm);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void put(byte[] b, int off, String s) {
        for (int i = 0; i < s.length(); i++) b[off + i] = (byte) s.charAt(i);
    }

    private static void le(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xff);
        b[off + 1] = (byte) ((v >> 8) & 0xff);
        b[off + 2] = (byte) ((v >> 16) & 0xff);
        b[off + 3] = (byte) ((v >> 24) & 0xff);
    }
}
