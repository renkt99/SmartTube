package com.liskovsoft.smartyoutubetv2.tv.ui.profiles;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.liskovsoft.smartyoutubetv2.common.misc.UtoobShare;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * uToob fork: read-only, for Rocket Launcher only (com.tomrenk.rocket). content://org.smarttube.utube.share gives one row,
 * column "json": {"current":"<person uToob has loaded>", "people":{"<id>":<UtoobShare snapshot>}}.
 *
 * <p>Runs in its own tiny ":share" process (MainApplication starts nothing there) and only reads the snapshot files, so Home
 * asking never starts uToob itself.
 */
public class UtoobShareProvider extends ContentProvider {
    private static final String TAG = "UtoobShare";
    private static final String ROCKET = "com.tomrenk.rocket";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!ROCKET.equals(getCallingPackage())) return null;
        JSONObject out = new JSONObject();
        try {
            JSONObject people = new JSONObject();
            File[] files = new File(getContext().getApplicationInfo().dataDir, "files/" + UtoobShare.DIR).listFiles();
            if (files != null) for (File f : files) {
                String n = f.getName();
                if (!f.isFile() || !n.endsWith(".json")) continue;
                try {
                    people.put(n.substring(0, n.length() - 5), new JSONObject(read(f)));
                } catch (Throwable e) {
                    Log.d(TAG, "skip " + n + ": " + e);
                }
            }
            out.put("current", Profiles.currentId(getContext())).put("people", people);
        } catch (Throwable e) {
            Log.e(TAG, "query: " + e);
        }
        MatrixCursor c = new MatrixCursor(new String[]{"json"});
        c.addRow(new Object[]{out.toString()});
        return c;
    }

    private static String read(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = 0;
            while (n < b.length) {
                int r = in.read(b, n, b.length - n);
                if (r < 0) break;
                n += r;
            }
            return new String(b, 0, n, StandardCharsets.UTF_8);
        }
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.utoob.share";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
