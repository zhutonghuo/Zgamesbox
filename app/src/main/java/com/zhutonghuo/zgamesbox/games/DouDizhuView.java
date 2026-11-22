package com.zhutonghuo.zgamesbox.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameSfx;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.core.Ui;
import com.zhutonghuo.zgamesbox.games.doudizhu.Card;
import com.zhutonghuo.zgamesbox.games.doudizhu.Combo;
import com.zhutonghuo.zgamesbox.games.doudizhu.DouDizhuAI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DouDizhuView extends BaseGameView {
    @SuppressWarnings("unchecked")
    private final List<Card>[] hands = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    private final List<Card> deck = new ArrayList<>();
    private final List<Card> bottom = new ArrayList<>();
    private final boolean[] sel = new boolean[54];
    private final RectF[] buttons = new RectF[3];
    private final RectF[] opp = new RectF[2];
    private final RectF cardRect = new RectF();
    private final Random rnd = new Random();
    private final String[] action = {"", "", ""};
    private int landlord = -1;
    private int current;
    private int lastPlayer = -1;
    private int passCount;
    private int phase;
    private int multiplier = 1;
    private int result;
    private Combo lastCombo;
    private String tip = "";
    private boolean netReady;
    private boolean resultShown;

    public DouDizhuView(Context c) {
        super(c);
        for (int i = 0; i < 3; i++) buttons[i] = new RectF();
        for (int i = 0; i < 2; i++) opp[i] = new RectF();
        deal();
    }

    @Override
    protected String gameId() { return "doudizhu"; }

    @Override
    protected boolean continuous() { return false; }

    @Override
    protected void onLayoutChanged() {
        float bw = Math.min(dp(86), (W - dp(60)) / 3);
        float start = W / 2f - bw * 1.5f - dp(6);
        for (int i = 0; i < 3; i++) {
            buttons[i].set(start + i * (bw + dp(6)), H - dp(160),
                    start + i * (bw + dp(6)) + bw, H - dp(120));
        }
        opp[0].set(dp(12), dp(78), dp(104), dp(164));
        opp[1].set(W - dp(104), dp(78), W - dp(12), dp(164));
    }

    @Override
    protected void onReset() { deal(); }

    @Override
    public void release() { super.release(); }

    private NetSession net() { return NetSession.get(); }
    private boolean netOn() { return netReady && net().isOnline(); }

    private void deal() {
        deck.clear();
        int uid = 0;
        for (int r = 3; r <= 15; r++) for (int s = 0; s < 4; s++) deck.add(new Card(r, s, uid++));
        deck.add(new Card(16, 4, uid++));
        deck.add(new Card(17, 4, uid++));
        Collections.shuffle(deck, rnd);
        for (int p = 0; p < 3; p++) hands[p].clear();
        bottom.clear();
        for (int i = 0; i < 17; i++) for (int p = 0; p < 3; p++) hands[p].add(deck.get(i * 3 + p));
        for (int i = 0; i < 3; i++) bottom.add(deck.get(51 + i));
        for (int p = 0; p < 3; p++) sort(p);
        for (int i = 0; i < 54; i++) sel[i] = false;
        landlord = -1;
        current = 0;
        lastPlayer = -1;
        lastCombo = null;
        passCount = 0;
        phase = 0;
        multiplier = 1;
        result = 0;
        resultShown = false;
        action[0] = "";
        action[1] = "";
        action[2] = "";
        netReady = net().isOnline();
        tip = netReady ? (current == net().seat() ? "轮到你叫地主" : "等待房主发牌…") : "轮到你叫地主";
        if (netOn() && net().seat() == 0) broadcastDeal();
    }

    private void sort(int p) {
        hands[p].sort((a, b) -> b.rank - a.rank);
    }

    private String name(int p) {
        if (!netOn()) return p == 0 ? "你" : p == 1 ? "左家" : "右家";
        int me = net().seat();
        if (p == me) return "你";
        String[] all = net().names();
        String nm = all[p];
        return nm == null || nm.isEmpty() ? (p == 1 ? "左家" : "右家") : nm;
    }

    private String encode(List<Card> cs) {
        StringBuilder sb = new StringBuilder();
        for (Card c : cs) { if (sb.length() > 0) sb.append(','); sb.append(c.uid); }
        return sb.toString();
    }

    private List<Card> decode(String s) {
        List<Card> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(",")) {
            try {
                int uid = Integer.parseInt(part.trim());
                if (uid >= 0 && uid < deck.size()) out.add(deck.get(uid));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private void rebuildDeck() {
        deck.clear();
        int uid = 0;
        for (int r = 3; r <= 15; r++) for (int s = 0; s < 4; s++) deck.add(new Card(r, s, uid++));
        deck.add(new Card(16, 4, uid++));
        deck.add(new Card(17, 4, uid++));
    }

    private void broadcastDeal() {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < 3; p++) {
            if (p > 0) sb.append('|');
            sb.append(encode(hands[p]));
        }
        sb.append('|').append(encode(bottom));
        net().send("DEAL", sb.toString());
    }

    private void beLandlord(int p) {
        landlord = p;
        hands[p].addAll(bottom);
        sort(p);
        phase = 1;
        current = p;
        lastCombo = null;
        lastPlayer = -1;
        passCount = 0;
        action[0] = "";
        action[1] = "";
        action[2] = "";
        tip = name(p) + "先出牌";
        play(GameSfx.TAP);
        if (netOn()) net().send("LL", String.valueOf(p));
        invalidate();
        if (phase != 2 && (p != net().seat() || !netOn()) && current != net().seat()) scheduleAiIfNeeded();
    }

    private void bidPass(int p) {
        action[p] = "不叫";
        if (p == 0) {
            int s1 = DouDizhuAI.strength(hands[1]);
            int s2 = DouDizhuAI.strength(hands[2]);
            if (s1 >= s2 && s1 >= 11) beLandlord(1);
            else if (s2 >= 11) beLandlord(2);
            else {
                tip = "三家都不叫，重新发牌";
                play(GameSfx.CLICK);
                postGame(() -> { deal(); invalidate(); }, 900);
            }
        }
        if (netOn()) net().send("BIDP", String.valueOf(p));
        invalidate();
    }

    private boolean isAiSeat(int p) {
        if (!netOn()) return p != 0;
        String[] all = net().names();
        return all[p] == null || all[p].isEmpty();
    }

    private void scheduleAiIfNeeded() {
        if (!netOn() && current == 0) return;
        if (netOn() && !isAiSeat(current)) return;
        postGame(() -> {
            if (phase != 1) return;
            aiTurn(current);
        }, 750);
    }

    private void aiTurn(int p) {
        int foe = p == 0 ? Math.min(hands[1].size(), hands[2].size())
                : Math.min(hands[0].size(), hands[(p == 1 ? 2 : 1)].size());
        List<Card> play = DouDizhuAI.choose(hands[p], lastCombo, lastCombo == null, foe);
        if (play == null || play.isEmpty()) {
            action[p] = "不出";
            passCount++;
            play(GameSfx.CLICK);
            if (netOn()) net().send("PASS", String.valueOf(p));
        } else {
            applyPlay(p, play);
            if (phase == 2) return;
            if (netOn()) net().send("PLAY", p + ":" + encode(play));
        }
        nextTurn();
        invalidate();
    }

    private void applyPlay(int p, List<Card> cs) {
        for (Card c : cs) {
            hands[p].removeIf(q -> q.uid == c.uid);
            sel[c.uid] = false;
        }
        Combo cmb = Combo.recognize(cs);
        lastCombo = cmb;
        lastPlayer = p;
        passCount = 0;
        action[p] = cmb == null ? "出牌" : cmb.name();
        tip = name(p) + " 出了 " + action[p];
        if (cmb != null && (cmb.type == Combo.BOMB || cmb.type == Combo.ROCKET)) {
            multiplier *= 2;
            buzz(50);
        }
        play(GameSfx.TAP);
        if (hands[p].isEmpty()) finish(p);
    }

    private void nextTurn() {
        if (passCount >= 2 && lastCombo != null) {
            current = lastPlayer;
            lastCombo = null;
            passCount = 0;
            tip = name(current) + "自由出牌";
        } else {
            current = (current + 1) % 3;
        }
        if (current == mySeatIndex()) tip = "轮到你出牌";
        else scheduleAiIfNeeded();
    }

    private void finish(int p) {
        phase = 2;
        result = (p == mySeatIndex()) ? 1 : (p == landlord ? 2 : (landlord == mySeatIndex() ? 2 : 1));
        net().send("END", p + ":" + multiplier);
        play(result == 1 ? GameSfx.WIN : GameSfx.FAIL);
        buzz(120);
        if (result == 1) {
            saveBest(Math.max(best(), multiplier * 100));
            recordWin();
        }
        popResult();
    }

    private void popResult() {
        resultShown = true;
        showResult(result == 1, result == 1 ? "你赢了！" : "你输了",
                "倍数 ×" + multiplier + " · 最高 " + best(), () -> {
                    if (netOn() && net().seat() == 0) net().send("RESET", "");
                    rebuildDeck();
                    deal();
                    invalidate();
                });
    }

    private void clearSel() {
        for (int i = 0; i < 54; i++) sel[i] = false;
    }

    private List<Card> selectedCards() {
        List<Card> out = new ArrayList<>();
        for (Card c : hands[netOn() ? net().seat() : 0]) if (sel[c.uid]) out.add(c);
        return out;
    }

    private int mySeatIndex() { return netOn() ? net().seat() : 0; }

    private void playerPlay() {
        int me = mySeatIndex();
        if (lastCombo != null && lastPlayer == me) return;
        List<Card> cs = selectedCards();
        if (cs.isEmpty()) { tip = "先点选要出的牌"; invalidate(); return; }
        Combo cmb = Combo.recognize(cs);
        if (cmb == null) { tip = "牌型不合法"; play(GameSfx.FAIL); invalidate(); return; }
        if (!Combo.beats(cmb, lastCombo)) { tip = "压不过上家"; play(GameSfx.FAIL); invalidate(); return; }
        clearSel();
        applyPlay(me, cs);
        if (netOn()) net().send("PLAY", me + ":" + encode(cs));
        if (phase == 2) { invalidate(); return; }
        nextTurn();
        invalidate();
    }

    private void playerPass() {
        int me = mySeatIndex();
        if (lastCombo == null || lastPlayer == me) { tip = "你现在必须出牌"; invalidate(); return; }
        action[me] = "不出";
        passCount++;
        play(GameSfx.CLICK);
        if (netOn()) net().send("PASS", String.valueOf(me));
        nextTurn();
        invalidate();
    }

    private void hint() {
        int me = mySeatIndex();
        int foe = Math.min(hands[(me + 1) % 3].size(), hands[(me + 2) % 3].size());
        List<Card> best = DouDizhuAI.choose(hands[me], lastCombo, lastCombo == null, foe);
        clearSel();
        if (best == null) { tip = "没有能压过的牌，选择不出"; }
        else {
            for (Card c : best) sel[c.uid] = true;
            Combo cmb = Combo.recognize(best);
            tip = "提示：" + (cmb == null ? "出牌" : cmb.name());
            play(GameSfx.CLICK);
        }
        invalidate();
    }

    @Override
    public void onNet(int from, String act, String payload) {
        if (!net().isOnline()) return;
        switch (act) {
            case "DEAL": {
                rebuildDeck();
                String[] f = payload.split("\\|", -1);
                if (f.length < 4) return;
                for (int p = 0; p < 3; p++) hands[p].clear();
                bottom.clear();
                for (int p = 0; p < 3; p++) hands[p].addAll(decode(f[p]));
                bottom.addAll(decode(f[3]));
                for (int p = 0; p < 3; p++) sort(p);
                clearSel();
                landlord = -1;
                current = 0;
                lastPlayer = -1;
                lastCombo = null;
                passCount = 0;
                phase = 0;
                multiplier = 1;
                result = 0;
                resultShown = false;
                action[0] = action[1] = action[2] = "";
                netReady = true;
                tip = current == net().seat() ? "轮到你叫地主" : "等待其他人叫地主…";
                invalidate();
                break;
            }
            case "LL": {
                int p = parseFirst(payload);
                if (p < 0 || p > 2) return;
                landlord = p;
                hands[p].addAll(bottom);
                bottom.clear();
                sort(p);
                phase = 1;
                current = p;
                lastCombo = null;
                lastPlayer = -1;
                passCount = 0;
                tip = name(p) + "先出牌";
                play(GameSfx.TAP);
                invalidate();
                if (current != net().seat() && isAiSeat(current)) scheduleAiIfNeeded();
                break;
            }
            case "BIDP": {
                int p = parseFirst(payload);
                if (p >= 0 && p <= 2) { action[p] = "不叫"; tip = name(p) + "不叫"; invalidate(); }
                break;
            }
            case "PLAY": {
                String[] f = payload.split(":", 2);
                if (f.length < 2) return;
                int p;
                try { p = Integer.parseInt(f[0]); } catch (Exception e) { return; }
                if (p < 0 || p > 2) return;
                applyPlay(p, decode(f[1]));
                if (phase == 2) { invalidate(); return; }
                nextTurn();
                invalidate();
                break;
            }
            case "PASS": {
                int p = parseFirst(payload);
                if (p < 0 || p > 2) return;
                action[p] = "不出";
                passCount++;
                play(GameSfx.CLICK);
                nextTurn();
                invalidate();
                break;
            }
            case "END": {
                String[] f = payload.split(":", 2);
                try {
                    int p = Integer.parseInt(f[0]);
                    multiplier = f.length > 1 ? Integer.parseInt(f[1]) : multiplier;
                    phase = 2;
                    result = (p == mySeatIndex()) ? 1 : (p == landlord ? 2 : (landlord == mySeatIndex() ? 2 : 1));
                    play(result == 1 ? GameSfx.WIN : GameSfx.FAIL);
                    if (result == 1) saveBest(Math.max(best(), multiplier * 100));
                    popResult();
                    invalidate();
                } catch (Exception ignored) {}
                break;
            }
            case "RESET":
                rebuildDeck();
                deal();
                invalidate();
                break;
            default:
                break;
        }
    }

    private int parseFirst(String s) {
        try { return Integer.parseInt(s.trim().split("[:|]")[0]); }
        catch (Exception e) { return -1; }
    }

    private float cw() { return Math.min(dp(48), (W - dp(34)) / 9f); }
    private float ch() { return cw() * 1.4f; }
    private float spacing() {
        int n = hands[mySeatIndex()].size();
        if (n <= 1) return cw() + dp(4);
        return Math.min(cw() + dp(3), (W - dp(24) - cw()) / (n - 1));
    }
    private float handX() { return W / 2f - (cw() + spacing() * (hands[mySeatIndex()].size() - 1)) / 2; }
    private float handY() { return H - ch() - dp(8); }

    @Override
    protected void drawGame(Canvas c) {
        header(c, "斗地主", (landlord < 0 ? "叫地主" : "地主：" + name(landlord)) + " · ×" + multiplier, Ui.BAD);
        int me = mySeatIndex();
        int o1 = (me + 1) % 3, o2 = (me + 2) % 3;
        drawOpponent(c, o1, opp[0]);
        drawOpponent(c, o2, opp[1]);
        drawBottom(c);
        drawLast(c);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(12));
        paint.setColor(Ui.MUTED);
        c.drawText(tip, W / 2f, H - ch() - dp(74), paint);

        if (phase == 1 && current == me) {
            String[] labels = {"提示", "不出", "出牌"};
            for (int i = 0; i < 3; i++) {
                paint.setColor(i == 2 ? Ui.PRIMARY : Ui.PANEL2);
                c.drawRoundRect(buttons[i], dp(8), dp(8), paint);
                paint.setColor(Ui.TEXT);
                paint.setTextSize(dp(14));
                paint.setFakeBoldText(i == 2);
                c.drawText(labels[i], buttons[i].centerX(), buttons[i].centerY() + dp(5), paint);
                paint.setFakeBoldText(false);
            }
        } else if (phase == 0 && current == me) {
            paint.setColor(Ui.PRIMARY);
            c.drawRoundRect(buttons[0], dp(8), dp(8), paint);
            paint.setColor(Ui.PANEL2);
            c.drawRoundRect(buttons[1], dp(8), dp(8), paint);
            paint.setColor(Ui.TEXT);
            paint.setTextSize(dp(14));
            c.drawText("叫地主", buttons[0].centerX(), buttons[0].centerY() + dp(5), paint);
            c.drawText("不叫", buttons[1].centerX(), buttons[1].centerY() + dp(5), paint);
        }

        float y = handY();
        for (int i = 0; i < hands[me].size(); i++) {
            Card card = hands[me].get(i);
            float x = handX() + i * spacing();
            drawCard(c, card, x, y - (sel[card.uid] ? dp(16) : 0), cw(), ch(), true, sel[card.uid]);
        }

        if (netOn()) {
            String[] all = net().names();
            StringBuilder sb = new StringBuilder("联机 ");
            for (int i = 0; i < 3; i++) {
                String nm = all[i];
                if (i > 0) sb.append(" · ");
                sb.append(i == me ? "我" : (nm == null || nm.isEmpty() ? "AI" : nm));
            }
            paint.setColor(Ui.MUTED);
            paint.setTextSize(dp(11));
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(sb.toString(), W / 2f, dp(72), paint);
        }

        if (phase == 2) {
            if (!resultShown && !isResultShowing()) popResult();
            paint.setColor(0xDD0A0E14);
            c.drawRect(0, dp(62), W, H, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(30));
            paint.setColor(result == 1 ? Ui.OK : Ui.BAD);
            c.drawText(result == 1 ? "你赢了！" : "你输了", W / 2f, H / 2f - dp(20), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(14));
            paint.setColor(Ui.TEXT);
            c.drawText("倍数 ×" + multiplier, W / 2f, H / 2f + dp(18), paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawOpponent(Canvas c, int p, RectF r) {
        paint.setColor(Ui.PANEL);
        c.drawRoundRect(r, dp(12), dp(12), paint);
        paint.setColor(landlord == p ? Ui.BAD : Ui.PANEL2);
        c.drawCircle(r.centerX(), r.top + dp(22), dp(16), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(12));
        paint.setColor(Ui.TEXT);
        c.drawText(name(p), r.centerX(), r.top + dp(26), paint);
        paint.setColor(Ui.MUTED);
        paint.setTextSize(dp(12));
        c.drawText(hands[p].size() + " 张", r.centerX(), r.top + dp(56), paint);
        paint.setColor(current == p && phase == 1 ? Ui.ACCENT : Ui.MUTED);
        paint.setTextSize(dp(11));
        c.drawText(action[p].isEmpty() ? "等待" : action[p], r.centerX(), r.top + dp(74), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawBottom(Canvas c) {
        float w = dp(30), h = dp(42);
        float x = W / 2f - w * 1.5f - dp(4);
        for (Card card : bottom) {
            drawCard(c, card, x, dp(160), w, h, landlord >= 0, false);
            x += w + dp(4);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(10));
        paint.setColor(Ui.MUTED);
        c.drawText("底牌", W / 2f, dp(155), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawLast(Canvas c) {
        if (lastCombo == null) return;
        if (lastPlayer == mySeatIndex() && phase == 1) return;
        List<Card> cs = lastCombo.cards;
        float w = dp(30), h = dp(42);
        float sp = Math.min(w + dp(2), (W - dp(50) - w) / Math.max(1, cs.size() - 1));
        float start = W / 2f - (w + sp * (cs.size() - 1)) / 2;
        float y = H / 2f - h / 2;
        for (int i = 0; i < cs.size(); i++) drawCard(c, cs.get(i), start + i * sp, y, w, h, true, false);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(12));
        paint.setColor(Ui.MUTED);
        c.drawText(name(lastPlayer) + " · " + lastCombo.name(), W / 2f, y - dp(8), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCard(Canvas c, Card card, float x, float y, float w, float h, boolean faceUp, boolean isSel) {
        cardRect.set(x, y, x + w, y + h);
        if (!faceUp) {
            paint.setColor(Ui.PRIMARY);
            c.drawRoundRect(cardRect, dp(4), dp(4), paint);
            paint.setColor(0x66FFFFFF);
            c.drawRoundRect(x + dp(4), y + dp(4), x + w - dp(4), y + h - dp(4), dp(3), dp(3), paint);
            paint.setColor(Ui.PRIMARY);
            c.drawRect(x + dp(8), y + dp(10), x + w - dp(8), y + dp(12), paint);
            return;
        }
        paint.setColor(isSel ? 0xFFF2A900 : 0xFFFFFFFF);
        c.drawRoundRect(cardRect, dp(4), dp(4), paint);
        paint.setColor(card.red() ? 0xFFDC2626 : 0xFF111827);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(true);
        paint.setTextSize(w * 0.32f);
        c.drawText(card.rankText(), x + dp(3), y + w * 0.36f, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(w * 0.26f);
        c.drawText(card.suitText(), x + dp(4), y + w * 0.62f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.42f);
        c.drawText(card.rank >= 16 ? "王" : card.suitText(), x + w / 2, y + h * 0.72f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void update(float dt, long now) {}

    @Override
    protected void onGameTouch(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_DOWN) return;
        float x = e.getX(), y = e.getY();
        int me = mySeatIndex();
        if (phase == 2) {
            if (netOn() && net().seat() == 0) net().send("RESET", "");
            rebuildDeck();
            deal();
            invalidate();
            return;
        }
        if (phase == 0 && current == me) {
            if (buttons[0].contains(x, y)) {
                landlord = me;
                hands[me].addAll(bottom);
                bottom.clear();
                sort(me);
                phase = 1;
                current = me;
                lastCombo = null;
                lastPlayer = -1;
                passCount = 0;
                action[0] = action[1] = action[2] = "";
                tip = name(me) + "先出牌";
                play(GameSfx.TAP);
                if (netOn()) net().send("LL", String.valueOf(me));
                invalidate();
            } else if (buttons[1].contains(x, y)) bidPass(me);
            invalidate();
            return;
        }
        if (phase != 1 || current != me) return;
        for (int i = 0; i < 3; i++) {
            if (!buttons[i].contains(x, y)) continue;
            if (i == 0) hint();
            else if (i == 1) playerPass();
            else playerPlay();
            return;
        }
        float top = handY() - dp(18);
        if (y < top || y > handY() + ch() + dp(4)) return;
        for (int i = hands[me].size() - 1; i >= 0; i--) {
            Card card = hands[me].get(i);
            float cx = handX() + i * spacing();
            if (x >= cx && x <= cx + cw()) {
                sel[card.uid] = !sel[card.uid];
                play(GameSfx.CLICK);
                invalidate();
                return;
            }
        }
    }
}
