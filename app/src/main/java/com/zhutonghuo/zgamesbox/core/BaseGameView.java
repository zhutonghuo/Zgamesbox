package com.zhutonghuo.zgamesbox.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

public abstract class BaseGameView extends View {
    protected final Context ctx;
    protected final Paint paint = Ui.paint(Ui.TEXT);
    protected final RectF field = new RectF();
    protected int W, H;
    protected float density;
    protected boolean paused;
    private long lastFrame;
    private final Handler gameHandler = new Handler(Looper.getMainLooper());

    private boolean resultShowing;
    private long resultShownAt;
    private boolean resultWin;
    private String resultTitle = "";
    private String resultDetail = "";
    private Runnable resultReplay;
    private Runnable lobbyCallback;
    private final RectF resultCard = new RectF();
    private final RectF[] resultBtns = {new RectF(), new RectF(), new RectF()};

    public BaseGameView(Context c) {
        super(c);
        ctx = c;
        density = c.getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
    }

    protected abstract String gameId();
    protected abstract void update(float dt, long now);
    protected abstract void drawGame(Canvas c);
    protected abstract void onGameTouch(MotionEvent e);

    protected void onReset() {}
    protected void onLayoutChanged() {}
    protected void saveState() {}
    protected boolean continuous() { return true; }
    public boolean handleBack() {
        if (resultShowing) { hideResult(); return true; }
        return false;
    }
    public void onNet(int from, String action, String payload) {}
    public void release() {
        cancelGameCallbacks();
    }

    protected float dp(float v) { return Ui.dp(ctx, v); }
    protected int dpi(float v) { return Ui.dpi(ctx, v); }
    protected int best() { return App.prefs().best(gameId()); }
    protected void saveBest(int score) { App.prefs().saveBest(gameId(), score); }
    protected int winCount() { return App.prefs().wins(gameId()); }
    protected void recordWin() { App.prefs().addWin(gameId()); }
    protected void play(int s) {
        GameSfx sfx = App.sfx();
        if (sfx != null) sfx.play(s);
    }

    protected void buzz(int ms) {
        Haptics h = App.haptics();
        if (h != null) h.buzz(ms);
    }

    protected void postGame(Runnable r, long delayMs) {
        gameHandler.postDelayed(r, delayMs);
    }

    protected void cancelGameCallbacks() {
        gameHandler.removeCallbacksAndMessages(null);
    }

    public void restart() {
        hideResult();
        onReset();
        lastFrame = now();
        invalidate();
    }

    public void setLobbyCallback(Runnable cb) { lobbyCallback = cb; }

    protected void showResult(boolean win, String title, String detail, Runnable replay) {
        resultShowing = true;
        resultShownAt = now();
        resultWin = win;
        resultTitle = title == null ? "" : title;
        resultDetail = detail == null ? "" : detail;
        resultReplay = replay;
        play(win ? GameSfx.WIN : GameSfx.FAIL);
        invalidate();
    }

    public void hideResult() {
        resultShowing = false;
        resultReplay = null;
        invalidate();
    }

    public boolean isResultShowing() { return resultShowing; }

    private void layoutResult() {
        float cw = Math.min(dp(300), W - dp(48));
        float ch = dp(216);
        float cx = (W - cw) / 2f, cy = (H - ch) / 2f;
        resultCard.set(cx, cy, cx + cw, cy + ch);
        float pad = dp(18), gap = dp(8);
        float bw = (cw - pad * 2 - gap * 2) / 3f;
        float by = cy + ch - dp(62);
        for (int i = 0; i < 3; i++) {
            float bx = cx + pad + i * (bw + gap);
            resultBtns[i].set(bx, by, bx + bw, by + dp(44));
        }
    }

    private void drawResult(Canvas c) {
        layoutResult();
        c.drawRect(0, dp(62), W, H, paint(0xAA050810));
        c.drawRoundRect(resultCard, dp(18), dp(18), paint(Ui.PANEL));
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(resultWin ? Ui.OK : Ui.BAD);
        paint.setStrokeWidth(dp(2));
        c.drawRoundRect(resultCard, dp(18), dp(18), paint);
        paint.setStyle(Paint.Style.FILL);

        float cx = resultCard.centerX();
        float iconY = resultCard.top + dp(58);
        paint.setColor(resultWin ? Ui.OK : Ui.BAD);
        c.drawCircle(cx, iconY, dp(26), paint);
        paint.setColor(0xFFFFFFFF);
        paint.setStrokeWidth(dp(4));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        Path icon = new Path();
        if (resultWin) {
            icon.moveTo(cx - dp(11), iconY);
            icon.lineTo(cx - dp(3), iconY + dp(8));
            icon.lineTo(cx + dp(12), iconY - dp(10));
        } else {
            icon.moveTo(cx - dp(9), iconY - dp(9));
            icon.lineTo(cx + dp(9), iconY + dp(9));
            icon.moveTo(cx + dp(9), iconY - dp(9));
            icon.lineTo(cx - dp(9), iconY + dp(9));
        }
        c.drawPath(icon, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(21));
        paint.setColor(Ui.TEXT);
        c.drawText(resultTitle, cx, resultCard.top + dp(112), paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(dp(12));
        paint.setColor(Ui.MUTED);
        c.drawText(resultDetail, cx, resultCard.top + dp(138), paint);

        String[] labels = {"再来一局", "返回大厅", "分享战绩"};
        int[] colors = {Ui.PRIMARY, Ui.PANEL2, Ui.PANEL2};
        for (int i = 0; i < 3; i++) {
            RectF r = resultBtns[i];
            paint.setColor(colors[i]);
            c.drawRoundRect(r, dp(9), dp(9), paint);
            paint.setColor(i == 0 ? 0xFFFFFFFF : Ui.TEXT);
            paint.setTextSize(dp(12));
            paint.setFakeBoldText(i == 0);
            c.drawText(labels[i], r.centerX(), r.centerY() + dp(4), paint);
            paint.setFakeBoldText(false);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void shareResult() {
        String text = resultTitle + " · " + resultDetail + "\n—— Zgamesbox 小游戏合集";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        Intent chooser = Intent.createChooser(send, "分享战绩");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(chooser); } catch (Exception ignored) {}
    }

    private boolean onResultTouch(MotionEvent e) {
        if (now() - resultShownAt < 300) return true;
        if (e.getActionMasked() != MotionEvent.ACTION_UP) return true;
        float x = e.getX(), y = e.getY();
        if (resultBtns[0].contains(x, y)) {
            hideResult();
            if (resultReplay != null) resultReplay.run();
        } else if (resultBtns[1].contains(x, y)) {
            hideResult();
            if (lobbyCallback != null) lobbyCallback.run();
        } else if (resultBtns[2].contains(x, y)) {
            shareResult();
        } else if (!resultCard.contains(x, y)) {
            hideResult();
        }
        return true;
    }

    public void onHostPause() {
        paused = true;
        saveState();
    }

    public void onHostResume() {
        paused = false;
        lastFrame = now();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        W = w;
        H = h;
        float m = dp(14);
        field.set(m, dp(76), w - m, h - m);
        onLayoutChanged();
    }

    @Override
    protected void onDraw(Canvas c) {
        long t = now();
        if (lastFrame == 0) lastFrame = t;
        float dt = (t - lastFrame) / 1000f;
        lastFrame = t;
        if (dt > 0.05f) dt = 0.05f;
        if (!paused) update(dt, t);
        drawGame(c);
        if (resultShowing) drawResult(c);
        if (!paused && continuous()) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (paused) return true;
        if (resultShowing) return onResultTouch(e);
        onGameTouch(e);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastFrame = now();
    }

    protected long now() { return System.nanoTime() / 1000000L; }

    protected void header(Canvas c, String title, String right, int accent) {
        c.drawRect(0, 0, W, dp(62), paint(Ui.PANEL));
        c.drawRect(0, dp(60), W, dp(62), paint(accent));
        paint.setTextSize(dp(17));
        paint.setFakeBoldText(true);
        paint.setColor(Ui.TEXT);
        paint.setTextAlign(Paint.Align.LEFT);
        c.drawText(title, dp(16), dp(39), paint);
        paint.setTextSize(dp(13));
        paint.setFakeBoldText(false);
        paint.setColor(Ui.MUTED);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText(right == null ? "" : right, W - dp(16), dp(38), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    protected Paint paint(int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(dp(2));
        return paint;
    }
}
