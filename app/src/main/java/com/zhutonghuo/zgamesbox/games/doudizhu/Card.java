package com.zhutonghuo.zgamesbox.games.doudizhu;

public final class Card {
    public final int rank;
    public final int suit;
    public final int uid;

    public Card(int rank, int suit, int uid) {
        this.rank = rank;
        this.suit = suit;
        this.uid = uid;
    }

    public static String rankName(int rank) {
        if (rank == 11) return "J";
        if (rank == 12) return "Q";
        if (rank == 13) return "K";
        if (rank == 14) return "A";
        if (rank == 15) return "2";
        if (rank == 16) return "小王";
        if (rank == 17) return "大王";
        return String.valueOf(rank);
    }

    public boolean red() {
        return suit == 1 || suit == 3 || rank == 17;
    }

    public String rankText() {
        if (rank == 11) return "J";
        if (rank == 12) return "Q";
        if (rank == 13) return "K";
        if (rank == 14) return "A";
        if (rank == 15) return "2";
        if (rank == 16) return "小";
        if (rank == 17) return "大";
        return String.valueOf(rank);
    }

    public String suitText() {
        if (suit == 0) return "♠";
        if (suit == 1) return "♥";
        if (suit == 2) return "♣";
        if (suit == 3) return "♦";
        return "★";
    }

    public String label() {
        return rank >= 16 ? rankText() + "王" : rankText() + suitText();
    }
}
