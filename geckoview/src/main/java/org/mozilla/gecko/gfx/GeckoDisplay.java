package org.mozilla.gecko.gfx;

import android.view.Surface;
import android.util.Log;

import org.mozilla.geckoview.GeckoSession;

public class GeckoDisplay {
  private static final String LOGTAG = "java::SV::GeckoDisplay";
  private boolean initiated = false;
  private GeckoSession mSession;

  public GeckoDisplay(GeckoSession session) {
    mSession = session;
  }

  public void surfaceChanged(final Surface surface, final int width, final int height) {
    Log.d(LOGTAG, "surfaceChanged(" + width + "," + height + ")");
    if (initiated) {
      return;
    }
    initiated = true;
    mSession.onSurfaceReady(surface, width, height);
  }
  public void surfaceDestroyed() {
    Log.d(LOGTAG, "surfaceDestroyed()" + " THREAD: " + Thread.currentThread().getName());
  }
}