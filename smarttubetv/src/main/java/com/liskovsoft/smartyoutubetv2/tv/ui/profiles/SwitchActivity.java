package com.liskovsoft.smartyoutubetv2.tv.ui.profiles;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;

import java.io.File;

/**
 * Runs in its own short-lived process (":switch", see the manifest). Makes sure the main uToob process is gone, swaps
 * the preference files over to the chosen person ({@link Profiles#swap}) and starts uToob again. Same idea as
 * ProcessPhoenix: no SmartTube code is running while the files move, so nothing can write the old person's settings back.
 */
public class SwitchActivity extends Activity {
    static final String EXTRA_PID = "mainPid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pid = getIntent().getIntExtra(EXTRA_PID, -1);
        if (pid > 0 && pid != Process.myPid()) {
            Process.killProcess(pid);
            long until = SystemClock.uptimeMillis() + 3000;
            while (new File("/proc/" + pid).exists() && SystemClock.uptimeMillis() < until) SystemClock.sleep(20);
        }

        Profiles.swap(this);

        Intent start = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(getPackageName(), "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(start);
        finish();
        Runtime.getRuntime().exit(0);
    }
}
