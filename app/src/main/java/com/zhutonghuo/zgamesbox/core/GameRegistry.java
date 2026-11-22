package com.zhutonghuo.zgamesbox.core;

import java.util.ArrayList;
import java.util.List;

public final class GameRegistry {
    public static final List<GameDef> ALL = new ArrayList<>();

    static {
        ALL.add(new GameDef("snake", "贪吃蛇", "吃豆成长，别撞墙", 0xFF22C55E, c -> new com.zhutonghuo.zgamesbox.games.SnakeView(c)));
        ALL.add(new GameDef("2048", "2048", "滑动合并，凑出 2048", 0xFFF2A900, c -> new com.zhutonghuo.zgamesbox.games.Game2048View(c)));
        ALL.add(new GameDef("flappy", "Flappy Bird", "点按振翅，可联机比拼", 0xFF06B6D4, c -> new com.zhutonghuo.zgamesbox.games.FlappyView(c)));
        ALL.add(new GameDef("tetris", "俄罗斯方块", "旋转下落，消除整行", 0xFFA855F7, c -> new com.zhutonghuo.zgamesbox.games.TetrisView(c)));
        ALL.add(new GameDef("breakout", "打砖块", "挡板弹球，清光砖块", 0xFFF97316, c -> new com.zhutonghuo.zgamesbox.games.BreakoutView(c)));
        ALL.add(new GameDef("memory", "记忆翻牌", "翻牌配对，考验记忆", 0xFFEC4899, c -> new com.zhutonghuo.zgamesbox.games.MemoryView(c)));
        ALL.add(new GameDef("gomoku", "五子棋", "联机/人机，先连五胜", 0xFFEAB308, c -> new com.zhutonghuo.zgamesbox.games.GomokuView(c)));
        ALL.add(new GameDef("mines", "扫雷", "排雷插旗，首击保护", 0xFF94A3B8, c -> new com.zhutonghuo.zgamesbox.games.MinesweeperView(c)));
        ALL.add(new GameDef("tictactoe", "井字棋", "三连线，联机/AI 对战", 0xFF3B82F6, c -> new com.zhutonghuo.zgamesbox.games.TicTacToeView(c)));
        ALL.add(new GameDef("doudizhu", "斗地主", "叫地主出牌，联机/AI", 0xFFEF4444, c -> new com.zhutonghuo.zgamesbox.games.DouDizhuView(c)));
    }

    private GameRegistry() {}
}
