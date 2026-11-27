package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Pair;

import androidx.leanback.widget.Presenter;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * uToob fork: video grid pages (Subscriptions, History, a channel's videos...) styled like Rocket Launcher's Home rows: three big
 * 16:9 cards across the screen, a one-line title and a short line under it ("Channel · 3 days ago", no view count).
 */
public class UtoobGridCardPresenter extends VideoCardPresenter {
    public static final int COLUMNS = 3;
    /** Gap between cards: a focused card grows 6% (as on Rocket's Home) and still has room around it. */
    public static final int GAP_DP = 32;

    /** The grid for these cards: smaller focus zoom than SmartTube's, wider gaps both ways. */
    public static androidx.leanback.widget.VerticalGridPresenter gridPresenter() {
        return new androidx.leanback.widget.VerticalGridPresenter(androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_XSMALL,
                com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil.FOCUS_DIMMER_ENABLED) {
            @Override
            protected androidx.leanback.widget.VerticalGridPresenter.ViewHolder createGridViewHolder(android.view.ViewGroup parent) {
                androidx.leanback.widget.VerticalGridPresenter.ViewHolder vh = super.createGridViewHolder(parent);
                int gap = (int) (GAP_DP * parent.getResources().getDisplayMetrics().density);
                vh.getGridView().setHorizontalSpacing(gap);
                vh.getGridView().setVerticalSpacing(gap);
                return vh;
            }
        };
    }

    /** Card size that makes exactly COLUMNS cards fill the grid's width (same sums as GridFragmentHelper). */
    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        int displayWidthPx = Math.max(dm.widthPixels, dm.heightPixels);
        float uiScale = MainUIData.instance(context).getUIScale();
        float spacing = GAP_DP * dm.density;
        // the closed sidebar's icon rail takes the left edge
        float available = displayWidthPx - displayWidthPx * 0.1f * uiScale
                - com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers.ExtendedHeadersSupportFragment.railWidthPx(context);
        float width = available / COLUMNS - spacing;
        float baseW = res.getDimensionPixelSize(R.dimen.card_width);
        float baseH = res.getDimensionPixelSize(R.dimen.card_height);
        return new Pair<>((int) width, (int) (baseH * width / baseW));
    }

    @Override
    protected boolean useRingFocus() {
        return true;
    }

    @Override
    protected boolean isCardMultilineTitleEnabled(Context context) {
        return false;
    }

    @Override
    protected boolean isCardMultilineSubtitleEnabled(Context context) {
        return false;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);
        ((ComplexImageCardView) viewHolder.view).setContentText(shortInfo(((Video) item).getSecondTitle()));
    }

    /** "Author • 12K views • 3 days ago" -> "Author · 3 days ago" (first and last part); shorter lines stay as they are. */
    static CharSequence shortInfo(CharSequence info) {
        if (info == null) return null;
        String clean = info.toString().replaceAll("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "");
        List<String> parts = new ArrayList<>();
        for (String p : clean.split("\u2022")) {
            String t = p.trim();
            if (!t.isEmpty()) parts.add(t);
        }
        if (parts.size() < 3) return parts.isEmpty() ? info : android.text.TextUtils.join(" \u00B7 ", parts);
        return parts.get(0) + " \u00B7 " + parts.get(parts.size() - 1);
    }
}
