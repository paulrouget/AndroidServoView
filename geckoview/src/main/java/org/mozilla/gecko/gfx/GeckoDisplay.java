package org.mozilla.gecko.gfx;

import org.mozilla.geckoview.GeckoSession;
import android.view.Surface;
import android.util.Log;

public class GeckoDisplay {
    public void surfaceChanged(Surface surface, int width, int height) {
      Log.w(GeckoSession.LOGTAG, "GeckoDisplay::surfaceChanged()");
      // HERE!!!
    }
    public void surfaceDestroyed() {
      Log.w(GeckoSession.LOGTAG, "GeckoDisplay::surfaceDestroyed()");
    }
}
