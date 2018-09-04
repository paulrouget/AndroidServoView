package org.mozilla.geckoview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public final class SessionTextInput {

  public synchronized void setView(final @Nullable View view) {
  }

  public synchronized @Nullable InputConnection onCreateInputConnection(final @NonNull EditorInfo attrs) {
    return null;
  }

  public boolean onKeyPreIme(final int keyCode, final @NonNull KeyEvent event) {
    return true;
  }

  public boolean onKeyDown(final int keyCode, final @NonNull KeyEvent event) {
    return true;
  }

  public boolean onKeyUp(final int keyCode, final @NonNull KeyEvent event) {
    return true;
  }

  public boolean onKeyLongPress(final int keyCode, final @NonNull KeyEvent event) {
    return true;
  }

  public boolean onKeyMultiple(final int keyCode, final int repeatCount, final @NonNull KeyEvent event) {
    return true;
  }

  public void setShowSoftInputOnFocus(final boolean showSoftInputOnFocus) {
  }

  public void setDelegate(@Nullable final GeckoSession.TextInputDelegate delegate) {
  }
}

