package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class FlappyView extends BaseGameView {
    private static final class Pipe {
        float x, gapY;
        boolean scored;
        Pipe(float x, float gapY) { this.x = x; this.gapY = gapY; }
    }

    private final ArrayList<Pipe> pipes = new ArrayList<>();
    private final Random rnd = new Random();
    private final int[] netScore = new int[3];
    private final boolean[] netDead = new boolean[3];
    private float birdY, vy, spawnTimer;
    private int score, state;

    public FlappyView(Context c) {
        super(c);
        reset();
    }

    @Override
    protected String gameId() { return "flappy"; }

    @Override
    protected void onReset() { reset(); }

    private boolean netOn() {
        NetSession n = NetSession.get();
        return n.isOnline() && n.peerCount() >= 2;
    }

    private void reset() {
        birdY = H * 0.42f;
        vy = 0;
        pipes.clear();
        spawnTimer = 0.7f;
        score = 0;
        state = 0;
        for (int i = 0; i < 3; i++) { netScore[i] = 0; netDead[i] = false; }
    }

    private float ground() { return H - dp(72); }
    private float birdX() { return W * 0.3f; }
    private float birdR() { return dp(15); }

    @Override
    protected void update(float dt, long now) {
        if (state != 1) return;
        vy += dp(1450) * dt;
        birdY += vy * dt;
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            spawnTimer = 1.55f;
            float max = ground() - dp(220) - dp(90);
            float gapY = dp(90) + rnd.nextFloat() * Math.max(dp(10), max);
            pipes.add(new Pipe(W + dp(20), gapY));
        }
        float pw = dp(66);
        float gapH = dp(190);
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= dp(165) * dt;
            float bx = birdX();
            float br = birdR();
            if (bx + br > p.x && bx - br < p.x + pw) {
                if (birdY - br < p.gapY || birdY + br > p.gapY + gapH) die();
            }
            if (!p.scored && p.x + pw < bx) {
                p.scored = true;
                score += 10;
                play(GameSfx.SCORE);
                saveBest(score);
                buzz(15);
                if (netOn()) {
                    netScore[NetSession.get().seat()] = score;
                    NetSession.get().send("SC", String.valueOf(score));
                }
            }
            if (p.x < -pw) it.remove();
        }
        if (birdY + birdR() > ground()) die();
    }

    private void die() {
        if (state != 1) return;
        state = 2;
        saveBest(score);
        play(GameSfx.FAIL);
        buzz(70);
        if (netOn()) {
            netScore[NetSession.get().seat()] = score;
            netDead[NetSession.get().seat()] = true;
            NetSession.get().send("DIE", String.valueOf(score));
        }
        final int points = score / 10;
        showResult(false, "撞到了！", "得分 " + points + " · 最高 " + best() / 10, () -> {
            reset();
            if (netOn()) NetSession.get().send("RESET", "");
        });
    }

    @Override
    public void onNet(int from, String action, String payload) {
        if (from < 0 || from > 2) return;
        if ("RESET".equals(action)) { reset(); invalidate(); return; }
        if ("SC".equals(action)) {
            try { netScore[from] = Integer.parseInt(payload.trim()); } catch (Exception ignored) {}
            invalidate();
        } else if ("DIE".equals(action)) {
            netDead[from] = true;
            try { netScore[from] = Integer.parseInt(payload.trim()); } catch (Exception ignored) {}
            invalidate();
        }
    }

    private String standing() {
        NetSession ns = NetSession.get();
        String[] all = ns.names();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String nm = all[i];
            if (nm == null || nm.isEmpty()) continue;
            sb.append(i == ns.seat() ? "我" : nm).append(" ").append(netScore[i] / 10);
            if (netDead[i]) sb.append("✓");
            sb.append("  ");
        }
        return sb.toString().trim();
    }

    @Override
    protected void drawGame(Canvas c) {
        boolean net = netOn();
        header(c, "Flappy Bird", net ? "联机 · 最高 " + best() : "得分 " + score + " · 最高 " + best(), Ui.ACCENT);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0F2027);
        c.drawRect(0, dp(62), W, ground(), paint);
        paint.setColor(0xFF123145);
        for (int i = 0; i < 7; i++) {
            float y = dp(90) + i * dp(80);
            c.drawRect(0, y, W, y + dp(2), paint);
        }
        float pw = dp(66);
        float gapH = dp(190);
        for (Pipe p : pipes) {
            paint.setColor(0xFF22C55E);
            c.drawRect(p.x, dp(62), p.x + pw, p.gapY, paint);
            c.drawRect(p.x, p.gapY + gapH, p.x + pw, ground(), paint);
            paint.setColor(0xFF16A34A);
            c.drawRect(p.x - dp(3), p.gapY - dp(16), p.x + pw + dp(3), p.gapY, paint);
            c.drawRect(p.x - dp(3), p.gapY + gapH, p.x + pw + dp(3), p.gapY + gapH + dp(16), paint);
        }
        paint.setColor(0xFF8B5E3C);
        c.drawRect(0, ground(), W, H, paint);
        paint.setColor(0xFF22C55E);
        c.drawRect(0, ground(), W, ground() + dp(10), paint);

        float bx = birdX();
        float by = birdY;
        float r = birdR();
        c.save();
        c.translate(bx, by);
        c.rotate(Math.max(-28, Math.min(70, vy / dp(18))));
        paint.setColor(0xFFFDE047);
        c.drawCircle(0, 0, r, paint);
        paint.setColor(0xFFF59E0B);
        c.drawCircle(-r * 0.35f, r * 0.12f, r * 0.6f, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(r * 0.45f, -r * 0.3f, r * 0.3f, paint);
        paint.setColor(0xFF0A0E14);
        c.drawCircle(r * 0.53f, -r * 0.3f, r * 0.13f, paint);
        c.restore();

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(40));
        paint.setColor(0xFFFFFFFF);
        c.drawText(String.valueOf(score / 10), W / 2f, dp(130), paint);
        paint.setFakeBoldText(false);

        if (net) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(dp(13));
            c.drawText(standing(), W / 2f, dp(168), paint);
        }

        if (state != 1) {
            paint.setColor(0x990A0E14);
            c.drawRect(0, dp(62), W, H, paint);
            paint.setTextSize(dp(26));
            paint.setFakeBoldText(true);
            paint.setColor(Ui.TEXT);
            c.drawText(state == 0 ? "点击起飞" : "撞到了！", W / 2f, H / 2f - dp(20), paint);
            paint.setTextSize(dp(14));
            paint.setFakeBoldText(false);
            paint.setColor(Ui.MUTED);
            c.drawText(state == 0 ? "点按屏幕振翅穿管道" : "得分 " + score,
                    W / 2f, H / 2f + dp(18), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return;
        if (state == 0) {
            state = 1;
            vy = -dp(480);
            play(GameSfx.TAP);
        } else if (state == 1) {
            vy = -dp(480);
            play(GameSfx.TAP);
        } else {
            reset();
            if (netOn()) NetSession.get().send("RESET", "");
        }
    }
}
