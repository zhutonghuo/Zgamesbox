package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.Random;

public class Game2048View extends BaseGameView {
    private final int[][] g = new int[4][4];
    private final float[][] sc = new float[4][4];
    private final Random rnd = new Random();
    private final RectF tile = new RectF();
    private int score;
    private boolean over, won, anim;
    private float downX, downY;
    private final RectF restartRect = new RectF();

    public Game2048View(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "2048"; }

    @Override
    protected boolean continuous() { return anim; }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) { g[r][c] = 0; sc[r][c] = 1; }
        score = 0;
        over = false;
        won = false;
        anim = false;
        spawn();
        spawn();
    }

    private void spawn() {
        int n = 0;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (g[r][c] == 0) n++;
        if (n == 0) return;
        int k = rnd.nextInt(n);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (g[r][c] == 0) {
                if (k == 0) {
                    g[r][c] = rnd.nextInt(10) == 0 ? 4 : 2;
                    sc[r][c] = 1.35f;
                    anim = true;
                    return;
                }
                k--;
            }
        }
    }

    @Override
    protected void update(float dt, long now) {
        if (!anim) return;
        boolean moving = false;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (sc[r][c] > 1f) {
                sc[r][c] -= dt * 4f;
                if (sc[r][c] < 1f) sc[r][c] = 1f;
                if (sc[r][c] > 1f) moving = true;
            }
        }
        anim = moving;
    }

    private boolean move(int dir) {
        boolean changed = false;
        for (int line = 0; line < 4; line++) {
            int[] v = new int[4];
            for (int k = 0; k < 4; k++) {
                int r = dir == 2 ? line : (dir == 3 ? 3 - line : (dir == 0 ? k : 3 - k));
                int c = dir == 0 ? line : (dir == 1 ? 3 - line : (dir == 2 ? k : 3 - k));
                v[k] = g[r][c];
            }
            int[] out = new int[4];
            boolean[] merged = new boolean[4];
            int m = 0;
            for (int k = 0; k < 4; k++) {
                if (v[k] == 0) continue;
                if (m > 0 && out[m - 1] == v[k] && !merged[m - 1]) {
                    out[m - 1] *= 2;
                    score += out[m - 1];
                    merged[m - 1] = true;
                    changed = true;
                } else {
                    out[m++] = v[k];
                }
            }
            for (int k = 0; k < 4; k++) {
                int r = dir == 2 ? line : (dir == 3 ? 3 - line : (dir == 0 ? k : 3 - k));
                int c = dir == 0 ? line : (dir == 1 ? 3 - line : (dir == 2 ? k : 3 - k));
                if (g[r][c] != out[k]) changed = true;
                g[r][c] = out[k];
                if (out[k] != 0 && merged[k]) { sc[r][c] = 1.35f; anim = true; }
                if (g[r][c] >= 2048 && !won) { won = true; play(GameSfx.WIN); }
            }
        }
        if (changed) {
            spawn();
            play(GameSfx.TAP);
            saveBest(score);
            if (!canMove()) {
                over = true;
                play(GameSfx.FAIL);
                showResult(won, won ? "达成 2048！" : "无路可走", "得分 " + score + " · 最高 " + best(), this::reset);
            }
        }
        return changed;
    }

    private boolean canMove() {
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (g[r][c] == 0) return true;
            if (c < 3 && g[r][c] == g[r][c + 1]) return true;
            if (r < 3 && g[r][c] == g[r + 1][c]) return true;
        }
        return false;
    }

    private static int color(int v) {
        switch (v) {
            case 2: return 0xFF2A3446;
            case 4: return 0xFF354259;
            case 8: return 0xFF06B6D4;
            case 16: return 0xFF0EA5E9;
            case 32: return 0xFF6366F1;
            case 64: return 0xFF8B5CF6;
            case 128: return 0xFFA855F7;
            case 256: return 0xFFEC4899;
            case 512: return 0xFFF43F5E;
            case 1024: return 0xFFF59E0B;
            default: return 0xFFF2A900;
        }
    }

    @Override
    protected void drawGame(Canvas c) {
        header(c, "2048", "分数 " + score + " · 最高 " + best(), Ui.GOLD);
        float size = Math.min(field.width(), field.height() - dp(70));
        float bx = field.left + (field.width() - size) / 2;
        float by = field.top;
        float gap = dp(8);
        float cs = (size - gap * 5) / 4;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF141C28);
        c.drawRoundRect(bx, by, bx + size, by + size, dp(14), dp(14), paint);
        for (int r = 0; r < 4; r++) for (int col = 0; col < 4; col++) {
            tile.set(bx + gap + col * (cs + gap), by + gap + r * (cs + gap),
                    bx + gap + col * (cs + gap) + cs, by + gap + r * (cs + gap) + cs);
            paint.setColor(0xFF0F1620);
            c.drawRoundRect(tile, dp(8), dp(8), paint);
            int v = g[r][col];
            if (v == 0) continue;
            float s = sc[r][col];
            float cx = tile.centerX();
            float cy = tile.centerY();
            tile.inset(cs * (1 - s) / 2, cs * (1 - s) / 2);
            paint.setColor(color(v));
            c.drawRoundRect(tile, dp(8), dp(8), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(v >= 1024 ? 22 : v >= 128 ? 27 : 32));
            paint.setColor((v == 4 || v == 2) ? Ui.MUTED : 0xFFFFFFFF);
            c.drawText(String.valueOf(v), cx, cy + dp(11), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(13));
        paint.setColor(won ? Ui.OK : Ui.MUTED);
        c.drawText(won ? "已达成 2048！继续挑战" : "滑动合并数字", W / 2f, by + size + dp(30), paint);

        restartRect.set(W / 2f - dp(70), H - dp(52), W / 2f + dp(70), H - dp(14));
        paint.setColor(Ui.PANEL2);
        c.drawRoundRect(restartRect, dp(10), dp(10), paint);
        paint.setColor(Ui.TEXT);
        paint.setTextSize(dp(14));
        c.drawText("重新开始", W / 2f, H - dp(27), paint);

        if (over) {
            paint.setColor(0xDD0A0E14);
            c.drawRect(bx, by, bx + size, by + size, paint);
            paint.setColor(Ui.BAD);
            paint.setTextSize(dp(28));
            paint.setFakeBoldText(true);
            c.drawText("游戏结束", W / 2f, by + size / 2f, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(14));
            paint.setColor(Ui.TEXT);
            c.drawText("得分 " + score + " · 点击重新开始", W / 2f, by + size / 2f + dp(36), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
        } else if (a == MotionEvent.ACTION_UP) {
            if (restartRect.contains(e.getX(), e.getY()) || over) { reset(); return; }
            float dx = e.getX() - downX;
            float dy = e.getY() - downY;
            if (Math.max(Math.abs(dx), Math.abs(dy)) < dp(22)) return;
            if (Math.abs(dx) > Math.abs(dy)) move(dx > 0 ? 1 : 3);
            else move(dy > 0 ? 2 : 0);
            invalidate();
        }
    }
}
