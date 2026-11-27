package com.liskovsoft.smartyoutubetv2.tv.ui.profiles;

import android.graphics.Rect;
import android.view.View;

import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * uToob fork: spaces the sidebar like Streamezy's when everything fits on screen. The person row stays at the top, Settings
 * (the last row) sits at the bottom, and the rows between them (Search first) are centred in the space left. When the list is
 * longer than the screen nothing changes: it just scrolls, Settings last.
 */
public class RailSpacing extends RecyclerView.ItemDecoration implements View.OnLayoutChangeListener {
    private final VerticalGridView mList;
    private int mExtra;

    public RailSpacing(VerticalGridView list) {
        mList = list;
        list.addItemDecoration(this);
        list.addOnLayoutChangeListener(this);
    }

    /** Free height when every row is laid out without our gaps (all header rows are the same height). */
    private int freeHeight() {
        RecyclerView.Adapter<?> a = mList.getAdapter();
        int n = a != null ? a.getItemCount() : 0;
        if (n < 3 || mList.getChildCount() == 0) return 0;
        int rowH = mList.getChildAt(0).getHeight();
        if (rowH <= 0) return 0;
        int content = n * rowH + (n - 1) * mList.getVerticalSpacing();
        int room = mList.getHeight() - mList.getPaddingTop() - mList.getPaddingBottom();
        return Math.max(0, room - content);
    }

    private boolean isLast(int pos) {
        RecyclerView.Adapter<?> a = mList.getAdapter();
        return a != null && pos > 1 && pos == a.getItemCount() - 1;
    }

    @Override
    public void getItemOffsets(Rect out, View view, RecyclerView parent, RecyclerView.State state) {
        if (mExtra <= 0) return;
        int pos = parent.getChildAdapterPosition(view);
        if (pos == 1) out.top = mExtra / 2;                    // Search: top of the centred middle group
        else if (isLast(pos)) out.top = mExtra - mExtra / 2;   // Settings: pushed to the bottom
    }

    @Override
    public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
        int extra = freeHeight();
        if (extra != mExtra) {
            mExtra = extra;
            mList.post(mList::invalidateItemDecorations);
        }
    }
}
