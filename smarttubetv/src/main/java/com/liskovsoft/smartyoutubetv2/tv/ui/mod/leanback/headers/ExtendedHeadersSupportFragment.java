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
            new com.liskovsoft.smartyoutubetv2.tv.ui.profiles.RailSpacing(getVerticalGridView());
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
