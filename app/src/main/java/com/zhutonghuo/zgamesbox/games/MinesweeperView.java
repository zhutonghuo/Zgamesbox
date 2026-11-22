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
import java.util.Random;

public class MinesweeperView extends BaseGameView {
    private static final int N = 9;
    private static final int MINES = 10;
    private final boolean[][] mine = new boolean[N][N];
    private final int[][] num = new int[N][N];
    private final int[][] state = new int[N][N];
    private final RectF flagBtn = new RectF();
    private final RectF resetBtn = new RectF();
    private final Random rnd = new Random();
    private boolean placed, dead, won, flagMode, review;
    private int revealed, flags;
    private long started, downTime;
    private float downX, downY;

    public MinesweeperView(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "mines"; }

    @Override
    protected void update(float dt, long now) {}

    @Override
    protected boolean continuous() { return false; }

    @Override
    protected void onLayoutChanged() {
        flagBtn.set(W / 2f - dp(124), H - dp(58), W / 2f - dp(8), H - dp(12));
        resetBtn.set(W / 2f + dp(8), H - dp(58), W / 2f + dp(124), H - dp(12));
    }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            mine[r][c] = false;
            num[r][c] = 0;
            state[r][c] = 0;
        }
        placed = false;
        dead = false;
        won = false;
        flagMode = false;
        review = false;
        revealed = 0;
        flags = 0;
        started = now();
    }

    private void placeMines(int sx, int sy) {
        int n = 0;
        while (n < MINES) {
            int r = rnd.nextInt(N), c = rnd.nextInt(N);
            if (mine[r][c]) continue;
            if (Math.abs(r - sy) <= 1 && Math.abs(c - sx) <= 1) continue;
            mine[r][c] = true;
            n++;
        }
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (mine[r][c]) continue;
            int k = 0;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nc >= 0 && nr < N && nc < N && mine[nr][nc]) k++;
            }
            num[r][c] = k;
        }
        placed = true;
    }

    private void flood(int sx, int sy) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int r = p[0], c = p[1];
            if (r < 0 || c < 0 || r >= N || c >= N || state[r][c] != 0) continue;
            state[r][c] = 1;
            revealed++;
            if (num[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                    if (dr != 0 || dc != 0) q.add(new int[]{r + dr, c + dc});
                }
            }
        }
    }

    private void reveal(int r, int c) {
        if (dead || won || state[r][c] == 1 || state[r][c] == 2) return;
        if (!placed) {
            placeMines(c, r);
            started = now();
        }
        if (mine[r][c]) {
            state[r][c] = 3;
            dead = true;
            play(GameSfx.FAIL);
            buzz(90);
            int sec = (int) ((now() - started) / 1000);
            showResult(false, "踩雷了", "用时 " + sec + "s · 关闭弹窗后点「复盘」看所有雷", this::reset);
            return;
        }
        flood(r, c);
        play(GameSfx.TAP);
        if (revealed == N * N - MINES) {
            won = true;
            int sec = (int) ((now() - started) / 1000);
            int score = Math.max(10, 1000 - sec * 10);
            saveBest(score);
            play(GameSfx.WIN);
            buzz(60);
            showResult(true, "排雷成功！", "用时 " + sec + "s · 得分 " + score + " · 最高 " + best(), this::reset);
        }
    }

    private void toggleFlag(int r, int c) {
        if (dead || won || state[r][c] == 1 || state[r][c] == 3) return;
        if (state[r][c] == 2) {
            state[r][c] = 0;
            flags--;
        } else {
            state[r][c] = 2;
            flags++;
        }
        play(GameSfx.CLICK);
    }

    private float cell() {
        return Math.min((field.width() - dp(24)) / N, (field.height() - dp(84)) / N);
    }

    private float ox() { return W / 2f - cell() * N / 2; }
    private float oy() { return field.top + (field.height() - cell() * N - dp(84)) / 2 + dp(10); }

    @Override
    protected void drawGame(Canvas c) {
        int sec = placed ? (int) ((now() - started) / 1000) : 0;
        boolean over = dead || won;
        header(c, "扫雷", (review ? "复盘 · " : "") + "剩雷 " + (MINES - flags) + " · " + sec + "s · 最高 " + best(), 0xFF94A3B8);
        float s = cell();
        for (int r = 0; r < N; r++) for (int col = 0; col < N; col++) {
            float x = ox() + col * s, y = oy() + r * s;
            int v = state[r][col];
            boolean showMine = review && mine[r][col] && v != 1 && v != 3;
            boolean wrongFlag = review && v == 2 && !mine[r][col];
            if (showMine) {
                paint.setColor(0xFF3B1D1D);
            } else if (v == 1) {
                paint.setColor(0xFF111A24);
            } else if (v == 3) {
                paint.setColor(Ui.BAD);
            } else {
                paint.setColor(Ui.PANEL2);
            }
            c.drawRoundRect(x + dp(1), y + dp(1), x + s - dp(1), y + s - dp(1), dp(3), dp(3), paint);
            if (showMine) {
                paint.setColor(0xFFF87171);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.6f);
                c.drawText("●", x + s / 2, y + s * 0.72f, paint);
                paint.setFakeBoldText(false);
            } else if (wrongFlag) {
                paint.setColor(Ui.MUTED);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.6f);
                c.drawText("×", x + s / 2, y + s * 0.72f, paint);
                paint.setFakeBoldText(false);
            } else if (v == 2 || v == 3) {
                paint.setColor(v == 2 ? Ui.BAD : 0xFFFFFFFF);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.6f);
                c.drawText(v == 2 ? "⚑" : "●", x + s / 2, y + s * 0.72f, paint);
                paint.setFakeBoldText(false);
            } else if (v == 1 && num[r][col] > 0) {
                paint.setColor(new int[]{0, 0xFF60A5FA, 0xFF22C55E, 0xFFF87171,
                        0xFFA78BFA, 0xFFF97316, 0xFF06B6D4, 0xFFE5E7EB}[num[r][col]]);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                paint.setTextSize(s * 0.58f);
                c.drawText(String.valueOf(num[r][col]), x + s / 2, y + s * 0.72f, paint);
                paint.setFakeBoldText(false);
            }
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(over || flagMode ? Ui.PRIMARY : Ui.PANEL2);
        c.drawRoundRect(flagBtn, dp(9), dp(9), paint);
        paint.setColor(Ui.TEXT);
        paint.setTextSize(dp(13));
        c.drawText(over ? (review ? "退出复盘" : "复盘") : (flagMode ? "插旗中" : "插旗"),
                flagBtn.centerX(), H - dp(36), paint);
        paint.setColor(Ui.PANEL2);
        c.drawRoundRect(resetBtn, dp(9), dp(9), paint);
        paint.setColor(Ui.TEXT);
        c.drawText("新局", resetBtn.centerX(), H - dp(36), paint);
        paint.setTextAlign(Paint.Align.LEFT);

        if (over && !review) {
            paint.setColor(0xBB0A0E14);
            c.drawRect(ox(), oy(), ox() + s * N, oy() + s * N, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(25));
            paint.setColor(won ? Ui.OK : Ui.BAD);
            c.drawText(won ? "排雷成功！" : "踩雷了", W / 2f, H / 2f - dp(10), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(13));
            paint.setColor(Ui.TEXT);
            c.drawText("点击「复盘」看所有雷 · 点棋盘再来一局", W / 2f, H / 2f + dp(22), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        } else if (review) {
            paint.setColor(Ui.MUTED);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(12));
            c.drawText("● 地雷   × 插错的旗", W / 2f, oy() + s * N + dp(20), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downTime = now();
            downX = x;
            downY = y;
            if (flagBtn.contains(x, y)) {
                if (dead || won) review = !review;
                else flagMode = !flagMode;
                invalidate();
                return;
            }
            if (resetBtn.contains(x, y)) { reset(); invalidate(); return; }
            if (dead || won) { reset(); invalidate(); return; }
        }
        if (a != MotionEvent.ACTION_UP) return;
        float s = cell();
        int col = (int) ((x - ox()) / s);
        int row = (int) ((y - oy()) / s);
        if (col < 0 || row < 0 || col >= N || row >= N) return;
        boolean longPress = now() - downTime > 420 || Math.abs(x - downX) > dp(10) || Math.abs(y - downY) > dp(10);
        if (flagMode || longPress) toggleFlag(row, col);
        else reveal(row, col);
        invalidate();
    }
}
