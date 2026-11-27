package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.util.Log;

import com.liskovsoft.mediaserviceinterfaces.data.ItemGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;
import com.liskovsoft.youtubeapi.channelgroups.ChannelGroupServiceImpl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * uToob fork: a small snapshot of one person's subscribed channels and how far they got through each video, for Rocket
 * Launcher's "uToob subscriptions" row on the TV's Home. Saved to files/utube_profiles/share/&lt;person&gt;.json every time a
 * uToob screen pauses (so it is fresh by the time Home is back), and handed to Rocket by UtoobShareProvider.
 *
 * <p>{"person":id, "at":ms, "channels":[{"id","title"}], "watched":{"videoId":percent}}. Percent is 0-100; 90+ counts as
 * watched (Video.WATCHED_PERCENT), the same rule that greys cards out in uToob.
 */
public final class UtoobShare {
    private static final String TAG = "UtoobShare";
    public static final String DIR = "utube_profiles/share";

    private UtoobShare() {
    }

    public static void snapshot(Context context, String personId) {
        if (personId == null || personId.isEmpty() || !context.getPackageName().endsWith(".utube")) return;
        try {
            JSONArray channels = new JSONArray();
            List<String> seen = new ArrayList<>();
            ItemGroup group = ChannelGroupServiceImpl.INSTANCE.getSubscribedChannelGroup();
            List<ItemGroup.Item> items = group != null ? group.getItems() : null;
            if (items != null) for (ItemGroup.Item it : new ArrayList<>(items)) {
                String id = it != null ? it.getChannelId() : null;
                if (id == null || id.isEmpty() || seen.contains(id)) continue;
                seen.add(id);
                channels.put(new JSONObject().put("id", id).put("title", it.getTitle() != null ? it.getTitle() : ""));
            }

            JSONObject watched = new JSONObject();
            VideoStateService states = VideoStateService.instance(context);
            if (states != null) for (State s : new ArrayList<>(states.getStates())) {
                if (s == null || s.video == null || s.video.videoId == null) continue;
                float pct = s.durationMs > 0 ? s.positionMs * 100f / s.durationMs : s.video.percentWatched;
                if (pct < 0) continue;
                watched.put(s.video.videoId, Math.round(Math.min(100f, pct)));   // newest state last, so it wins
            }

            JSONObject out = new JSONObject().put("person", personId).put("at", System.currentTimeMillis())
                    .put("channels", channels).put("watched", watched);
            File dir = new File(context.getApplicationInfo().dataDir, "files/" + DIR);
            dir.mkdirs();
            File tmp = new File(dir, personId + ".json.part");
            try (FileOutputStream o = new FileOutputStream(tmp)) {
                o.write(out.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!tmp.renameTo(new File(dir, personId + ".json"))) Log.e(TAG, "rename failed");
        } catch (Throwable e) {
            Log.e(TAG, "snapshot: " + e);
        }
    }
}
