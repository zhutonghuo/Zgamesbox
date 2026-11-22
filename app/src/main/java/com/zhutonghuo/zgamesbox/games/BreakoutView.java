package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.Ui;

public class BreakoutView extends BaseGameView {
    private static final int COLS = 7;
    private static final int ROWS = 5;
    private static final int[] ROW_COLORS = {
            0xFFEF4444, 0xFFF97316, 0xFFF2A900, 0xFF22C55E, 0xFF06B6D4
    };

    private final boolean[][] bricks = new boolean[ROWS][COLS];
    private final RectF br = new RectF();
    private final RectF paddle = new RectF();
    private float bx, by, bw, bh;
    private float ballX, ballY, vx, vy, radius;
    private float paddleW;
    private int score, lives, level, state;

    public BreakoutView(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "breakout"; }

    @Override
    protected void onLayoutChanged() {
        bw = field.width() / COLS;
        bh = dp(26);
        bx = field.left;
        by = field.top + dp(12);
        paddleW = Math.max(dp(80), Math.min(dp(130), field.width() / 3));
        paddle.set(W / 2f - paddleW / 2, H - dp(96), W / 2f + paddleW / 2, H - dp(82));
        radius = dp(7);
        if (state == 0) resetBall();
    }

    @Override
    protected void onReset() { reset(); }

    private void reset() {
        state = 0;
        score = 0;
        lives = 3;
        level = 1;
        vx = dp(190);
        vy = dp(260);
        fillBricks();
        resetBall();
    }

    private void fillBricks() {
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) bricks[r][c] = true;
    }

    private void resetBall() {
        ballX = paddle.centerX();
        ballY = paddle.top - radius - dp(2);
        vx = (level % 2 == 0 ? 1 : -1) * dp(190 + level * 25);
        vy = dp(260 + level * 20);
    }

    @Override
    protected void update(float dt, long now) {
        if (state != 1) return;
        ballX += vx * dt;
        ballY += vy * dt;
        if (ballX < radius) { ballX = radius; vx = Math.abs(vx); }
        if (ballX > W - radius) { ballX = W - radius; vx = -Math.abs(vx); }
        if (ballY < dp(62) + radius) { ballY = dp(62) + radius; vy = Math.abs(vy); }

        if (vy > 0 && ballY + radius >= paddle.top && ballY - radius <= paddle.bottom
                && ballX >= paddle.left - radius && ballX <= paddle.right + radius) {
            ballY = paddle.top - radius;
            float off = (ballX - paddle.centerX()) / (paddle.width() / 2);
            float speed = (float) Math.hypot(vx, vy) * 1.02f;
            float nx = off * 0.85f;
            vx = speed * nx;
            vy = -speed * (float) Math.sqrt(Math.max(0.08, 1 - nx * nx));
            play(GameSfx.TAP);
        }

        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            if (!bricks[r][c]) continue;
            br.set(bx + c * bw + dp(2), by + r * bh + dp(2),
                    bx + (c + 1) * bw - dp(2), by + (r + 1) * bh - dp(2));
            if (ballX + radius > br.left && ballX - radius < br.right
                    && ballY + radius > br.top && ballY - radius < br.bottom) {
                float overlapX = Math.min(Math.abs(ballX - br.left), Math.abs(ballX - br.right));
                float overlapY = Math.min(Math.abs(ballY - br.top), Math.abs(ballY - br.bottom));
                if (overlapX < overlapY) vx = -vx; else vy = -vy;
                bricks[r][c] = false;
                score += 10;
                saveBest(score);
                play(GameSfx.SCORE);
                buzz(10);
                boolean clear = true;
                for (int rr = 0; rr < ROWS; rr++) for (int cc = 0; cc < COLS; cc++) if (bricks[rr][cc]) clear = false;
                if (clear) {
                    level++;
                    fillBricks();
                    resetBall();
                    play(GameSfx.WIN);
                }
                return;
            }
        }

        if (ballY - radius > H) {
            lives--;
            play(GameSfx.FAIL);
            buzz(60);
            if (lives <= 0) {
                state = 2;
                saveBest(score);
                showResult(false, "游戏结束", "得分 " + score + " · 到达第 " + level + " 关", this::reset);
            } else {
                resetBall();
            }
        }
    }

    @Override
    protected void drawGame(Canvas c) {
        header(c, "打砖块", "分数 " + score + " · 生命 " + lives, 0xFFF97316);
        for (int r = 0; r < ROWS; r++) for (int col = 0; col < COLS; col++) {
            if (!bricks[r][col]) continue;
            br.set(bx + col * bw + dp(2), by + r * bh + dp(2),
                    bx + (col + 1) * bw - dp(2), by + (r + 1) * bh - dp(2));
            paint.setColor(ROW_COLORS[r]);
            c.drawRoundRect(br, dp(5), dp(5), paint);
        }
        paint.setColor(0x55FFFFFF);
        c.drawCircle(ballX - vx * 0.03f, ballY - vy * 0.03f, radius * 0.7f, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(ballX, ballY, radius, paint);
        paint.setColor(Ui.ACCENT);
        c.drawRoundRect(paddle, dp(7), dp(7), paint);
        paint.setColor(0x88FFFFFF);
        c.drawRect(paddle.left + dp(8), paddle.top + dp(2), paddle.right - dp(8), paddle.top + dp(4), paint);

        if (state != 1) {
            paint.setColor(0x990A0E14);
            c.drawRect(0, dp(62), W, H, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(26));
            paint.setColor(Ui.TEXT);
            c.drawText(state == 0 ? "点击开始" : "游戏结束", W / 2f, H / 2f - dp(14), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(14));
            paint.setColor(Ui.MUTED);
            c.drawText(state == 0 ? "左右拖动控制挡板" : "得分 " + score,
                    W / 2f, H / 2f + dp(20), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        float x = e.getX();
        paddle.left = Math.max(0, Math.min(W - paddleW, x - paddleW / 2));
        paddle.right = paddle.left + paddleW;
        if (state == 0) {
            ballX = paddle.centerX();
            ballY = paddle.top - radius - dp(2);
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) state = 1;
        } else if (state == 2 && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            reset();
        }
    }
}
