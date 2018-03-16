package org.mozilla.geckoview;

import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

public class GeckoView extends FrameLayout {

  private GLSurfaceView mView;

  public GeckoView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    setFocusable(true);
    setFocusableInTouchMode(true);
    setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
    setWillNotCacheDrawing(false);
    mView = new GLSurfaceView(getContext());
    mView.setEGLContextClientVersion(3);
    mView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);
    Renderer r = new Renderer();
    mView.setRenderer(r);
    mView.setRenderMode(RENDERMODE_WHEN_DIRTY);
    addView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
  }

  public void requestRender() {
    mView.requestRender();
  }

  public void queueEvent(Runnable r) {
    mView.queueEvent(r);
  }

  private GeckoSession mSession;
  public void setSession(GeckoSession session) {
    mSession = session;
    mSession.setView(this);
  }
  public GeckoSession getSession() {
    return mSession;
  }

  class Renderer implements GLSurfaceView.Renderer {
    public void onDrawFrame(GL10 gl) {
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      mSession.onGLReady();
    }
  }
}
