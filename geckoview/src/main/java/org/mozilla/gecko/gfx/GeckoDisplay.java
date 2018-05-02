package org.mozilla.gecko.gfx;

import android.view.Surface;
import android.util.Log;

public class GeckoDisplay {
  private static final String LOGTAG = "java::ServoView::GeckoDisplay";
  public void surfaceChanged(Surface surface, int width, int height) {
    Log.w(LOGTAG, "surfaceChanged()");
    // HERE!!!
  }
  public void surfaceDestroyed() {
    Log.w(LOGTAG, "surfaceDestroyed()");
  }
}
