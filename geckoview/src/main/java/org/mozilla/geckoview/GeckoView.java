package org.mozilla.geckoview;

import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.content.Context;
import android.widget.TextView;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class GeckoView  extends FrameLayout {
  public GeckoView(final Context context) {
    super(context);
  }

  private SurfaceView mView;
  public GeckoView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    setFocusable(true);
    setFocusableInTouchMode(true);
    setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
    setWillNotCacheDrawing(false);

    mView = new SurfaceView(getContext());
    mView.setBackgroundColor(Color.RED);
    addView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
  }


  private GeckoSession mSession;

  public void setSession(GeckoSession session) {
    mSession = session;
  }

  public GeckoSession getSession() {
    return mSession;
  }
}
