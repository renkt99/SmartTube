package com.liskovsoft.smartyoutubetv2.tv.ui.profiles;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/** "Who's watching?": the people from Rocket Launcher, same pictures. OK picks, Back keeps whoever is using uTube now. */
public class WhoActivity extends Activity {
    /** Same ids, emoji and colours as Streamezy and Rocket (Rocket Ui.kt Avatars). */
    private static final Map<String, Object[]> AVATARS = new HashMap<>();

    static {
        a("rocket", "🚀", 0xFF5F3DC4, 0xFF1971C2); a("popcorn", "🍿", 0xFFFFCE00, 0xFFE8590C);
        a("clapper", "🎬", 0xFF495057, 0xFF212529); a("film", "🎞️", 0xFF7048E8, 0xFF364FC7);
        a("ticket", "🎟️", 0xFFF03E3E, 0xFFC2255C); a("star", "⭐", 0xFFFAB005, 0xFFF76707);
        a("camera", "🎥", 0xFF1C7ED6, 0xFF0B7285); a("alien", "👽", 0xFF2F9E44, 0xFF087F5B);
        a("robot", "🤖", 0xFF868E96, 0xFF495057); a("ghost", "👻", 0xFFCED4DA, 0xFF845EF7);
        a("cat", "🐱", 0xFFF59F00, 0xFFE8590C); a("dog", "🐶", 0xFFA9713B, 0xFF7C4A1D);
        a("fox", "🦊", 0xFFFD7E14, 0xFFD9480F); a("panda", "🐼", 0xFFADB5BD, 0xFF343A40);
        a("lion", "🦁", 0xFFFCC419, 0xFFE67700); a("octopus", "🐙", 0xFFE64980, 0xFF862E9C);
        a("dino", "🦖", 0xFF37B24D, 0xFF1B5E20); a("dragon", "🐉", 0xFFE03131, 0xFF6741D9);
        a("unicorn", "🦄", 0xFFF783AC, 0xFF9775FA); a("owl", "🦉", 0xFF8D6E63, 0xFF4E342E);
        a("wizard", "🧙", 0xFF7950F2, 0xFF3B5BDB); a("ninja", "🥷", 0xFF343A40, 0xFFC92A2A);
        a("pirate", "🏴‍☠️", 0xFF212529, 0xFF1971C2); a("planet", "🪐", 0xFF22B8CF, 0xFF5F3DC4);
    }

    private static void a(String id, String emoji, long c1, long c2) {
        AVATARS.put(id, new Object[]{emoji, (int) c1, (int) c2});
    }

    private static final int RED = 0xFFE62117;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Profiles.RocketInfo info = Profiles.readRocket(this);
        if (info == null || info.people.isEmpty()) {
            finish();
            return;
        }
        String cur = Profiles.currentId(this);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(0xFF0F0F0F);

        TextView title = text("Who's watching?", 34, Color.WHITE, true);
        page.addView(title, new LinearLayout.LayoutParams(-2, -2));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(24), dp(36), dp(24), dp(24));
        scroll.addView(row);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        page.addView(scroll, sp);

        View focus = null;
        for (final Profiles.Person p : info.people) {
            View card = card(p, p.id.equals(cur));
            card.setOnClickListener(v -> pick(p.id, cur));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(170), -2);
            lp.setMargins(dp(14), 0, dp(14), 0);
            row.addView(card, lp);
            if (p.id.equals(cur) || focus == null) focus = card;
        }

        TextView hint = text("Each person has their own settings, sign-in and subscriptions", 15, 0xFF9A9A9A, false);
        page.addView(hint, new LinearLayout.LayoutParams(-2, -2));

        setContentView(page);
        if (focus != null) focus.requestFocus();
    }

    private void pick(String id, String cur) {
        if (id.equals(cur)) {
            Profiles.put(this, "lastAway", System.currentTimeMillis());
            finish();
        } else {
            Profiles.switchTo(this, id);
        }
    }

    private View card(Profiles.Person p, boolean isCurrent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));
        card.setFocusable(true);
        card.setClickable(true);

        FrameLayout ring = new FrameLayout(this);
        ring.setPadding(dp(5), dp(5), dp(5), dp(5));
        ring.addView(avatar(p, 120), new FrameLayout.LayoutParams(dp(120), dp(120)));
        card.addView(ring, new LinearLayout.LayoutParams(dp(130), dp(130)));

        TextView name = text(p.name, 20, Color.WHITE, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
        np.topMargin = dp(12);
        card.addView(name, np);
        TextView sub = text(isCurrent ? "Using uTube now" : " ", 13, 0xFF9A9A9A, false);
        card.addView(sub, new LinearLayout.LayoutParams(-2, -2));

        card.setOnFocusChangeListener((v, has) -> {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(Color.TRANSPARENT);
            if (has) g.setStroke(dp(4), RED);
            ring.setBackground(g);
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(120).start();
            name.setTextColor(has ? Color.WHITE : 0xFFCCCCCC);
        });
        return card;
    }

    private TextView avatar(Profiles.Person p, int sizeDp) {
        Object[] a = AVATARS.get(p.avatar);
        TextView v = new TextView(this);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        v.setTextColor(Color.WHITE);
        v.setText(a != null ? (String) a[0] : initials(p.name));
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(sizeDp * (a != null ? 0.5f : 0.38f)));
        if (a == null) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{a != null ? (int) a[1] : 0xFF5F3DC4, a != null ? (int) a[2] : 0xFF1971C2});
        bg.setShape(GradientDrawable.OVAL);
        v.setBackground(bg);
        return v;
    }

    private static String initials(String name) {
        StringBuilder b = new StringBuilder();
        for (String w : name.trim().split("\\s+")) {
            if (!w.isEmpty() && b.length() < 2) b.append(Character.toUpperCase(w.charAt(0)));
        }
        return b.length() == 0 ? "?" : b.toString();
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setSingleLine(true);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }
}
