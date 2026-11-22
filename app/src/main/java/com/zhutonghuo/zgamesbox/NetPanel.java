package com.zhutonghuo.zgamesbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.zhutonghuo.zgamesbox.core.NetController;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.core.Ui;

public class NetPanel extends View {
    private final NetController ctl;
    private final Paint p = Ui.paint(Ui.TEXT);
    private final RectF fab = new RectF();
    private final RectF card = new RectF();
    private final RectF bHost = new RectF();
    private final RectF bJoin = new RectF();
    private final RectF bLeave = new RectF();
    private final RectF bClose = new RectF();
    private boolean expanded;
    private boolean pressedFab;
    private int w, h;

    public NetPanel(Context c, NetController ctl) {
        super(c);
        this.ctl = ctl;
        setWillNotDraw(false);
        NetSession.get().setStatusListener(this::refresh);
    }

    public boolean isExpanded() { return expanded; }
    public void expand() { expanded = true; refresh(); }
    public void collapse() { expanded = false; refresh(); }
    public void refresh() { invalidate(); }

    @Override
    protected void onSizeChanged(int W, int H, int oldw, int oldh) {
        super.onSizeChanged(W, H, oldw, oldh);
        w = W; h = H;
        float d = Ui.dp(getContext(), 46);
        float m = Ui.dp(getContext(), 14);
        fab.set(W - m - d, Ui.dp(getContext(), 76), W - m, Ui.dp(getContext(), 76) + d);
        float cw = Math.min(Ui.dp(getContext(), 320), W - Ui.dp(getContext(), 48));
        float chh = Ui.dp(getContext(), 340);
        card.set(W / 2f - cw / 2, H / 2f - chh / 2, W / 2f + cw / 2, H / 2f + chh / 2);
        float bw = cw - Ui.dp(getContext(), 48);
        float bx = card.left + Ui.dp(getContext(), 24);
        float by = card.top + Ui.dp(getContext(), 158);
        float bh = Ui.dp(getContext(), 46);
        bHost.set(bx, by, bx + bw, by + bh);
        bJoin.set(bx, by + bh + Ui.dp(getContext(), 10), bx + bw, by + bh * 2 + Ui.dp(getContext(), 10));
        bLeave.set(bx, bJoin.bottom + Ui.dp(getContext(), 10), bx + bw, bJoin.bottom + Ui.dp(getContext(), 10) + bh);
        bClose.set(card.right - Ui.dp(getContext(), 42), card.top + Ui.dp(getContext(), 8),
                card.right - Ui.dp(getContext(), 10), card.top + Ui.dp(getContext(), 40));
    }

    @Override
    protected void onDraw(Canvas c) {
        NetSession ns = NetSession.get();
        if (!expanded) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(ns.isOnline() ? Ui.PRIMARY : Ui.PANEL2);
            c.drawRoundRect(fab, Ui.dp(getContext(), 23), Ui.dp(getContext(), 23), p);
            p.setColor(ns.isOnline() ? 0xFFFFFFFF : Ui.TEXT);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Ui.dp(getContext(), 15));
            p.setFakeBoldText(true);
            c.drawText("联机", fab.centerX(), fab.centerY() + Ui.dp(getContext(), 5), p);
            p.setFakeBoldText(false);
            p.setTextAlign(Paint.Align.LEFT);
            return;
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x99050810);
        c.drawRect(0, 0, w, h, p);
        p.setColor(Ui.PANEL);
        c.drawRoundRect(card, Ui.dp(getContext(), 18), Ui.dp(getContext(), 18), p);
        p.setColor(Ui.PRIMARY);
        c.drawRoundRect(bClose, Ui.dp(getContext(), 8), Ui.dp(getContext(), 8), p);
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Ui.dp(getContext(), 18));
        c.drawText("×", bClose.centerX(), bClose.centerY() + Ui.dp(getContext(), 6), p);

        p.setColor(Ui.TEXT);
        p.setTextSize(Ui.dp(getContext(), 19));
        p.setFakeBoldText(true);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("局域网联机", card.left + Ui.dp(getContext(), 24), card.top + Ui.dp(getContext(), 42), p);
        p.setFakeBoldText(false);

        float ty = card.top + Ui.dp(getContext(), 74);
        p.setTextSize(Ui.dp(getContext(), 13));
        if (ns.isHosting()) {
            p.setColor(Ui.OK);
            c.drawText("● 房主 · 房间已开", card.left + Ui.dp(getContext(), 24), ty, p);
            ty += Ui.dp(getContext(), 21);
            p.setColor(Ui.TEXT);
            c.drawText("房间号  " + ns.room(), card.left + Ui.dp(getContext(), 24), ty, p);
            ty += Ui.dp(getContext(), 19);
            c.drawText("IP  " + NetSession.localIPv4(), card.left + Ui.dp(getContext(), 24), ty, p);
            ty += Ui.dp(getContext(), 19);
            c.drawText("端口  " + ns.port(), card.left + Ui.dp(getContext(), 24), ty, p);
        } else if (ns.isOnline()) {
            p.setColor(Ui.OK);
            c.drawText("● 已加入 · 座位 " + (ns.seat() + 1), card.left + Ui.dp(getContext(), 24), ty, p);
            ty += Ui.dp(getContext(), 21);
            p.setColor(Ui.TEXT);
            c.drawText("房间  " + ns.room() + "   " + ns.clientIp() + ":" + ns.port(), card.left + Ui.dp(getContext(), 24), ty, p);
        } else {
            p.setColor(Ui.MUTED);
            c.drawText("未连接。开房后把房间号、IP、端口告诉朋友；", card.left + Ui.dp(getContext(), 24), ty, p);
            ty += Ui.dp(getContext(), 19);
            c.drawText("加入者依次填 IP、端口、房间号、名字即可。", card.left + Ui.dp(getContext(), 24), ty, p);
        }

        String[] names = ns.names();
        ty = card.top + Ui.dp(getContext(), 146);
        p.setColor(Ui.MUTED);
        p.setTextSize(Ui.dp(getContext(), 12));
        StringBuilder sb = new StringBuilder("玩家：");
        for (int i = 0; i < 3; i++) {
            String nm = names[i];
            sb.append(i == 0 ? "房主" : ("P" + (i + 1))).append("=")
                    .append(nm == null || nm.isEmpty() ? "空" : nm).append("  ");
        }
        c.drawText(sb.toString(), card.left + Ui.dp(getContext(), 24), ty, p);

        drawBtn(c, bHost, ns.isOnline() ? "已连接" : "开房（我建房间）", Ui.PRIMARY, !ns.isOnline());
        drawBtn(c, bJoin, ns.isOnline() ? "已连接" : "加入朋友的房间", Ui.ACCENT, !ns.isOnline());
        drawBtn(c, bLeave, "断开连接", Ui.BAD, ns.isOnline());
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawBtn(Canvas c, RectF r, String text, int color, boolean enabled) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(enabled ? color : Ui.PANEL2);
        c.drawRoundRect(r, Ui.dp(getContext(), 11), Ui.dp(getContext(), 11), p);
        p.setColor(enabled ? 0xFFFFFFFF : Ui.MUTED);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Ui.dp(getContext(), 14));
        p.setFakeBoldText(true);
        c.drawText(text, r.centerX(), r.centerY() + Ui.dp(getContext(), 5), p);
        p.setFakeBoldText(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        float x = e.getX(), y = e.getY();
        if (a == MotionEvent.ACTION_DOWN) {
            pressedFab = !expanded && fab.contains(x, y);
            return expanded || pressedFab;
        }
        if (a == MotionEvent.ACTION_CANCEL) { pressedFab = false; return false; }
        boolean wasPressed = pressedFab;
        if (a == MotionEvent.ACTION_UP) pressedFab = false;
        if (!expanded && !wasPressed) return false;
        if (a != MotionEvent.ACTION_UP) return expanded || wasPressed;
        if (!expanded) {
            if (fab.contains(x, y)) { ctl.open(); return true; }
            return false;
        }
        if (bClose.contains(x, y)) { collapse(); return true; }
        NetSession ns = NetSession.get();
        if (!ns.isOnline() && bHost.contains(x, y)) { ctl.askHost(); return true; }
        if (!ns.isOnline() && bJoin.contains(x, y)) { ctl.askJoin(); return true; }
        if (ns.isOnline() && bLeave.contains(x, y)) {
            ctl.confirm("断开连接", "确定离开当前房间？", () -> { NetSession.get().leave(); refresh(); });
            return true;
        }
        if (card.contains(x, y)) return true;
        collapse();
        return true;
    }
}
