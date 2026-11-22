package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.App;
import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.core.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GomokuView extends BaseGameView {
    private static final int N = 15;
    private final int[][] board = new int[N][N];
    private final List<int[]> moves = new ArrayList<>();
    private final RectF[] buttons = new RectF[5];
    private final Random rnd = new Random();
    private final int[] netScore = new int[3];
    private int difficulty = 1;
    private int winner;
    private int[] last;
    private boolean playerTurn, thinking;

    public GomokuView(Context c) {
        super(c);
        for (int i = 0; i < 5; i++) buttons[i] = new RectF();
        reset();
    }

    @Override
    protected String gameId() { return "gomoku"; }

    @Override
    protected void update(float dt, long now) {}

    @Override
    protected boolean continuous() { return false; }

    @Override
    protected void onLayoutChanged() {
        float w = Math.min(dp(50), (W - dp(60)) / 5);
        float start = W / 2f - w * 2.5f - dp(16);
        for (int i = 0; i < 5; i++) {
            buttons[i].set(start + i * (w + dp(6)), H - dp(58),
                    start + i * (w + dp(6)) + w, H - dp(12));
        }
    }

    @Override
    protected void onReset() { reset(); }

    private boolean netOn() {
        NetSession n = NetSession.get();
        return n.isOnline() && n.seat() < 2 && n.peerCount() >= 2;
    }

    private int myPiece() { return NetSession.get().seat() == 0 ? 1 : 2; }

    private void reset() {
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) board[r][c] = 0;
        moves.clear();
        winner = 0;
        last = null;
        playerTurn = !netOn() || NetSession.get().seat() == 0;
        thinking = false;
    }

    private float cell() {
        return Math.min(field.width(), field.height() - dp(74)) / N;
    }

    private float ox() { return W / 2f - cell() * N / 2; }
    private float oy() { return field.top + (field.height() - cell() * N - dp(74)) / 2; }

    private int count(int x, int y, int dx, int dy, int p) {
        int n = 0;
        int cx = x + dx, cy = y + dy;
        while (cx >= 0 && cy >= 0 && cx < N && cy < N && board[cy][cx] == p) {
            n++;
            cx += dx;
            cy += dy;
        }
        return n;
    }

    private boolean isWin(int x, int y, int p) {
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            if (count(x, y, d[0], d[1], p) + count(x, y, -d[0], -d[1], p) + 1 >= 5) return true;
        }
        return false;
    }

    private int scorePoint(int x, int y, int p) {
        board[y][x] = p;
        int total = 0;
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int a = count(x, y, d[0], d[1], p);
            int b = count(x, y, -d[0], -d[1], p);
            int n = a + b + 1;
            int ax = x + d[0] * (a + 1), ay = y + d[1] * (a + 1);
            int bx = x - d[0] * (b + 1), by = y - d[1] * (b + 1);
            boolean openA = ax >= 0 && ay >= 0 && ax < N && ay < N && board[ay][ax] == 0;
            boolean openB = bx >= 0 && by >= 0 && bx < N && by < N && board[by][bx] == 0;
            int s;
            if (n >= 5) s = 100000;
            else if (n == 4) s = (openA && openB) ? 12000 : (openA || openB ? 1800 : 0);
            else if (n == 3) s = (openA && openB) ? 1600 : (openA || openB ? 180 : 0);
            else if (n == 2) s = (openA && openB) ? 160 : 20;
            else s = 10;
            total += s;
        }
        board[y][x] = 0;
        return total;
    }

    private int aiMove() {
        List<int[]> cand = new ArrayList<>();
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (board[r][c] != 0) continue;
            boolean near = false;
            for (int dr = -2; dr <= 2 && !near; dr++) for (int dc = -2; dc <= 2; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nc >= 0 && nr < N && nc < N && board[nr][nc] != 0) { near = true; break; }
            }
            if (near) cand.add(new int[]{c, r});
        }
        if (cand.isEmpty()) return 7 * N + 7;
        float best = -1;
        int bestIdx = 0;
        for (int[] q : cand) {
            float atk = scorePoint(q[0], q[1], 2);
            float def = scorePoint(q[0], q[1], 1);
            float s = atk + def * (difficulty == 0 ? 0.25f : difficulty == 1 ? 0.8f : 1.15f);
            s += rnd.nextFloat() * (difficulty == 0 ? 900 : difficulty == 1 ? 90 : 2);
            if (s > best) { best = s; bestIdx = q[1] * N + q[0]; }
        }
        return bestIdx;
    }

    private int localWinner(int p) {
        if (!netOn()) return p;
        return p == myPiece() ? 1 : 2;
    }

    private void place(int x, int y, int p) {
        if (x < 0 || y < 0 || x >= N || y >= N || board[y][x] != 0) return;
        board[y][x] = p;
        last = new int[]{x, y};
        moves.add(new int[]{x, y, p});
        if (isWin(x, y, p)) {
            winner = p;
            int res = localWinner(p);
            if (res == 1) {
                App.prefs().setInt("gomoku_win", App.prefs().getInt("gomoku_win", 0) + 1);
                saveBest(App.prefs().getInt("gomoku_win", 0));
                if (netOn()) netScore[NetSession.get().seat()]++;
                play(GameSfx.WIN);
            } else {
                play(GameSfx.FAIL);
            }
            buzz(80);
            String detail = netOn()
                    ? "比分 " + netScore[0] + " : " + netScore[1] + " : " + netScore[2]
                    : "累计胜场 " + App.prefs().getInt("gomoku_win", 0);
            showResult(res == 1, res == 1 ? "你赢了！" : (netOn() ? "对手获胜" : "AI 获胜"), detail, () -> {
                if (netOn()) NetSession.get().send("RESET", "");
                reset();
                invalidate();
            });
        } else {
            play(p == 1 ? GameSfx.TAP : GameSfx.CLICK);
        }
    }

    private void undo() {
        if (winner != 0 || thinking || moves.isEmpty()) return;
        int k = 2;
        while (k > 0 && !moves.isEmpty()) {
            int[] m = moves.remove(moves.size() - 1);
            board[m[1]][m[0]] = 0;
            k--;
        }
        last = moves.isEmpty() ? null : new int[]{moves.get(moves.size() - 1)[0], moves.get(moves.size() - 1)[1]};
    }

    @Override
    public void onNet(int from, String action, String payload) {
        if ("RESET".equals(action)) { reset(); invalidate(); return; }
        if ("MV".equals(action)) {
            String[] f = payload.split(",");
            if (f.length < 2) return;
            try {
                int x = Integer.parseInt(f[0]), y = Integer.parseInt(f[1]);
                if (x < 0 || y < 0 || x >= N || y >= N || board[y][x] != 0) return;
                place(x, y, from == 0 ? 1 : 2);
                playerTurn = true;
                thinking = false;
                invalidate();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void drawGame(Canvas c) {
        boolean net = netOn();
        NetSession ns = NetSession.get();
        String info = net ? "联机 · " + ns.name() : "胜 " + App.prefs().getInt("gomoku_win", 0) + " · " +
                new String[]{"简单", "普通", "困难"}[difficulty];
        header(c, "五子棋", info, 0xFFEAB308);
        float s = cell();
        float ox = ox(), oy = oy();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFD9A76A);
        c.drawRect(ox - s * 0.5f, oy - s * 0.5f, ox + s * (N - 0.5f), oy + s * (N - 0.5f), paint);
        paint.setColor(0xFF8A5A32);
        paint.setStrokeWidth(Math.max(1f, s * 0.035f));
        for (int i = 0; i < N; i++) {
            c.drawLine(ox + i * s, oy, ox + i * s, oy + (N - 1) * s, paint);
            c.drawLine(ox, oy + i * s, ox + (N - 1) * s, oy + i * s, paint);
        }
        for (int[] star : new int[][]{{3, 3}, {3, 11}, {11, 3}, {11, 11}, {7, 7}}) {
            c.drawCircle(ox + star[0] * s, oy + star[1] * s, Math.max(2f, s * 0.09f), paint);
        }
        for (int r = 0; r < N; r++) for (int col = 0; col < N; col++) {
            int v = board[r][col];
            if (v == 0) continue;
            float cx = ox + col * s, cy = oy + r * s;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx, cy, s * 0.5f,
                    v == 1 ? 0xFFFFFFFF : 0xFF6B7280,
                    v == 1 ? 0xFFD1D5DB : 0xFF111827, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, s * 0.44f, paint);
            paint.setShader(null);
        }
        if (last != null) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Ui.ACCENT);
            paint.setStrokeWidth(dp(2));
            c.drawCircle(ox + last[0] * s, oy + last[1] * s, s * 0.52f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        String[] names = net ? new String[]{"重开", "认输", "", "", ""} :
                new String[]{"简单", "普通", "困难", "悔棋", "重开"};
        for (int i = 0; i < 5; i++) {
            if (names[i].isEmpty()) continue;
            paint.setColor(!net && i == difficulty ? Ui.PRIMARY : Ui.PANEL2);
            c.drawRoundRect(buttons[i], dp(9), dp(9), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(12));
            paint.setColor(Ui.TEXT);
            c.drawText(names[i], buttons[i].centerX(), buttons[i].centerY() + dp(4), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        if (net) {
            String[] all = ns.names();
            String opponent = all[1 - ns.seat()];
            paint.setColor(Ui.MUTED);
            paint.setTextSize(dp(12));
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText("我 " + ns.name() + " · 对手 " + (opponent == null || opponent.isEmpty() ? "等待中" : opponent)
                            + " · " + netScore[ns.seat()] + ":" + netScore[1 - ns.seat()],
                    W / 2f, field.top - dp(4), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        if (winner != 0) {
            paint.setColor(0xCC0A0E14);
            c.drawRect(ox - s, oy - s, ox + s * N, oy + s * N, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(30));
            paint.setColor(localWinner(winner) == 1 ? Ui.OK : Ui.BAD);
            c.drawText(localWinner(winner) == 1 ? "你赢了！" : (net ? "对手获胜" : "AI 获胜"), W / 2f, oy + s * N / 2f, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(13));
            paint.setColor(Ui.TEXT);
            c.drawText("战报已弹出", W / 2f, oy + s * N / 2f + dp(30), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onGameTouch(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return;
        float x = e.getX(), y = e.getY();
        boolean net = netOn();
        for (int i = 0; i < 5; i++) {
            if (!buttons[i].contains(x, y)) continue;
            if (net) {
                if (i == 0) { NetSession.get().send("RESET", ""); reset(); }
                else if (i == 1) {
                    winner = 2;
                    showResult(false, "已认输", "下次再来一局", () -> {
                        NetSession.get().send("RESET", "");
                        reset();
                        invalidate();
                    });
                }
            } else {
                if (i < 3) { difficulty = i; reset(); }
                else if (i == 3) undo();
                else reset();
            }
            invalidate();
            return;
        }
        if (winner != 0) { if (net) NetSession.get().send("RESET", ""); reset(); invalidate(); return; }
        if (!playerTurn || thinking) return;
        float s = cell();
        int col = Math.round((x - ox()) / s);
        int row = Math.round((y - oy()) / s);
        if (col < 0 || row < 0 || col >= N || row >= N || board[row][col] != 0) return;
        int piece = net ? myPiece() : 1;
        place(col, row, piece);
        if (winner == 0 && net) {
            playerTurn = false;
            NetSession.get().send("MV", col + "," + row);
        } else if (winner == 0) {
            playerTurn = false;
            thinking = true;
            postGame(() -> {
                int m = aiMove();
                place(m % N, m / N, 2);
                thinking = false;
                playerTurn = true;
                invalidate();
            }, 320);
        }
        invalidate();
    }
}
