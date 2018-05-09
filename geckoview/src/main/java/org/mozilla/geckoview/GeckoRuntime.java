package org.mozilla.geckoview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public final class GeckoRuntime {
  private static final String LOGTAG = "java::ServoView::GeckoRuntime";
  private static GeckoRuntime sDefaultRuntime;
  private Context mContext;

  private GeckoRuntime(final @NonNull Context context) {
    mContext = context;
  }

  public Context getContext() {
    return mContext;
  }

  public static synchronized @NonNull GeckoRuntime getDefault(final @NonNull Context context) {
    Log.d(LOGTAG, "getDefault()");
    if (sDefaultRuntime == null) {
      sDefaultRuntime = new GeckoRuntime(context);
    }
    return sDefaultRuntime;
  }

  void setPref(final String name, final Object value) {
    Log.d(LOGTAG, "setPref()");
  }

  public void attachTo(final @NonNull Context context) {
    Log.d(LOGTAG, "attachTo()");
    mContext = context;
  }

  public static @NonNull GeckoRuntime create(final @NonNull Context context) {
    Log.d(LOGTAG, "create()");
    return new GeckoRuntime(context);
  }

  private static GeckoRuntimeSettings mSettings;
  public GeckoRuntimeSettings getSettings() {
    Log.d(LOGTAG, "getSettings");
    return mSettings;
  }

  public static @NonNull GeckoRuntime create(final @NonNull Context context, final @NonNull GeckoRuntimeSettings settings) {
    Log.d(LOGTAG, "create()");
    mSettings = settings;
    return new GeckoRuntime(context);
  }


}

