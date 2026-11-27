package com.liskovsoft.smartyoutubetv2.tv.ui.profiles;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * uToob people (fork addition). Each person has their own copy of everything SmartTube stores in its preference files
 * (settings, layout, sidebar, player options, history, YouTube sign-in and so subscriptions).
 *
 * <p>The people themselves come from Rocket Launcher (content://com.tomrenk.rocket.people), so they are the same people,
 * names and pictures as on Home. When Home just opened uToob, uToob follows the person picked there. Otherwise uToob keeps
 * whoever used it last, unless it has been away for 2 hours, then it asks "Who's watching?".
 *
 * <p>Switching moves the live preference files into files/utube_profiles/&lt;old id&gt;/ and the new person's into place,
 * while no uToob process is running (see {@link SwitchActivity}, which runs in its own process), then starts uToob again.
 * The registry lives in files/utube_profiles/state.json, which SmartTube's own Backup/Restore never touches.
 */
public final class Profiles {
    private static final String TAG = "UtubeProfiles";
    static final Uri ROCKET = Uri.parse("content://com.tomrenk.rocket.people");
    private static final long FROM_ROCKET_MS = 30_000L;
    private static final long ASK_AFTER_AWAY_MS = 2 * 3600_000L;
    private static final String DIR = "utube_profiles";
    /** SmartTube's preference folders under files/ (the same ones its Backup copies). */
    private static final String[] FILE_DIRS = {"app_prefs", "yt_service_prefs", "global_prefs"};

    private static int sStarted;
    private static boolean sNeedCheck;

    private Profiles() {
    }

    // ------------------------------------------------------------------ people from Rocket

    public static final class Person {
        public final String id, name, avatar;

        Person(String id, String name, String avatar) {
            this.id = id;
            this.name = name;
            this.avatar = avatar;
        }
    }

    static final class RocketInfo {
        final List<Person> people = new ArrayList<>();
        String current = "";
        String launchPkg = "";
        long launchAt;

        Person find(String id) {
            for (Person p : people) if (p.id.equals(id)) return p;
            return null;
        }
    }

    static RocketInfo readRocket(Context ctx) {
        try (Cursor c = ctx.getContentResolver().query(ROCKET, null, null, null, null)) {
            if (c == null || !c.moveToFirst()) return null;
            JSONObject o = new JSONObject(c.getString(0));
            RocketInfo info = new RocketInfo();
            JSONArray arr = o.optJSONArray("people");
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                String id = safe(p.optString("id"));
                if (!id.isEmpty()) info.people.add(new Person(id, p.optString("name"), p.optString("avatar")));
            }
            info.current = safe(o.optString("current"));
            info.launchPkg = o.optString("launchPkg");
            info.launchAt = o.optLong("launchAt");
            return info;
        } catch (Throwable e) {
            Log.d(TAG, "Rocket not available: " + e);
            return null;
        }
    }

    /** Rocket ids are base-36 timestamps; keep only safe characters so an id is always a plain folder name. */
    static String safe(String id) {
        return id == null ? "" : id.replaceAll("[^A-Za-z0-9_-]", "");
    }

    // ------------------------------------------------------------------ registry (state.json)

    static File root(Context ctx) {
        return new File(ctx.getApplicationInfo().dataDir, "files/" + DIR);
    }

    static JSONObject state(Context ctx) {
        File f = new File(root(ctx), "state.json");
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = 0;
            while (n < b.length) {
                int r = in.read(b, n, b.length - n);
                if (r < 0) break;
                n += r;
            }
            return new JSONObject(new String(b, 0, n, StandardCharsets.UTF_8));
        } catch (Throwable e) {
            return new JSONObject();
        }
    }

    static void saveState(Context ctx, JSONObject s) {
        File dir = root(ctx);
        dir.mkdirs();
        File tmp = new File(dir, "state.json.part");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(s.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (Throwable e) {
            Log.e(TAG, "saveState: " + e);
            return;
        }
        tmp.renameTo(new File(dir, "state.json"));
    }

    static void put(Context ctx, String key, Object value) {
        JSONObject s = state(ctx);
        try {
            s.put(key, value);
        } catch (Throwable ignored) {
        }
        saveState(ctx, s);
    }

    public static String currentId(Context ctx) {
        return state(ctx).optString("current");
    }

    /** True for our fork (people, Who's watching button); upstream flavors keep SmartTube's own behaviour. */
    public static boolean enabled(Context ctx) {
        return ctx.getPackageName().endsWith(".utube");
    }

    /** The person using uToob now (null when Rocket isn't there or nobody is set yet). */
    public static Person currentPerson(Context ctx) {
        RocketInfo info = readRocket(ctx);
        if (info == null) return null;
        Person p = info.find(currentId(ctx));
        return p != null ? p : info.find(info.current);
    }

    // ------------------------------------------------------------------ when uToob comes to the front

    /** Called from MainApplication (main process only). */
    public static void install(Application app) {
        if (!app.getPackageName().endsWith(".utube")) return;   // only our fork
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity a) {
                if (sStarted++ == 0) sNeedCheck = true;
            }

            @Override
            public void onActivityResumed(Activity a) {
                // SplashActivity is singleInstance (its own task): wait for a normal screen so the picker opens on top of it.
                if (!sNeedCheck || a instanceof WhoActivity || a.getClass().getSimpleName().startsWith("Splash")) return;
                sNeedCheck = false;
                onForeground(a);
            }

            @Override
            public void onActivityStopped(Activity a) {
                if (--sStarted <= 0) {
                    sStarted = 0;
                    put(a, "lastAway", System.currentTimeMillis());
                }
            }

            @Override
            public void onActivityCreated(Activity a, Bundle b) {
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
            }
        });
    }

    static void onForeground(Activity a) {
        RocketInfo info = readRocket(a);
        if (info == null || info.people.isEmpty()) return;     // no Rocket on this TV: one shared setup, as before

        JSONObject s = state(a);
        String cur = s.optString("current");
        long now = System.currentTimeMillis();

        if (cur.isEmpty()) {
            // First run with people: what uToob has now becomes Tom's (the user's choice), else Home's current person.
            Person owner = null;
            for (Person p : info.people) if (p.name.trim().equalsIgnoreCase("tom")) owner = p;
            if (owner == null) owner = info.find(info.current);
            if (owner == null) owner = info.people.get(0);
            cur = owner.id;
            put(a, "current", cur);
            put(a, "lastAway", now);
        }

        boolean fromRocket = a.getPackageName().equals(info.launchPkg) && now - info.launchAt < FROM_ROCKET_MS
                && info.launchAt != s.optLong("handledLaunch");
        if (fromRocket) {
            put(a, "handledLaunch", info.launchAt);
            if (!info.current.isEmpty() && !info.current.equals(cur) && info.find(info.current) != null) {
                switchTo(a, info.current);
            }
            return;
        }

        long away = s.optLong("lastAway");
        if (info.people.size() > 1 && away > 0 && now - away > ASK_AFTER_AWAY_MS) {
            a.startActivity(new Intent(a, WhoActivity.class));
        }
    }

    /** Remember who to load, then hand over to SwitchActivity (own process), which swaps the files and starts uToob again. */
    public static void switchTo(Activity a, String id) {
        put(a, "pending", safe(id));
        Intent i = new Intent(a, SwitchActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(SwitchActivity.EXTRA_PID, Process.myPid());
        a.startActivity(i);
        a.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    // ------------------------------------------------------------------ the swap (runs in the :switch process)

    /** Moves the live preference files to the current person's folder and the pending person's into place. */
    static void swap(Context ctx) {
        JSONObject s = state(ctx);
        String cur = safe(s.optString("current"));
        String next = safe(s.optString("pending"));
        if (next.isEmpty() || next.equals(cur)) {
            s.remove("pending");
            saveState(ctx, s);
            return;
        }
        File data = new File(ctx.getApplicationInfo().dataDir);
        File live = data;
        if (!cur.isEmpty()) moveSet(live, new File(root(ctx), cur), true);
        File incoming = new File(root(ctx), next);
        if (incoming.exists()) moveSet(incoming, live, false);   // missing = new person: SmartTube's defaults, signed out
        s.remove("pending");
        try {
            s.put("current", next);
            s.put("lastAway", System.currentTimeMillis());
        } catch (Throwable ignored) {
        }
        saveState(ctx, s);
    }

    /** One person's set: every shared_prefs/*.xml plus files/{app_prefs,yt_service_prefs,global_prefs}. */
    private static void moveSet(File from, File to, boolean clearTarget) {
        if (clearTarget) deleteTree(to);
        File fromPrefs = new File(from, "shared_prefs");
        File toPrefs = new File(to, "shared_prefs");
        toPrefs.mkdirs();
        File[] xml = fromPrefs.listFiles();
        if (xml != null) for (File f : xml) {
            if (f.isFile()) move(f, new File(toPrefs, f.getName()));
        }
        for (String d : FILE_DIRS) {
            File src = new File(from, "files/" + d);
            if (!src.exists()) continue;
            File dst = new File(to, "files/" + d);
            deleteTree(dst);
            dst.getParentFile().mkdirs();
            move(src, dst);
        }
        if (!clearTarget) deleteTree(from);                  // the person's folder is now live; don't keep a stale copy
    }

    private static void move(File src, File dst) {
        if (!src.renameTo(dst)) Log.e(TAG, "could not move " + src + " -> " + dst);
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }

    /** True in the short-lived :switch process, where MainApplication must not start SmartTube's own machinery. */
    public static boolean isSwitchProcess() {
        try (FileInputStream in = new FileInputStream("/proc/self/cmdline")) {
            byte[] b = new byte[256];
            int n = in.read(b);
            return n > 0 && new String(b, 0, n, StandardCharsets.UTF_8).trim().endsWith(":switch");
        } catch (Throwable e) {
            return false;
        }
    }
}
