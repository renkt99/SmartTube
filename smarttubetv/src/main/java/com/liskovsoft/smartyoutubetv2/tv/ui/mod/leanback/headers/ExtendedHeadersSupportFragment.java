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
            setRowsVisibleWhenDisabled(true);
        }
    }

    /**
     * uToob fork: closed sidebar = icon rail. The icon starts ~21.5dp in (18dp padding + the row's own inset) and is 32dp wide,
     * so 76dp leaves the same gap on both sides (measured on the TV 2026-11-27: icons at 38-96px in a 116px strip).
     */
    public static int railWidthPx(android.content.Context ctx) {
        return (int) (76 * ctx.getResources().getDisplayMetrics().density);
    }

    private boolean mCollapsed;

    /** Closed: only the icons show (labels hidden), in the rail BrowseSupportFragment narrows the sidebar to. */
    public void setCollapsed(boolean collapsed) {
        mCollapsed = collapsed;
        recolorRows();
    }

    /** uToob fork: the page that is open now has its name and icon in red (the row may be highlighted or not). */
    private static final int LOGO_RED = 0xFFE62117;
    private int mCurrentPageColor;

    /** The red of the theme's progress bars (which matches the logo); the logo's own red if it can't be read. */
    private int currentPageColor() {
        if (mCurrentPageColor != 0) return mCurrentPageColor;
        mCurrentPageColor = LOGO_RED;
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(com.liskovsoft.smartyoutubetv2.tv.R.attr.cardProgressStyle, tv, true)) {
                android.content.res.TypedArray a = getContext().obtainStyledAttributes(tv.resourceId, new int[]{android.R.attr.progressDrawable});
                android.graphics.drawable.Drawable d = a.getDrawable(0);
                a.recycle();
                if (d instanceof android.graphics.drawable.LayerDrawable && android.os.Build.VERSION.SDK_INT >= 24) {
                    android.graphics.drawable.Drawable p = ((android.graphics.drawable.LayerDrawable) d).findDrawableByLayerId(android.R.id.progress);
                    if (p instanceof android.graphics.drawable.ClipDrawable) p = ((android.graphics.drawable.ClipDrawable) p).getDrawable();
                    if (p instanceof android.graphics.drawable.GradientDrawable && ((android.graphics.drawable.GradientDrawable) p).getColor() != null) {
                        mCurrentPageColor = ((android.graphics.drawable.GradientDrawable) p).getColor().getDefaultColor();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return mCurrentPageColor;
    }
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
        label.setTextColor(current ? currentPageColor() : (Integer) normal);
        label.setVisibility(mCollapsed ? View.INVISIBLE : View.VISIBLE);
        android.widget.ImageView icon = row.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.header_icon);
        if (icon != null && grid.getChildAdapterPosition(row) != 0) {          // row 0 = the person's own picture
            if (current) icon.setColorFilter(currentPageColor(), android.graphics.PorterDuff.Mode.SRC_IN);
            else icon.clearColorFilter();
        }
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
