package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.App;
import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicTacToeView extends BaseGameView {
    private final int[][] board = new int[3][3];
    private final Random rnd = new Random();
    private final RectF[] levels = new RectF[3];
    private final RectF restart = new RectF();
    private int difficulty = 1;
    private int winner;
    private int[] line;
    private boolean playerTurn;
    private final int[] netScore = new int[3];

    public TicTacToeView(Context c) {
        super(c);
        for (int i = 0; i < 3; i++) levels[i] = new RectF();
        reset();
    }

    @Override
    protected String gameId() { return "tictactoe"; }

    @Override
    protected void update(float dt, long now) {}

    @Override
    protected boolean continuous() { return false; }

    @Override
    protected void onLayoutChanged() {
        float w = Math.min(dp(52), (W - dp(80)) / 3);
        float start = W / 2f - w * 1.5f - dp(12);
        for (int i = 0; i < 3; i++) {
            levels[i].set(start + i * (w + dp(6)), H - dp(60),
                    start + i * (w + dp(6)) + w, H - dp(16));
        }
        restart.set(W / 2f - dp(60), H - dp(112), W / 2f + dp(60), H - dp(72));
    }

    @Override
    protected void onReset() { reset(); }

    private boolean netOn() {
        NetSession n = NetSession.get();
        return n.isOnline() && n.seat() < 2 && n.peerCount() >= 2;
    }

    private int myPiece() { return NetSession.get().seat() == 0 ? 1 : 2; }

    private void reset() {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) board[r][c] = 0;
        winner = 0;
        line = null;
        playerTurn = !netOn() || NetSession.get().seat() == 0;
    }

    private int[][] wins() {
        return new int[][]{
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };
    }

    private int check() {
        for (int[] w : wins()) {
            int a = board[w[0] / 3][w[0] % 3];
            if (a != 0 && a == board[w[1] / 3][w[1] % 3] && a == board[w[2] / 3][w[2] % 3]) {
                line = w;
                return a;
            }
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (board[r][c] == 0) return 0;
        return 3;
    }

    private int findMove(int player) {
        List<int[]> empt = new ArrayList<>();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (board[r][c] == 0) empt.add(new int[]{r, c});
        if (empt.isEmpty()) return -1;
        if (difficulty == 0 || rnd.nextInt(5) == 0) return pick(empt);
        for (int[] m : empt) {
            board[m[0]][m[1]] = player;
            int w = check();
            board[m[0]][m[1]] = 0;
            if (w == player) return m[0] * 3 + m[1];
        }
        if (difficulty == 2) {
            int best = -999;
            int idx = -1;
            for (int[] m : empt) {
                board[m[0]][m[1]] = 2;
                int s = minimax(false, 0);
                board[m[0]][m[1]] = 0;
                if (s > best) { best = s; idx = m[0] * 3 + m[1]; }
            }
            return idx;
        }
        for (int[] m : empt) {
            board[m[0]][m[1]] = 1;
            int w = check();
            board[m[0]][m[1]] = 0;
            if (w == 1) return m[0] * 3 + m[1];
        }
        return pick(empt);
    }

    private int pick(List<int[]> empt) {
        int[] m = empt.get(rnd.nextInt(empt.size()));
        return m[0] * 3 + m[1];
    }

    private int minimax(boolean aiTurn, int depth) {
        int w = check();
        if (w == 2) return 10 - depth;
        if (w == 1) return depth - 10;
        if (w == 3) return 0;
        int best = aiTurn ? -999 : 999;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            if (board[r][c] != 0) continue;
            board[r][c] = aiTurn ? 2 : 1;
            int s = minimax(!aiTurn, depth + 1);
            board[r][c] = 0;
            best = aiTurn ? Math.max(best, s) : Math.min(best, s);
        }
        return best;
    }

    private int localResult(int w) {
        if (!netOn()) return w;
        if (w == 0) return 0;
        return w == myPiece() ? 1 : 2;
    }

    private void record(int result) {
        String key = result == 1 ? "ttt_win" : result == 2 ? "ttt_lose" : "ttt_draw";
        App.prefs().setInt(key, App.prefs().getInt(key, 0) + 1);
        if (result == 1) saveBest(App.prefs().getInt("ttt_win", 0));
    }

    private void settle() {
        if (winner == 0) return;
        int res = localResult(winner);
        record(res);
        if (netOn() && res == 1) netScore[NetSession.get().seat()]++;
        play(res == 1 ? GameSfx.WIN : res == 2 ? GameSfx.FAIL : GameSfx.TAP);
        buzz(40);
        String detail = netOn()
                ? "比分 " + netScore[0] + " : " + netScore[1] + " : " + netScore[2]
                : "胜 " + App.prefs().getInt("ttt_win", 0) + " · 负 " + App.prefs().getInt("ttt_lose", 0) + " · 平 " + App.prefs().getInt("ttt_draw", 0);
        String title = res == 1 ? "你赢了！" : res == 2 ? (netOn() ? "对手赢了" : "AI 赢了") : "平局";
        showResult(res == 1, title, detail, () -> {
            if (netOn()) NetSession.get().send("RESET", "");
            reset();
            invalidate();
        });
    }

    @Override
    public void onNet(int from, String action, String payload) {
        if ("RESET".equals(action)) { reset(); invalidate(); return; }
        if ("MV".equals(action)) {
            String[] f = payload.split(",");
            if (f.length < 2) return;
            try {
                int r = Integer.parseInt(f[0]), c = Integer.parseInt(f[1]);
                if (r < 0 || r >= 3 || c < 0 || c >= 3 || board[r][c] != 0) return;
                board[r][c] = from == 0 ? 1 : 2;
                winner = check();
                playerTurn = true;
                if (winner != 0) settle(); else play(GameSfx.CLICK);
                invalidate();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void drawGame(Canvas c) {
        boolean net = netOn();
        NetSession ns = NetSession.get();
        String right = net ? "联机 · " + ns.name() : "胜 " + App.prefs().getInt("ttt_win", 0) +
                " · 负 " + App.prefs().getInt("ttt_lose", 0) +
                " · 平 " + App.prefs().getInt("ttt_draw", 0);
        header(c, "井字棋", right, 0xFF3B82F6);
        float s = Math.min(field.width() * 0.8f, field.height() - dp(180));
        s = Math.min(s, dp(120));
        float ox = W / 2f - s * 1.5f;
        float oy = field.top + (field.height() - s * 3 - dp(160)) / 2 + dp(20);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.PANEL);
        c.drawRoundRect(ox - dp(8), oy - dp(8), ox + s * 3 + dp(8), oy + s * 3 + dp(8), dp(14), dp(14), paint);
        for (int r = 0; r < 3; r++) for (int col = 0; col < 3; col++) {
            float x = ox + col * s, y = oy + r * s;
            paint.setColor(((r + col) % 2 == 0) ? 0xFF121A24 : 0xFF0F161F);
            c.drawRect(x + 1, y + 1, x + s - 1, y + s - 1, paint);
            int v = board[r][col];
            if (v == 0) continue;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(7));
            paint.setColor(v == 1 ? Ui.ACCENT : 0xFFF87171);
            if (v == 1) {
                c.drawLine(x + s * 0.28f, y + s * 0.28f, x + s * 0.72f, y + s * 0.72f, paint);
                c.drawLine(x + s * 0.72f, y + s * 0.28f, x + s * 0.28f, y + s * 0.72f, paint);
            } else {
                c.drawCircle(x + s / 2, y + s / 2, s * 0.26f, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
        if (winner != 0 && line != null) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(5));
            paint.setColor(Ui.GOLD);
            c.drawLine(ox + (line[0] % 3 + 0.5f) * s, oy + (line[0] / 3 + 0.5f) * s,
                    ox + (line[2] % 3 + 0.5f) * s, oy + (line[2] / 3 + 0.5f) * s, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(16));
        paint.setFakeBoldText(true);
        int res = localResult(winner);
        paint.setColor(res == 1 ? Ui.OK : res == 2 ? Ui.BAD : Ui.MUTED);
        String msg;
        if (winner != 0) msg = res == 1 ? "你赢了！" : res == 2 ? (net ? "对手赢了" : "AI 赢了") : "平局";
        else if (net) msg = playerTurn ? "轮到你（" + (myPiece() == 1 ? "X" : "O") + "）" : "等待对手落子…";
        else msg = "轮到你（X）";
        c.drawText(msg, W / 2f, oy + s * 3 + dp(32), paint);
        paint.setFakeBoldText(false);

        paint.setColor(Ui.PANEL2);
        c.drawRoundRect(restart, dp(9), dp(9), paint);
        paint.setColor(Ui.TEXT);
        paint.setTextSize(dp(13));
        c.drawText(net ? "重开一局" : "再来一局", W / 2f, H - dp(88), paint);
        String[] names = {"简单", "普通", "困难"};
        for (int i = 0; i < 3; i++) {
            paint.setColor(!net && difficulty == i ? Ui.PRIMARY : Ui.PANEL2);
            c.drawRoundRect(levels[i], dp(9), dp(9), paint);
            paint.setColor(Ui.TEXT);
            paint.setTextSize(dp(13));
            c.drawText(names[i], levels[i].centerX(), H - dp(36), paint);
        }
        if (net) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(dp(12));
            String[] all = ns.names();
            String info = "我 " + ns.name() + " · 对手 " + (all[1 - ns.seat()] == null || all[1 - ns.seat()].isEmpty() ? "等待中" : all[1 - ns.seat()])
                    + " · 比分 " + netScore[ns.seat()] + ":" + netScore[1 - ns.seat()];
            c.drawText(info, W / 2f, field.top - dp(4), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return;
        float x = e.getX(), y = e.getY();
        if (restart.contains(x, y)) {
            if (netOn()) NetSession.get().send("RESET", "");
            reset();
            invalidate();
            return;
        }
        boolean net = netOn();
        if (!net) {
            for (int i = 0; i < 3; i++) {
                if (levels[i].contains(x, y)) { difficulty = i; reset(); invalidate(); return; }
            }
        }
        if (winner != 0 || !playerTurn) return;
        float s = Math.min(Math.min(field.width() * 0.8f, field.height() - dp(180)), dp(120));
        float ox = W / 2f - s * 1.5f;
        float oy = field.top + (field.height() - s * 3 - dp(160)) / 2 + dp(20);
        int col = (int) ((x - ox) / s);
        int row = (int) ((y - oy) / s);
        if (col < 0 || col >= 3 || row < 0 || row >= 3 || board[row][col] != 0) return;
        board[row][col] = net ? myPiece() : 1;
        winner = check();
        if (net) {
            playerTurn = false;
            NetSession.get().send("MV", row + "," + col);
            if (winner != 0) settle(); else play(GameSfx.TAP);
        } else if (winner == 0) {
            playerTurn = false;
            int m = findMove(2);
            if (m >= 0) board[m / 3][m % 3] = 2;
            winner = check();
            playerTurn = true;
            play(GameSfx.TAP);
        }
        if (winner != 0 && !net) settle();
        invalidate();
    }
}
