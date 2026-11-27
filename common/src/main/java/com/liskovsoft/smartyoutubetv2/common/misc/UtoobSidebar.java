package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;

/**
 * uToob fork: the sidebar laid out like Streamezy's. The person at the top, Search first of the middle items, Settings at the
 * bottom. The person and Search rows are buttons, not pages: the tv module's BrowseFragment opens Who's watching / Search for
 * them and doesn't switch the page when they are only highlighted.
 */
public final class UtoobSidebar {
    public static final int PERSON_ID = -9001;
    public static final int SEARCH_ID = -9002;

    /** Set by the tv module (Profiles) at start: the current person's name and a file:// address of their round picture. */
    public static volatile String personName;
    public static volatile String personIconUrl;

    private UtoobSidebar() {
    }

    public static boolean enabled(Context ctx) {
        return ctx != null && ctx.getPackageName().endsWith(".utube");
    }

    public static boolean isButton(long id) {
        return id == PERSON_ID || id == SEARCH_ID;
    }
}
