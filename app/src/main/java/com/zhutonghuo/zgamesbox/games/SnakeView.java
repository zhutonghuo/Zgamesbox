package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

public class SnakeView extends BaseGameView {
    private static final int COLS = 15;
    private static final int ROWS = 25;
    private final ArrayList<int[]> body = new ArrayList<>();
    private final ArrayDeque<Integer> turns = new ArrayDeque<>();
    private final Random rnd = new Random();
    private final RectF cellRect = new RectF();
    private int dir = 1;
    private int pendingDir = 1;
    private int[] food;
    private int score;
    private int state;
    private float cell, ox, oy;
    private long stepMs;
    private float downX, downY;

    public SnakeView(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "snake"; }

    @Override
    protected void onLayoutChanged() {
        cell = Math.min(field.width() / COLS, field.height() / ROWS);
        ox = field.left + (field.width() - cell * COLS) / 2;
        oy = field.top + (field.height() - cell * ROWS) / 2;
    }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        body.clear();
        turns.clear();
        body.add(new int[]{COLS / 2, ROWS / 2});
        dir = 1;
        pendingDir = 1;
        score = 0;
        state = 0;
        stepMs = 0;
        spawnFood();
    }

    private void spawnFood() {
        for (int tries = 0; tries < 500; tries++) {
            int x = rnd.nextInt(COLS);
            int y = rnd.nextInt(ROWS);
            boolean hit = false;
            for (int[] s : body) if (s[0] == x && s[1] == y) { hit = true; break; }
            if (!hit) { food = new int[]{x, y}; return; }
        }
        food = new int[]{0, 0};
    }

    @Override
    protected void update(float dt, long now) {
        if (state != 1) return;
        stepMs += dt * 1000;
        float interval = Math.max(85f, 200f - score * 1.5f);
        while (stepMs >= interval && state == 1) {
            stepMs -= interval;
            step();
        }
    }

    private void step() {
        if (!turns.isEmpty()) dir = turns.poll();
        int[] h = body.get(0);
        int nx = h[0], ny = h[1];
        if (dir == 1) nx++;
        else if (dir == 2) ny++;
        else if (dir == 3) nx--;
        else ny--;
        if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS) { die(); return; }
        for (int[] s : body) if (s[0] == nx && s[1] == ny) { die(); return; }
        body.add(0, new int[]{nx, ny});
        if (nx == food[0] && ny == food[1]) {
            score += 10;
            play(GameSfx.SCORE);
            buzz(18);
            spawnFood();
        } else {
            body.remove(body.size() - 1);
        }
    }

    private void die() {
        state = 2;
        saveBest(score);
        play(GameSfx.FAIL);
        buzz(80);
        showResult(false, "游戏结束", "得分 " + score + " · 最高 " + best(), this::reset);
    }

    @Override
    protected void drawGame(Canvas c) {
        header(c, "贪吃蛇", "分数 " + score + " · 最高 " + best(), Ui.OK);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0D141C);
        c.drawRect(ox, oy, ox + cell * COLS, oy + cell * ROWS, paint);

        paint.setColor(0xFF18222E);
        for (int i = 0; i <= COLS; i++) c.drawRect(ox + i * cell - 0.5f, oy, ox + i * cell + 0.5f, oy + cell * ROWS, paint);
        for (int i = 0; i <= ROWS; i++) c.drawRect(ox, oy + i * cell - 0.5f, ox + cell * COLS, oy + i * cell + 0.5f, paint);

        paint.setColor(0xFFFF5B6E);
        float fx = ox + food[0] * cell + cell / 2;
        float fy = oy + food[1] * cell + cell / 2;
        c.drawCircle(fx, fy, cell * 0.32f, paint);

        for (int i = 0; i < body.size(); i++) {
            int[] s = body.get(i);
            cellRect.set(ox + s[0] * cell + 1, oy + s[1] * cell + 1,
                    ox + (s[0] + 1) * cell - 1, oy + (s[1] + 1) * cell - 1);
            paint.setColor(i == 0 ? 0xFF86EFAC : (i % 2 == 0 ? 0xFF22C55E : 0xFF16A34A));
            Ui.roundRect(c, cellRect, cell * 0.28f, paint);
        }
        if (!body.isEmpty()) {
            int[] h = body.get(0);
            float cx = ox + h[0] * cell + cell / 2;
            float cy = oy + h[1] * cell + cell / 2;
            paint.setColor(0xFF0A0E14);
            float ex = dir == 3 ? -cell * 0.12f : cell * 0.12f;
            c.drawCircle(cx + ex, cy - cell * 0.13f, cell * 0.055f, paint);
            c.drawCircle(cx + ex, cy + cell * 0.13f, cell * 0.055f, paint);
        }

        if (state != 1) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xCC0A0E14);
            c.drawRect(ox, oy, ox + cell * COLS, oy + cell * ROWS, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(26));
            paint.setFakeBoldText(true);
            paint.setColor(Ui.TEXT);
            String t = state == 0 ? "滑动开始" : "游戏结束";
            c.drawText(t, W / 2f, oy + cell * ROWS / 2f - dp(10), paint);
            paint.setTextSize(dp(14));
            paint.setFakeBoldText(false);
            paint.setColor(Ui.MUTED);
            c.drawText(state == 0 ? "上下左右滑动控制方向" : "得分 " + score,
                    W / 2f, oy + cell * ROWS / 2f + dp(24), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
        } else if (a == MotionEvent.ACTION_UP) {
            if (state == 2) {
                reset();
                state = 1;
                return;
            }
            if (state == 0) state = 1;
            float dx = e.getX() - downX;
            float dy = e.getY() - downY;
            if (Math.max(Math.abs(dx), Math.abs(dy)) < dp(18)) return;
            int nd = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : 3) : (dy > 0 ? 2 : 4);
            if ((dir == 1 && nd == 3) || (dir == 3 && nd == 1) || (dir == 2 && nd == 4) || (dir == 4 && nd == 2)) return;
            if (turns.size() < 2) turns.add(nd);
        }
    }
}
