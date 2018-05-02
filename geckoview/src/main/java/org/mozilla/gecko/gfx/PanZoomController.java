package org.mozilla.gecko.gfx;

import android.view.MotionEvent;
import android.util.Log;

public final class PanZoomController {
  private static final String LOGTAG = "java::ServoView::PanZoomController";
  public boolean onTouchEvent(final MotionEvent event) {
    Log.w(LOGTAG, "onTouchEvent");
    return true;
  }
  public boolean onMotionEvent(MotionEvent event) {
    Log.w(LOGTAG, "onMotionEvent");
    return true;
  }
}
