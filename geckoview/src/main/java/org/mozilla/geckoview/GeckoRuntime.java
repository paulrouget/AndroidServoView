package org.mozilla.geckoview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public final class GeckoRuntime {
  private static GeckoRuntime sDefaultRuntime;
  public static synchronized @NonNull GeckoRuntime getDefault(final @NonNull Context context) {
    Log.w(GeckoSession.LOGTAG, "GeckoRuntime::getDefault()");
    if (sDefaultRuntime == null) {
      sDefaultRuntime = new GeckoRuntime();
    }
    return sDefaultRuntime;
  }

  public void attachTo(final @NonNull Context context) {
    Log.w(GeckoSession.LOGTAG, "GeckoRuntime::attachTo()");
  }

  public static @NonNull GeckoRuntime create(final @NonNull Context context) {
    Log.w(GeckoSession.LOGTAG, "GeckoRuntime::create()");
    return new GeckoRuntime();
  }
}

