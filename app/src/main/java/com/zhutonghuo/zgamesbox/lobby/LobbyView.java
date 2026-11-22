package com.zhutonghuo.zgamesbox.lobby;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zhutonghuo.zgamesbox.core.App;
import com.zhutonghuo.zgamesbox.core.GameDef;
import com.zhutonghuo.zgamesbox.core.GameRegistry;
import com.zhutonghuo.zgamesbox.core.Ui;

public class LobbyView extends LinearLayout {
    public interface Callback {
        void open(GameDef def);
    }

    public LobbyView(Context c, Callback cb) {
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);

        TextView title = new TextView(c);
        title.setText("Zgamesbox");
        title.setTextSize(28);
        title.getPaint().setFakeBoldText(true);
        title.setTextColor(Ui.TEXT);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Ui.dpi(c, 26), 0, Ui.dpi(c, 4));
        addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(c);
        sub.setText("10 款小游戏 · 全部原生绘制");
        sub.setTextSize(13);
        sub.setTextColor(Ui.MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, Ui.dpi(c, 18));
        addView(sub, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        GridView grid = new GridView(c);
        grid.setNumColumns(2);
        grid.setColumnWidth(Ui.dpi(c, 150));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(Ui.dpi(c, 10));
        grid.setVerticalSpacing(Ui.dpi(c, 10));
        grid.setPadding(Ui.dpi(c, 14), 0, Ui.dpi(c, 14), Ui.dpi(c, 20));
        grid.setClipToPadding(false);
        grid.setAdapter(new CardAdapter(c));
        grid.setOnItemClickListener((AdapterView<?> p, View v, int pos, long id) -> cb.open(GameRegistry.ALL.get(pos)));
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
    }

    private static class CardAdapter extends BaseAdapter {
        private final Context c;

        CardAdapter(Context c) { this.c = c; }

        @Override public int getCount() { return GameRegistry.ALL.size(); }
        @Override public Object getItem(int pos) { return GameRegistry.ALL.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Card v = (Card) convert;
            if (v == null) v = new Card(c);
            v.bind(GameRegistry.ALL.get(pos));
            return v;
        }
    }

    private static class Card extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private GameDef def;

        Card(Context c) { super(c); }

        void bind(GameDef d) {
            def = d;
            invalidate();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = android.view.View.resolveSize(Ui.dpi(getContext(), 155), wSpec);
            setMeasuredDimension(w, Ui.dpi(getContext(), 122));
        }

        @Override
        protected void onDraw(Canvas c) {
            if (def == null) return;
            float m = Ui.dp(getContext(), 2);
            r.set(m, m, getWidth() - m, getHeight() - m);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Ui.PANEL);
            c.drawRoundRect(r, Ui.dp(getContext(), 14), Ui.dp(getContext(), 14), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(getContext(), 1));
            p.setColor(def.accent);
            c.drawRoundRect(r, Ui.dp(getContext(), 14), Ui.dp(getContext(), 14), p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(def.accent);
            c.drawRoundRect(Ui.dp(getContext(), 14), Ui.dp(getContext(), 16),
                    Ui.dp(getContext(), 18), Ui.dp(getContext(), 44),
                    Ui.dp(getContext(), 4), Ui.dp(getContext(), 4), p);

            p.setTextAlign(Paint.Align.LEFT);
            p.setFakeBoldText(true);
            p.setTextSize(Ui.dp(getContext(), 15));
            p.setColor(Ui.TEXT);
            c.drawText(def.name, Ui.dp(getContext(), 28), Ui.dp(getContext(), 40), p);

            p.setFakeBoldText(false);
            p.setTextSize(Ui.dp(getContext(), 11));
            p.setColor(Ui.MUTED);
            c.drawText(def.desc, Ui.dp(getContext(), 15), Ui.dp(getContext(), 66), p);

            p.setTextSize(Ui.dp(getContext(), 11));
            if ("doudizhu".equals(def.id)) {
                int w = App.prefs().wins(def.id);
                p.setColor(w > 0 ? Ui.GOLD : Ui.MUTED);
                c.drawText("已赢 " + w + " 局", Ui.dp(getContext(), 15), Ui.dp(getContext(), 98), p);
            } else {
                int best = App.prefs().best(def.id);
                p.setColor(best > 0 ? Ui.GOLD : Ui.MUTED);
                c.drawText("最高 " + best, Ui.dp(getContext(), 15), Ui.dp(getContext(), 98), p);
            }
        }
    }
}
