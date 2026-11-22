package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MemoryView extends BaseGameView {
    private static final int N = 4;
    private static final int[] MARKS = {0xFFEF4444, 0xFFF97316, 0xFFF2A900, 0xFF22C55E,
            0xFF06B6D4, 0xFF3B82F6, 0xFFA855F7, 0xFFEC4899};
    private final int[] values = new int[N * N];
    private final int[] states = new int[N * N];
    private final float[] flip = new float[N * N];
    private final List<Integer> deck = new ArrayList<>();
    private final RectF card = new RectF();
    private final Random rnd = new Random();
    private int first = -1, second = -1, moves, matched;
    private boolean locked, anim;
    private long started;

    public MemoryView(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "memory"; }

    @Override
    protected boolean continuous() { return anim; }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        deck.clear();
        for (int i = 0; i < 8; i++) { deck.add(i); deck.add(i); }
        Collections.shuffle(deck, rnd);
        for (int i = 0; i < N * N; i++) {
            values[i] = deck.get(i);
            states[i] = 0;
            flip[i] = 0;
        }
        first = -1;
        second = -1;
        moves = 0;
        matched = 0;
        locked = false;
        anim = false;
        started = now();
    }

    @Override
    protected void update(float dt, long now) {
        if (!anim) return;
        boolean moving = false;
        for (int i = 0; i < N * N; i++) {
            float target = (states[i] == 0) ? 0f : 1f;
            if (flip[i] < target) {
                flip[i] = Math.min(target, flip[i] + dt * 5f);
                moving = true;
            } else if (flip[i] > target) {
                flip[i] = Math.max(target, flip[i] - dt * 5f);
                moving = true;
            }
        }
        anim = moving;
    }

    private float cardSize() {
        return Math.min((field.width() - dp(30)) / N, (field.height() - dp(50)) / N);
    }

    private float originX() { return field.left + (field.width() - cardSize() * N) / 2; }
    private float originY() { return field.top + dp(18); }

    @Override
    protected void drawGame(Canvas c) {
        int seconds = (int) ((now() - started) / 1000);
        header(c, "记忆翻牌", "步数 " + moves + " · " + seconds + "s", 0xFFEC4899);
        float s = cardSize();
        for (int i = 0; i < N * N; i++) {
            int row = i / N, col = i % N;
            card.set(originX() + col * s + dp(3), originY() + row * s + dp(3),
                    originX() + (col + 1) * s - dp(3), originY() + (row + 1) * s - dp(3));
            paint.setColor(states[i] == 2 ? Ui.PANEL : Ui.PANEL2);
            c.drawRoundRect(card, dp(10), dp(10), paint);
            if (states[i] == 2) {
                paint.setColor(MARKS[values[i]]);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.42f);
                c.drawText("✓", card.centerX(), card.centerY() + s * 0.15f, paint);
                paint.setFakeBoldText(false);
                continue;
            }
            if (flip[i] > 0.5f) {
                paint.setColor(MARKS[values[i]] & 0x33FFFFFF | 0x88000000);
                c.drawRoundRect(card, dp(10), dp(10), paint);
                paint.setColor(MARKS[values[i]]);
                c.drawCircle(card.centerX(), card.centerY(), s * 0.25f, paint);
                paint.setColor(0xFFFFFFFF);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.24f);
                c.drawText(String.valueOf(values[i] + 1), card.centerX(), card.centerY() + s * 0.09f, paint);
                paint.setFakeBoldText(false);
            } else {
                paint.setColor(Ui.PRIMARY);
                c.drawRoundRect(card, dp(10), dp(10), paint);
                paint.setColor(0xFFFFFFFF);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(s * 0.34f);
                c.drawText("?", card.centerX(), card.centerY() + s * 0.12f, paint);
            }
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(13));
        paint.setColor(Ui.MUTED);
        c.drawText("点开两张相同的牌 · 已配对 " + matched + "/8", W / 2f, H - dp(16), paint);
        paint.setTextAlign(Paint.Align.LEFT);

        if (matched == 8) {
            int score = Math.max(10, 1200 - seconds * 8 - moves * 5);
            saveBest(score);
            paint.setColor(0xCC0A0E14);
            c.drawRect(0, dp(62), W, H, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(28));
            paint.setFakeBoldText(true);
            paint.setColor(Ui.OK);
            c.drawText("全部配对！", W / 2f, H / 2f - dp(20), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(14));
            paint.setColor(Ui.TEXT);
            c.drawText("得分 " + score, W / 2f, H / 2f + dp(18), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return;
        if (matched == 8) { reset(); return; }
        if (locked) return;
        float s = cardSize();
        int col = (int) ((e.getX() - originX()) / s);
        int row = (int) ((e.getY() - originY()) / s);
        if (col < 0 || col >= N || row < 0 || row >= N) return;
        int idx = row * N + col;
        if (states[idx] != 0) return;
        states[idx] = 1;
        anim = true;
        play(GameSfx.TAP);
        if (first < 0) {
            first = idx;
        } else {
            second = idx;
            moves++;
            locked = true;
            postGame(() -> {
                if (values[first] == values[second]) {
                    states[first] = 2;
                    states[second] = 2;
                    matched++;
                    play(GameSfx.SCORE);
                    buzz(20);
                    if (matched == 8) {
                        int seconds = (int) ((now() - started) / 1000);
                        int score = Math.max(10, 1200 - seconds * 8 - moves * 5);
                        showResult(true, "全部配对！", "得分 " + score + " · 用时 " + seconds + "s · " + moves + " 步", this::reset);
                    }
                } else {
                    states[first] = 0;
                    states[second] = 0;
                    anim = true;
                }
                first = -1;
                second = -1;
                locked = false;
                invalidate();
            }, 620);
        }
        invalidate();
    }
}
