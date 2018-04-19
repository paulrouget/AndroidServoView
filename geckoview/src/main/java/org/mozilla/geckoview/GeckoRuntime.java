package org.mozilla.geckoview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public final class GeckoRuntime {
    private static GeckoRuntime sDefaultRuntime;
    public static synchronized @NonNull GeckoRuntime getDefault(final @NonNull Context context) {
        if (sDefaultRuntime == null) {
            sDefaultRuntime = new GeckoRuntime();
        }
        return sDefaultRuntime;
    }

    public void attachTo(final @NonNull Context context) {
    }

    public static @NonNull GeckoRuntime create(final @NonNull Context context) {
        return new GeckoRuntime();
    }
}

