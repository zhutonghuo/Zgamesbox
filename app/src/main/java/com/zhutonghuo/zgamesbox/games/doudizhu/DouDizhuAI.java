package com.zhutonghuo.zgamesbox.games.doudizhu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class DouDizhuAI {
    private static final Random RND = new Random();

    private DouDizhuAI() {}

    public static int strength(List<Card> hand) {
        int s = 0;
        TreeMap<Integer, Integer> cnt = new TreeMap<>();
        for (Card c : hand) cnt.merge(c.rank, 1, Integer::sum);
        for (Map.Entry<Integer, Integer> e : cnt.entrySet()) {
            if (e.getKey() >= 16) s += 6;
            else if (e.getKey() == 15) s += 3;
            if (e.getValue() == 4) s += 8;
            if (e.getValue() == 3) s += 2;
        }
        if (cnt.containsKey(16) && cnt.containsKey(17)) s += 8;
        return s;
    }

    public static List<List<Card>> candidates(List<Card> hand) {
        TreeMap<Integer, List<Card>> by = new TreeMap<>();
        for (Card c : hand) by.computeIfAbsent(c.rank, k -> new ArrayList<>()).add(c);
        List<List<Card>> out = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>(by.keySet());

        for (int r : ranks) {
            List<Card> cs = by.get(r);
            out.add(Collections.singletonList(cs.get(0)));
            if (cs.size() >= 2) out.add(new ArrayList<>(cs.subList(0, 2)));
            if (cs.size() >= 3) out.add(new ArrayList<>(cs.subList(0, 3)));
            if (cs.size() == 4) out.add(new ArrayList<>(cs));
        }
        if (by.containsKey(16) && by.containsKey(17)) {
            List<Card> rocket = new ArrayList<>();
            rocket.add(by.get(16).get(0));
            rocket.add(by.get(17).get(0));
            out.add(rocket);
        }

        List<Integer> singles = new ArrayList<>();
        for (int r : ranks) if (r <= 14) singles.add(r);
        for (int start : singles) {
            List<Card> run = new ArrayList<>();
            for (int r = start; r <= 14; r++) {
                if (!by.containsKey(r)) break;
                run.add(by.get(r).get(0));
                if (run.size() >= 5) out.add(new ArrayList<>(run));
            }
        }
        List<Integer> pairs = new ArrayList<>();
        for (int r : ranks) if (r <= 14 && by.get(r).size() >= 2) pairs.add(r);
        for (int start : pairs) {
            List<Card> run = new ArrayList<>();
            for (int r = start; r <= 14; r++) {
                if (!by.containsKey(r) || by.get(r).size() < 2) break;
                run.add(by.get(r).get(0));
                run.add(by.get(r).get(1));
                if (run.size() >= 6) out.add(new ArrayList<>(run));
            }
        }

        List<Integer> tris = new ArrayList<>();
        for (int r : ranks) if (by.get(r).size() >= 3) tris.add(r);
        for (int t : tris) {
            for (int s : ranks) {
                if (s == t || by.get(s).isEmpty()) continue;
                List<Card> c = new ArrayList<>(by.get(t).subList(0, 3));
                c.add(by.get(s).get(0));
                out.add(c);
                if (by.get(s).size() >= 2) {
                    c = new ArrayList<>(by.get(t).subList(0, 3));
                    c.add(by.get(s).get(0));
                    c.add(by.get(s).get(1));
                    out.add(c);
                }
            }
        }
        for (int t : ranks) {
            if (by.get(t).size() != 4) continue;
            List<Card> wings = new ArrayList<>();
            for (int s : ranks) if (s != t) wings.add(by.get(s).get(0));
            if (wings.size() >= 2) {
                List<Card> c = new ArrayList<>(by.get(t));
                c.add(wings.get(0));
                c.add(wings.get(1));
                out.add(c);
            }
        }
        for (int start : tris) {
            List<Integer> run = new ArrayList<>();
            for (int r = start; r <= 14; r++) {
                if (!by.containsKey(r) || by.get(r).size() < 3) break;
                run.add(r);
                if (run.size() < 2) continue;
                List<Card> base = new ArrayList<>();
                for (int q : run) base.addAll(by.get(q).subList(0, 3));
                out.add(new ArrayList<>(base));
                List<Card> extra = new ArrayList<>();
                for (int s : ranks) if (!run.contains(s)) extra.add(by.get(s).get(0));
                if (extra.size() >= run.size()) {
                    List<Card> c = new ArrayList<>(base);
                    for (int i = 0; i < run.size(); i++) c.add(extra.get(i));
                    out.add(c);
                }
                List<Card> extraPairs = new ArrayList<>();
                for (int s : ranks) if (!run.contains(s) && by.get(s).size() >= 2) {
                    extraPairs.add(by.get(s).get(0));
                    extraPairs.add(by.get(s).get(1));
                }
                if (extraPairs.size() >= run.size() * 2) {
                    List<Card> c = new ArrayList<>(base);
                    for (int i = 0; i < run.size() * 2; i++) c.add(extraPairs.get(i));
                    out.add(c);
                }
            }
        }
        return out;
    }

    public static List<Card> choose(List<Card> hand, Combo required, boolean lead, int foeCount) {
        List<List<Card>> cand = candidates(hand);
        List<Card> best = null;
        float bestCost = Float.MAX_VALUE;
        for (List<Card> cs : cand) {
            Combo cmb = Combo.recognize(cs);
            if (cmb == null) continue;
            if (!lead && !Combo.beats(cmb, required)) continue;
            float cost = lead ? leadCost(cmb, cs.size(), hand.size()) : followCost(cmb, cs.size());
            if (!lead && (cmb.type == Combo.BOMB || cmb.type == Combo.ROCKET)
                    && (required.type != Combo.BOMB && required.type != Combo.ROCKET)
                    && foeCount > 2 && RND.nextInt(4) != 0) cost += 500;
            if (cost < bestCost) {
                bestCost = cost;
                best = cs;
            }
        }
        if (best != null && best.size() == hand.size()) return best;
        return best;
    }

    private static float leadCost(Combo cmb, int size, int handSize) {
        float c = -size * 4f + cmb.main * 0.25f;
        if (cmb.type == Combo.BOMB || cmb.type == Combo.ROCKET) c += 600;
        if (cmb.type == Combo.SINGLE) c += 15;
        if (cmb.type == Combo.PAIR) c += 12;
        if (cmb.type == Combo.TRIPLE) c += 10;
        return c;
    }

    private static float followCost(Combo cmb, int size) {
        float c = cmb.main * 3f + cmb.type * 0.2f + size * 0.15f;
        if (cmb.type == Combo.BOMB || cmb.type == Combo.ROCKET) c += 500;
        return c;
    }
}
