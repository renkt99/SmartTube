package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.service.SidebarService;
import com.liskovsoft.youtubeapi.channelgroups.ChannelGroupServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * uToob fork: turns channels pinned to the sidebar into (local, no Google account) subscriptions, once per person, so the
 * Subscriptions page shows their newest videos like regular YouTube. Pins are left as they are.
 *
 * <p>The "done" flag is in shared_prefs/utoob_flags.xml, which belongs to the person (people swap all shared_prefs), so every
 * person gets their own one-time import.
 */
public final class UtoobSubscriptions {
    private static final String TAG = "UtoobSubscriptions";
    private static final String FLAGS = "utoob_flags";
    private static final String KEY_IMPORTED = "pinnedChannelsSubscribed";

    private UtoobSubscriptions() {
    }

    public static void importPinnedOnce(Context context) {
        Context ctx = context.getApplicationContext();
        if (!ctx.getPackageName().endsWith(".utube")) return;
        SharedPreferences flags = ctx.getSharedPreferences(FLAGS, Context.MODE_PRIVATE);
        if (flags.getBoolean(KEY_IMPORTED, false)) return;

        SidebarService sidebar = SidebarService.instance(ctx);
        List<Video> channels = new ArrayList<>();
        for (Video v : sidebar.getPinnedItems()) {
            if (v != null && v.channelId != null && v.playlistId == null) channels.add(v);
        }

        if (!sidebar.isSectionPinned(MediaGroup.TYPE_SUBSCRIPTIONS)) {
            sidebar.enableSection(MediaGroup.TYPE_SUBSCRIPTIONS, true);
            try {
                BrowsePresenter.instance(ctx).updateSections();
            } catch (Throwable e) {
                Log.d(TAG, "updateSections: " + e);
            }
        }

        // Off the main thread: subscribe() looks up a channel's name and picture online when it doesn't have them.
        new Thread(() -> {
            int n = 0;
            for (Video v : channels) {
                try {
                    boolean isChannelCard = v.isChannel();
                    String title = isChannelCard ? v.title : null;          // a pinned video card's title is the video's
                    String icon = isChannelCard ? v.cardImageUrl : null;
                    if (!ChannelGroupServiceImpl.INSTANCE.isSubscribed(v.channelId)) {
                        ChannelGroupServiceImpl.subscribe(true, v.channelId, title, icon);
                        n++;
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "subscribe " + v.channelId + ": " + e);
                }
            }
            flags.edit().putBoolean(KEY_IMPORTED, true).commit();
            Log.d(TAG, "subscribed to " + n + " pinned channels");
            int added = n;
            if (added > 0) {
                new Handler(Looper.getMainLooper()).post(() ->
                        MessageHelpers.showMessage(ctx, "Subscribed to " + added + " channels from your sidebar"));
            }
        }, "utoob-subs").start();
    }
}
