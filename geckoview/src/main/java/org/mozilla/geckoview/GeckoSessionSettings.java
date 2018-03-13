package org.mozilla.geckoview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public final class GeckoSessionSettings {
    public static final int DISPLAY_MODE_BROWSER = 0;
    public static final int DISPLAY_MODE_MINIMAL_UI = 1;
    public static final int DISPLAY_MODE_STANDALONE = 2;
    public static final int DISPLAY_MODE_FULLSCREEN = 3;

    public static class Key<T> {
        final String name;
        final boolean initOnly;
        final Collection<T> values;
        Key(final String name) {
            this(name, /* initOnly */ false, /* values */ null);
        }
        Key(final String name, final boolean initOnly, final Collection<T> values) {
            this.name = name;
            this.initOnly = initOnly;
            this.values = values;
        }
    }

    public static final Key<String> CHROME_URI = new Key<String>("chromeUri", /* initOnly */ true, /* values */ null);
    public static final Key<Integer> SCREEN_ID = new Key<Integer>("screenId", /* initOnly */ true, /* values */ null);
    public static final Key<Boolean> USE_TRACKING_PROTECTION = new Key<Boolean>("useTrackingProtection");
    public static final Key<Boolean> USE_PRIVATE_MODE = new Key<Boolean>("usePrivateMode", /* initOnly */ true, /* values */ null);
    public static final Key<Boolean> USE_MULTIPROCESS = new Key<Boolean>("useMultiprocess", /* initOnly */ true, /* values */ null);
    public static final Key<Integer> DISPLAY_MODE = new Key<Integer>("displayMode", /* initOnly */ false, Arrays.asList(DISPLAY_MODE_BROWSER, DISPLAY_MODE_MINIMAL_UI, DISPLAY_MODE_STANDALONE, DISPLAY_MODE_FULLSCREEN));
    public static final Key<Boolean> USE_REMOTE_DEBUGGER = new Key<Boolean>("useRemoteDebugger"); 

    public GeckoSessionSettings() {
    }
    public GeckoSessionSettings(final @Nullable GeckoSessionSettings settings,
                                       final @Nullable GeckoSession session) {
    }


    public void setBoolean(final Key<Boolean> key, final boolean value) {
    }

    public boolean getBoolean(final Key<Boolean> key) {
	    return false;
    }

    public void setInt(final Key<Integer> key, final int value) {
    }

    public int getInt(final Key<Integer> key) {
	    return 1;
    }

    public void setString(final Key<String> key, final String value) {
    }

    public String getString(final Key<String> key) {
	    return "";
    }
}
