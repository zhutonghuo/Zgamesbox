package com.zhutonghuo.zgamesbox.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.zhutonghuo.zgamesbox.NetPanel;

public class NetController {
    private final Activity act;
    private final NetPanel panel;

    public NetController(Activity act) {
        this.act = act;
        panel = new NetPanel(act, this);
    }

    public View view() { return panel; }
    public NetPanel panel() { return panel; }

    public void open() { panel.expand(); }
    public void close() { panel.collapse(); }
    public boolean isOpen() { return panel.isExpanded(); }

    public boolean onBack() {
        if (!panel.isExpanded()) return false;
        panel.collapse();
        return true;
    }

    public void askHost() {
        prompt("房间名（可留空）", "", false, name -> {
            int port = NetSession.DEFAULT_PORT;
            boolean ok = NetSession.get().startHost(name, port);
            if (!ok) toast("端口 " + port + " 被占用，开房失败");
            else toast("开房成功，等待对手加入");
            panel.refresh();
        });
    }

    public void askJoin() {
        prompt("房主 IP", NetSession.get().clientIp().isEmpty() ? "192.168.1." : NetSession.get().clientIp(), false, ip -> {
            prompt("端口", String.valueOf(NetSession.DEFAULT_PORT), true, portStr -> {
                int port;
                try { port = Integer.parseInt(portStr.trim()); }
                catch (Exception e) { toast("端口无效"); return; }
                toast("正在连接…");
                new Thread(() -> {
                    boolean alive = NetSession.get().ping(ip.trim(), port);
                    act.runOnUiThread(() -> {
                        if (!alive) toast("连不上 " + ip + ":" + port + "，检查 IP/端口与防火墙");
                        else askRoom(ip.trim(), port);
                    });
                }).start();
            });
        });
    }

    private void askRoom(String ip, int port) {
        prompt("4 位房间号", "", true, room -> {
            if (room.trim().length() != 4) { toast("房间号是 4 位数字"); return; }
            new Thread(() -> {
                boolean ok = NetSession.get().join(ip, port, room.trim(), "");
                act.runOnUiThread(() -> {
                    if (!ok) toast("房间号验证失败或连接断开");
                    else { toast("已加入房间 " + room); panel.refresh(); }
                });
            }).start();
        });
    }

    void prompt(String title, String def, boolean numeric, final InputCb cb) {
        final EditText et = new EditText(act);
        et.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        et.setText(def);
        et.setSelection(et.getText().length());
        new AlertDialog.Builder(act)
                .setTitle(title)
                .setView(et)
                .setPositiveButton("确定", (d, w) -> cb.onInput(et.getText().toString()))
                .setNegativeButton("取消", null)
                .show();
    }

    public void confirm(String title, String msg, Runnable yes) {
        new AlertDialog.Builder(act).setTitle(title).setMessage(msg)
                .setPositiveButton("确定", (d, w) -> yes.run())
                .setNegativeButton("取消", null).show();
    }

    void toast(String s) { Toast.makeText(act, s, Toast.LENGTH_SHORT).show(); }

    public interface InputCb { void onInput(String s); }
}
