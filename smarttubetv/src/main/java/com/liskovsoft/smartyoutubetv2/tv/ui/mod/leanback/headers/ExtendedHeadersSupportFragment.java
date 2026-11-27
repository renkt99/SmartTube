package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.KeyHelpers;

public class ExtendedHeadersSupportFragment extends HeadersSupportFragment {
    public interface OnHeaderLongPressedListener {
        /**
         * Called when a header item has been long pressed.
         *
         * @param viewHolder Row ViewHolder object corresponding to the selected Header.
         * @param row Row object corresponding to the selected Header.
         */
        void onHeaderLongPressed(RowHeaderPresenter.ViewHolder viewHolder, Row row);
    }

    private OnHeaderLongPressedListener mOnHeaderLongPressedListener;

    public ExtendedHeadersSupportFragment() {
        Helpers.setField(this, "mAdapterListener", mCustomAdapterListener);
    }

    @Override
    public void onViewCreated(android.view.View view, android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // uToob fork: sidebar spaced like Streamezy's (person top, middle centred, Settings bottom)
        if (com.liskovsoft.smartyoutubetv2.common.misc.UtoobSidebar.enabled(getContext()) && getVerticalGridView() != null) {
            androidx.leanback.widget.VerticalGridView grid = getVerticalGridView();
            float d = getResources().getDisplayMetrics().density;
            grid.setPadding((int) (18 * d), (int) (18 * d), grid.getPaddingRight(), (int) (14 * d));
            new com.liskovsoft.smartyoutubetv2.tv.ui.profiles.RailSpacing(grid);
            // a highlighted row grows (rightwards, see below): let it draw over the padding instead of being cut off
            grid.setClipToPadding(false);
            grid.setClipChildren(false);
            grid.post(() -> {
                // only the sidebar's own containers, up to (not including) the frame that also holds the pages
                for (android.view.ViewParent p = grid.getParent(); p instanceof android.view.ViewGroup; p = p.getParent()) {
                    if (p instanceof androidx.leanback.widget.BrowseFrameLayout) break;
                    ((android.view.ViewGroup) p).setClipChildren(false);
                    ((android.view.ViewGroup) p).setClipToPadding(false);
                }
            });
            grid.addOnChildAttachStateChangeListener(new androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View child) {
                    child.setPivotX(0); // the focus zoom scales this row wrapper: grow rightwards, not off the left edge
                    colorRow(grid, child);
                }

                @Override
                public void onChildViewDetachedFromWindow(View child) {
                }
            });
            grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> recolorRows());
        }
    }

    /** uToob fork: the page that is open now has its name in red (the row may be highlighted or not). */
    private static final int CURRENT_PAGE_COLOR = 0xFFFF4D5E;
    private int mCurrentRow = -1;

    public void setCurrentRow(int position) {
        mCurrentRow = position;
        recolorRows();
    }

    private void recolorRows() {
        androidx.leanback.widget.VerticalGridView grid = getVerticalGridView();
        if (grid == null || !com.liskovsoft.smartyoutubetv2.common.misc.UtoobSidebar.enabled(getContext())) return;
        for (int i = 0; i < grid.getChildCount(); i++) colorRow(grid, grid.getChildAt(i));
    }

    private void colorRow(androidx.leanback.widget.VerticalGridView grid, View row) {
        android.widget.TextView label = row.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.header_label);
        if (label == null) return;
        Object normal = label.getTag(com.liskovsoft.smartyoutubetv2.tv.R.id.header_label);
        if (normal == null) {
            normal = label.getCurrentTextColor();
            label.setTag(com.liskovsoft.smartyoutubetv2.tv.R.id.header_label, normal);
        }
        boolean current = mCurrentRow >= 0 && grid.getChildAdapterPosition(row) == mCurrentRow;
        label.setTextColor(current ? CURRENT_PAGE_COLOR : (Integer) normal);
    }

    @Override
    public void setAlignment(int windowAlignOffsetTop) {
        super.setAlignment(windowAlignOffsetTop);
        // uToob fork: rows only scroll when the list is longer than the screen, so the spacing above stays put
        if (com.liskovsoft.smartyoutubetv2.common.misc.UtoobSidebar.enabled(getContext()) && getVerticalGridView() != null) {
            getVerticalGridView().setWindowAlignment(androidx.leanback.widget.VerticalGridView.WINDOW_ALIGN_BOTH_EDGE);
        }
    }

    public void setOnHeaderLongPressedListener(OnHeaderLongPressedListener listener) {
        mOnHeaderLongPressedListener = listener;
    }

    private final ItemBridgeAdapter.AdapterListener mCustomAdapterListener =
            new ItemBridgeAdapter.AdapterListener() {
                @Override
                public void onCreate(final ItemBridgeAdapter.ViewHolder viewHolder) {
                    ItemBridgeAdapter.Wrapper wrapper = (ItemBridgeAdapter.Wrapper) Helpers.getField(ExtendedHeadersSupportFragment.this, "mWrapper");
                    OnLayoutChangeListener layoutChangeListener = (OnLayoutChangeListener) Helpers.getField(ExtendedHeadersSupportFragment.this, "sLayoutChangeListener");

                    View headerView = viewHolder.getViewHolder().view;
                    headerView.setOnClickListener(v -> {
                        OnHeaderClickedListener onHeaderClickedListener =
                                (OnHeaderClickedListener) Helpers.getField(ExtendedHeadersSupportFragment.this, "mOnHeaderClickedListener");
                        if (onHeaderClickedListener != null) {
                            onHeaderClickedListener.onHeaderClicked(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                        }
                    });

                    // NEW CODE
                    headerView.setOnLongClickListener(v -> {
                        if (mOnHeaderLongPressedListener != null) {
                            mOnHeaderLongPressedListener.onHeaderLongPressed(
                                    (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                    (Row) viewHolder.getItem());
                            return true; // don't provoke single click event
                        }

                        return false; // work as usual
                    });

                    // NEW CODE
                    headerView.setOnKeyListener((v, keyCode, event) -> {
                        if (mOnHeaderLongPressedListener != null) {
                            if (KeyHelpers.isMenuKey(keyCode)) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    mOnHeaderLongPressedListener.onHeaderLongPressed(
                                            (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                            (Row) viewHolder.getItem());
                                }
                            }
                        }

                        return false; // enable navigation events
                    });

                    if (wrapper != null) {
                        viewHolder.itemView.addOnLayoutChangeListener(layoutChangeListener);
                    } else {
                        headerView.addOnLayoutChangeListener(layoutChangeListener);
                    }
                }

            };
}
