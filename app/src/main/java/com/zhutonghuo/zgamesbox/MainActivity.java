package com.zhutonghuo.zgamesbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.zhutonghuo.zgamesbox.core.App;
import com.zhutonghuo.zgamesbox.core.BaseGameView;
import com.zhutonghuo.zgamesbox.core.GameDef;
import com.zhutonghuo.zgamesbox.core.NetController;
import com.zhutonghuo.zgamesbox.core.NetSession;
import com.zhutonghuo.zgamesbox.lobby.LobbyView;

public class MainActivity extends Activity {
    private FrameLayout root;
    private View current;
    private NetController net;
    private long lastBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.init(this);
        root = new FrameLayout(this);
        setContentView(root);
        net = new NetController(this);
        NetSession.get().setGameListener((from, action, payload) -> {
            if (current instanceof BaseGameView) ((BaseGameView) current).onNet(from, action, payload);
        });
        NetSession.get().setStatusListener(() -> {
            if (net != null) net.panel().refresh();
            if (current instanceof BaseGameView) ((BaseGameView) current).restart();
        });
        showLobby();
        root.addView(net.view());
    }

    private void dropCurrent() {
        if (current == null) return;
        if (current instanceof BaseGameView) {
            ((BaseGameView) current).onHostPause();
            ((BaseGameView) current).release();
        }
        root.removeView(current);
        current = null;
    }

    private void showLobby() {
        dropCurrent();
        NetSession.get().setActiveGame(null);
        net.close();
        LobbyView lobby = new LobbyView(this, this::open);
        current = lobby;
        root.addView(lobby, 0);
    }

    private void open(GameDef def) {
        dropCurrent();
        BaseGameView game = def.factory.create(this);
        game.setLobbyCallback(this::showLobby);
        current = game;
        root.addView(game, 0);
        NetSession.get().setActiveGame(def.id);
        game.onHostResume();
    }

    @Override
    public void onBackPressed() {
        if (net.onBack()) return;
        if (current instanceof BaseGameView && ((BaseGameView) current).handleBack()) return;
        if (!(current instanceof LobbyView)) {
            showLobby();
            return;
        }
        long t = System.currentTimeMillis();
        if (t - lastBack < 2000) {
            super.onBackPressed();
        } else {
            lastBack = t;
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        if (current instanceof BaseGameView) ((BaseGameView) current).onHostPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current instanceof BaseGameView) ((BaseGameView) current).onHostResume();
    }

    @Override
    protected void onDestroy() {
        if (current instanceof BaseGameView) ((BaseGameView) current).release();
        NetSession.get().leave();
        App.dispose();
        super.onDestroy();
    }
}
