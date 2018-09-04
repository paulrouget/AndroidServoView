package org.mozilla.geckoview;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class GeckoRuntimeSettings {
  private static final String LOGTAG = "java::ServoView::GeckoRuntimeSettings";
  public static final class Builder {
    private final GeckoRuntimeSettings mSettings;

    public Builder() {
      mSettings = new GeckoRuntimeSettings();
    }

    public Builder(final GeckoRuntimeSettings settings) {
      mSettings = new GeckoRuntimeSettings(settings);
    }

    public @NonNull GeckoRuntimeSettings build() {
      return new GeckoRuntimeSettings(mSettings);
    }

    public @NonNull Builder useContentProcessHint(final boolean use) {
      mSettings.mUseContentProcess = use;
      return this;
    }

    public @NonNull Builder arguments(final @NonNull String[] args) {
      if (args == null) {
        throw new IllegalArgumentException("Arguments must not  be null");
      }
      mSettings.mArgs = args;
      return this;
    }

    public @NonNull Builder extras(final @NonNull Bundle extras) {
      if (extras == null) {
        throw new IllegalArgumentException("Extras must not  be null");
      }
      mSettings.mExtras = extras;
      return this;
    }

    public @NonNull Builder javaScriptEnabled(final boolean flag) {
      mSettings.mJavaScript.set(flag);
      return this;
    }

    public @NonNull Builder remoteDebuggingEnabled(final boolean enabled) {
      mSettings.mRemoteDebugging.set(enabled);
      return this;
    }

    public @NonNull Builder webFontsEnabled(final boolean flag) {
      mSettings.mWebFonts.set(flag);
      return this;
    }

    public @NonNull Builder consoleOutput(boolean enabled) {
      return this;
    }

    public @NonNull Builder nativeCrashReportingEnabled(final boolean enabled) {
      return this;
    }

    public @NonNull Builder javaCrashReportingEnabled(final boolean enabled) {
      return this;
    }

    public @NonNull Builder trackingProtectionCategories(int categories) {
      return this;
    }

    public @NonNull Builder displayDensityOverride(float density) {
      return this;
    }

    public @NonNull Builder screenSizeOverride(int width, int height) {
      return this;
    }

    public @NonNull Builder displayDpiOverride(int dpi) {
      return this;
    }
  }

  GeckoRuntime runtime;
  boolean mUseContentProcess;
  String[] mArgs;
  Bundle mExtras;
  int prefCount;

  private class Pref<T> {
    public final String name;
    public final T defaultValue;
    private T value;

    public Pref(final String name, final T defaultValue) {
      GeckoRuntimeSettings.this.prefCount++;

      this.name = name;
      this.defaultValue = defaultValue;
      value = defaultValue;
    }

    public void set(T newValue) {
      value = newValue;
      flush();
    }

    public T get() {
      return value;
    }

    public void flush() {
      if (GeckoRuntimeSettings.this.runtime != null) {
        GeckoRuntimeSettings.this.runtime.setPref(name, value);
      }
    }
  }

  /* package */ Pref<Boolean> mJavaScript = new Pref<Boolean>(
      "javascript.enabled", true);
  /* package */ Pref<Boolean> mRemoteDebugging = new Pref<Boolean>(
      "devtools.debugger.remote-enabled", false);
  /* package */ Pref<Boolean> mWebFonts = new Pref<Boolean>(
      "browser.display.use_document_fonts", true);

  private final Pref<?>[] mPrefs = new Pref<?>[] {
    mJavaScript, mRemoteDebugging, mWebFonts
  };

  /* package */ GeckoRuntimeSettings() {
    this(null);
  }

  /* package */ GeckoRuntimeSettings(final @Nullable GeckoRuntimeSettings settings) {
    if (BuildConfig.DEBUG && prefCount != mPrefs.length) {
      throw new AssertionError("Add new pref to prefs list");
    }

    if (settings == null) {
      mArgs = new String[0];
      mExtras = new Bundle();
      return;
    }

    mUseContentProcess = settings.getUseContentProcessHint();
    mArgs = settings.getArguments().clone();
    mExtras = new Bundle(settings.getExtras());

    for (int i = 0; i < mPrefs.length; i++) {
      // We know this is safe.
      @SuppressWarnings("unchecked")
      final Pref<Object> uncheckedPref = (Pref<Object>) mPrefs[i];
      uncheckedPref.set(settings.mPrefs[i].get());
    }
  }

  /* package */ void flush() {
    for (final Pref<?> pref: mPrefs) {
      pref.flush();
    }
  }

  /**
   * Get the content process hint flag.
   *
   * @return The content process hint flag.
   */
  public boolean getUseContentProcessHint() {
    return mUseContentProcess;
  }

  /**
   * Get the custom Gecko process arguments.
   *
   * @return The Gecko process arguments.
   */
  public String[] getArguments() {
    return mArgs;
  }

  /**
   * Get the custom Gecko intent extras.
   *
   * @return The Gecko intent extras.
   */
  public Bundle getExtras() {
    return mExtras;
  }

  /**
   * Get whether JavaScript support is enabled.
   *
   * @return Whether JavaScript support is enabled.
   */
  public boolean getJavaScriptEnabled() {
    return mJavaScript.get();
  }

  /**
   * Set whether JavaScript support should be enabled.
   *
   * @param flag A flag determining whether JavaScript should be enabled.
   * @return This GeckoRuntimeSettings instance.
   */
  public @NonNull GeckoRuntimeSettings setJavaScriptEnabled(final boolean flag) {
    mJavaScript.set(flag);
    return this;
  }

  /**
   * Get whether remote debugging support is enabled.
   *
   * @return True if remote debugging support is enabled.
   */
  public boolean getRemoteDebuggingEnabled() {
    return mRemoteDebugging.get();
  }

  /**
   * Set whether remote debugging support should be enabled.
   *
   * @param enabled True if remote debugging should be enabled.
   * @return This GeckoRuntimeSettings instance.
   */
  public @NonNull GeckoRuntimeSettings setRemoteDebuggingEnabled(final boolean enabled) {
    mRemoteDebugging.set(enabled);
    return this;
  }

  /**
   * Get whether web fonts support is enabled.
   *
   * @return Whether web fonts support is enabled.
   */
  public boolean getWebFontsEnabled() {
    return mWebFonts.get();
  }

  /**
   * Set whether support for web fonts should be enabled.
   *
   * @param flag A flag determining whether web fonts should be enabled.
   * @return This GeckoRuntimeSettings instance.
   */
  public @NonNull GeckoRuntimeSettings setWebFontsEnabled(final boolean flag) {
    mWebFonts.set(flag);
    return this;
  }

  public @NonNull GeckoRuntimeSettings setConsoleOutputEnabled(boolean enabled) {
    return this;
  }

}

