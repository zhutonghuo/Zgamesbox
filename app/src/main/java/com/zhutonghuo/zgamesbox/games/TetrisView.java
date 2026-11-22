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
import java.util.Random;

public class TetrisView extends BaseGameView {
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int[][][] BASE = {
            {{0, 1}, {1, 1}, {2, 1}, {3, 1}},
            {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
            {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
            {{0, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{2, 0}, {0, 1}, {1, 1}, {2, 1}}
    };
    private static final int[] COLORS = {
            0xFF06B6D4, 0xFFF2A900, 0xFFA855F7, 0xFF22C55E,
            0xFFEF4444, 0xFF3B82F6, 0xFFF97316
    };

    private final int[][] board = new int[ROWS][COLS];
    private final Random rnd = new Random();
    private final RectF r = new RectF();
    private final RectF[] buttons = new RectF[5];
    private int[][] cells;
    private int piece, next, color, px, py, size;
    private float cell, bx, by;
    private float dropMs, downX, downY;
    private int score, lines, level, state;

    public TetrisView(Context c) {
        super(c);
        for (int i = 0; i < buttons.length; i++) buttons[i] = new RectF();
        reset();
    }

    @Override
    protected String gameId() { return "tetris"; }

    @Override
    protected void onLayoutChanged() {
        cell = Math.min(field.width() / COLS, (field.height() - dp(90)) / ROWS);
        bx = field.left + (field.width() - cell * COLS) / 2;
        by = field.top;
        float bw = Math.min(dp(54), (W - dp(40)) / 5);
        float start = W / 2f - bw * 2.5f - dp(12);
        for (int i = 0; i < 5; i++) {
            buttons[i].set(start + i * (bw + dp(6)), H - dp(66),
                    start + i * (bw + dp(6)) + bw, H - dp(14));
        }
    }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        for (int y = 0; y < ROWS; y++) for (int x = 0; x < COLS; x++) board[y][x] = 0;
        score = 0;
        lines = 0;
        level = 1;
        state = 0;
        dropMs = 0;
        next = rnd.nextInt(BASE.length);
        spawn();
    }

    private void spawn() {
        piece = next;
        next = rnd.nextInt(BASE.length);
        color = COLORS[piece];
        size = piece == 0 ? 4 : (piece == 1 ? 2 : 3);
        cells = new int[BASE[piece].length][2];
        for (int i = 0; i < cells.length; i++) {
            cells[i][0] = BASE[piece][i][0];
            cells[i][1] = BASE[piece][i][1];
        }
        px = 3;
        py = 0;
        if (!valid(px, py, cells)) {
            state = 2;
            saveBest(score);
            play(GameSfx.FAIL);
            buzz(90);
            showResult(false, "游戏结束", "得分 " + score + " · 消除 " + lines + " 行 · 等级 " + level, this::reset);
        }
    }

    private boolean valid(int x, int y, int[][] cs) {
        for (int[] q : cs) {
            int tx = x + q[0], ty = y + q[1];
            if (tx < 0 || tx >= COLS || ty >= ROWS) return false;
            if (ty >= 0 && board[ty][tx] != 0) return false;
        }
        return true;
    }

    private void rotate() {
        int[][] nc = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            nc[i][0] = size - 1 - cells[i][1];
            nc[i][1] = cells[i][0];
        }
        for (int dx : new int[]{0, -1, 1, -2, 2}) {
            if (valid(px + dx, py, nc)) {
                cells = nc;
                px += dx;
                play(GameSfx.TAP);
                return;
            }
        }
    }

    private void move(int dx) {
        if (valid(px + dx, py, cells)) { px += dx; play(GameSfx.TAP); }
    }

    private void drop(boolean hard) {
        if (valid(px, py + 1, cells)) {
            py++;
            if (hard) {
                while (valid(px, py + 1, cells)) py++;
                score += 2;
                play(GameSfx.TAP);
            }
        } else {
            lock();
        }
    }

    private void lock() {
        for (int[] q : cs()) {
            if (q[1] >= 0 && q[1] < ROWS && q[0] >= 0 && q[0] < COLS) board[q[1]][q[0]] = color;
        }
        int cleared = 0;
        for (int y = ROWS - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < COLS; x++) if (board[y][x] == 0) { full = false; break; }
            if (full) {
                cleared++;
                for (int yy = y; yy > 0; yy--) System.arraycopy(board[yy - 1], 0, board[yy], 0, COLS);
                for (int x = 0; x < COLS; x++) board[0][x] = 0;
                y++;
            }
        }
        if (cleared > 0) {
            score += cleared * cleared * 100;
            lines += cleared;
            level = 1 + lines / 10;
            play(GameSfx.SCORE);
            buzz(25);
        }
        saveBest(score);
        spawn();
    }

    private int[][] cs() {
        int[][] out = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            out[i][0] = cells[i][0] + px;
            out[i][1] = cells[i][1] + py;
        }
        return out;
    }

    @Override
    protected void update(float dt, long now) {
        if (state != 1) return;
        dropMs += dt * 1000;
        float interval = Math.max(110f, 720f - (level - 1) * 65f);
        while (dropMs >= interval) {
            dropMs -= interval;
            drop(false);
        }
    }

    @Override
    protected void drawGame(Canvas c) {
        header(c, "俄罗斯方块", "分数 " + score + " · 等级 " + level, 0xFFA855F7);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0D141C);
        c.drawRect(bx, by, bx + cell * COLS, by + cell * ROWS, paint);
        paint.setColor(0xFF16202B);
        for (int x = 0; x <= COLS; x++) c.drawRect(bx + x * cell - 0.5f, by, bx + x * cell + 0.5f, by + cell * ROWS, paint);
        for (int y = 0; y <= ROWS; y++) c.drawRect(bx, by + y * cell - 0.5f, bx + cell * COLS, by + y * cell + 0.5f, paint);

        for (int y = 0; y < ROWS; y++) for (int x = 0; x < COLS; x++) {
            if (board[y][x] != 0) block(c, bx + x * cell, by + y * cell, board[y][x], 255);
        }
        if (state != 2) {
            int gy = py;
            while (valid(px, gy + 1, cells)) gy++;
            for (int[] q : cells) {
                if (gy + q[1] >= 0) block(c, bx + (px + q[0]) * cell, by + (gy + q[1]) * cell, color, 70);
            }
            for (int[] q : cells) {
                if (py + q[1] >= 0) block(c, bx + (px + q[0]) * cell, by + (py + q[1]) * cell, color, 255);
            }
        }

        String[] labels = {"←", "→", "↻", "↓", "⤓"};
        for (int i = 0; i < 5; i++) {
            paint.setColor(Ui.PANEL2);
            c.drawRoundRect(buttons[i], dp(10), dp(10), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(22));
            paint.setColor(Ui.TEXT);
            c.drawText(labels[i], buttons[i].centerX(), buttons[i].centerY() + dp(8), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);

        if (state != 1) {
            paint.setColor(0xCC0A0E14);
            c.drawRect(bx, by, bx + cell * COLS, by + cell * ROWS, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(25));
            paint.setColor(Ui.TEXT);
            c.drawText(state == 0 ? "点击开始" : "游戏结束", W / 2f, by + cell * ROWS / 2f - dp(10), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(13));
            paint.setColor(Ui.MUTED);
            c.drawText(state == 0 ? "按钮或滑动操作" : "得分 " + score,
                    W / 2f, by + cell * ROWS / 2f + dp(22), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void block(Canvas c, float x, float y, int col, int alpha) {
        r.set(x + 1, y + 1, x + cell - 1, y + cell - 1);
        paint.setColor((col & 0xFFFFFF) | (alpha << 24));
        c.drawRoundRect(r, dp(3), dp(3), paint);
        paint.setColor(0x33FFFFFF);
        c.drawRect(r.left + dp(2), r.top + dp(2), r.right - dp(2), r.top + dp(4), paint);
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
            for (int i = 0; i < 5; i++) {
                if (buttons[i].contains(e.getX(), e.getY())) {
                    if (state == 0) state = 1;
                    if (state == 2) { reset(); return; }
                    if (i == 0) move(-1);
                    else if (i == 1) move(1);
                    else if (i == 2) rotate();
                    else if (i == 3) drop(false);
                    else { drop(true); play(GameSfx.TAP); }
                    return;
                }
            }
            if (state == 0) state = 1;
            if (state == 2) reset();
        } else if (a == MotionEvent.ACTION_UP) {
            float dx = e.getX() - downX;
            float dy = e.getY() - downY;
            if (Math.max(Math.abs(dx), Math.abs(dy)) < dp(18)) return;
            if (Math.abs(dx) > Math.abs(dy)) move(dx > 0 ? 1 : -1);
            else if (dy > 0) drop(true);
            else rotate();
        }
    }
}
