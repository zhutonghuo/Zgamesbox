package com.zhutonghuo.zgamesbox.games.doudizhu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Combo {
    public static final int SINGLE = 0;
    public static final int PAIR = 1;
    public static final int TRIPLE = 2;
    public static final int TRIPLE_ONE = 3;
    public static final int TRIPLE_PAIR = 4;
    public static final int STRAIGHT = 5;
    public static final int DOUBLE_STRAIGHT = 6;
    public static final int PLANE = 7;
    public static final int PLANE_ONE = 8;
    public static final int PLANE_PAIR = 9;
    public static final int FOUR_TWO_SINGLE = 10;
    public static final int FOUR_TWO_PAIR = 11;
    public static final int BOMB = 12;
    public static final int ROCKET = 13;

    public final int type;
    public final int main;
    public final int len;
    public final List<Card> cards;

    private Combo(int type, int main, int len, List<Card> cards) {
        this.type = type;
        this.main = main;
        this.len = len;
        this.cards = cards;
    }

    public static Combo recognize(List<Card> cs) {
        if (cs == null || cs.isEmpty()) return null;
        int n = cs.size();
        TreeMap<Integer, Integer> cnt = new TreeMap<>();
        for (Card c : cs) cnt.merge(c.rank, 1, Integer::sum);
        int min = cnt.firstKey();
        int max = cnt.lastKey();

        if (n == 2 && cnt.containsKey(16) && cnt.containsKey(17)) return new Combo(ROCKET, 17, 1, cs);
        if (n == 4 && cnt.size() == 1 && cnt.firstEntry().getValue() == 4) return new Combo(BOMB, max, 1, cs);

        if (n >= 5 && cnt.size() == n && max <= 14 && max - min + 1 == n) {
            return new Combo(STRAIGHT, max, n, cs);
        }
        if (n >= 6 && n % 2 == 0 && cnt.size() * 2 == n) {
            int pairs = cnt.size();
            if (pairs >= 3 && max <= 14 && max - min + 1 == pairs) {
                return new Combo(DOUBLE_STRAIGHT, max, pairs, cs);
            }
        }

        List<Integer> tris = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cnt.entrySet()) if (e.getValue() == 3) tris.add(e.getKey());
        int bestStart = -1, bestLen = 0;
        for (int t : tris) {
            int len = 1;
            while (tris.contains(t + len) && t + len <= 14) len++;
            if (len > bestLen) { bestLen = len; bestStart = t; }
        }
        if (bestLen >= 2) {
            int extra = n - bestLen * 3;
            if (extra == 0) return new Combo(PLANE, bestStart + bestLen - 1, bestLen, cs);
            if (extra == bestLen) return new Combo(PLANE_ONE, bestStart + bestLen - 1, bestLen, cs);
            if (extra == bestLen * 2 && cnt.size() == bestLen * 2) {
                boolean ok = true;
                for (Map.Entry<Integer, Integer> e : cnt.entrySet()) {
                    int k = e.getKey();
                    boolean wing = k < bestStart || k >= bestStart + bestLen;
                    if (wing && e.getValue() != 2) ok = false;
                    if (!wing && e.getValue() != 3) ok = false;
                }
                if (ok) return new Combo(PLANE_PAIR, bestStart + bestLen - 1, bestLen, cs);
            }
        }

        for (Map.Entry<Integer, Integer> e : cnt.entrySet()) {
            if (e.getValue() != 4) continue;
            int r = e.getKey();
            if (n == 6) return new Combo(FOUR_TWO_SINGLE, r, 1, cs);
            if (n == 8 && cnt.size() == 3) {
                boolean ok = true;
                for (Map.Entry<Integer, Integer> q : cnt.entrySet()) if (q.getKey() != r && q.getValue() != 2) ok = false;
                if (ok) return new Combo(FOUR_TWO_PAIR, r, 1, cs);
            }
        }

        if (n == 3 && cnt.size() == 1) return new Combo(TRIPLE, max, 1, cs);
        if (n == 4 && cnt.size() == 2) {
            for (Map.Entry<Integer, Integer> e : cnt.entrySet()) if (e.getValue() == 3) return new Combo(TRIPLE_ONE, e.getKey(), 1, cs);
        }
        if (n == 5 && cnt.size() == 2) {
            int t = -1, p = -1;
            for (Map.Entry<Integer, Integer> e : cnt.entrySet()) {
                if (e.getValue() == 3) t = e.getKey();
                else if (e.getValue() == 2) p = e.getKey();
            }
            if (t > 0 && p > 0) return new Combo(TRIPLE_PAIR, t, 1, cs);
        }
        if (n == 2 && cnt.size() == 1) return new Combo(PAIR, max, 1, cs);
        if (n == 1) return new Combo(SINGLE, max, 1, cs);
        return null;
    }

    public static boolean beats(Combo a, Combo b) {
        if (a == null) return false;
        if (b == null) return true;
        if (a.type == ROCKET) return true;
        if (b.type == ROCKET) return false;
        if (a.type == BOMB && b.type != BOMB) return true;
        if (b.type == BOMB && a.type != BOMB) return false;
        return a.type == b.type && a.len == b.len && a.main > b.main;
    }

    public String name() {
        switch (type) {
            case SINGLE: return "单 " + Card.rankName(main);
            case PAIR: return "对 " + Card.rankName(main);
            case TRIPLE: return "三 " + Card.rankName(main);
            case TRIPLE_ONE: return "三带一";
            case TRIPLE_PAIR: return "三带二";
            case STRAIGHT: return "顺子";
            case DOUBLE_STRAIGHT: return "连对";
            case PLANE: return "飞机";
            case PLANE_ONE: return "飞机带单";
            case PLANE_PAIR: return "飞机带对";
            case FOUR_TWO_SINGLE: return "四带二";
            case FOUR_TWO_PAIR: return "四带两对";
            case BOMB: return "炸弹";
            case ROCKET: return "王炸";
            default: return "出牌";
        }
    }
}
