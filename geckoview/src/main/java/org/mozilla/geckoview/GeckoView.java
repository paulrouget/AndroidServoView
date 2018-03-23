package org.mozilla.geckoview;

import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.util.Log;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.GestureDetector;
import android.widget.OverScroller;
import android.view.Choreographer;

import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

public class GeckoView extends FrameLayout implements GestureDetector.OnGestureListener, Choreographer.FrameCallback {

  private GLSurfaceView mView;
  private long mLastDownTime;

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
    mGestureDetector = new GestureDetector(this);
    mScroller = new OverScroller(context);
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
      if (mSession != null) {
        mSession.onViewResized(width, height);
      }
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      mSession.onGLReady();
    }
  }

  private GestureDetector mGestureDetector;
  private OverScroller mScroller;
  private int mLastY = 0;
  private int mCurY = 0;
  private int mPageWidth = 80000;
  private int mPageHeight = 80000;
  private boolean mFlinging = false;

  public void doFrame(long frameTimeNanos) {
    Log.w(GeckoSession.LOGTAG, "doFrame");

    if (mScroller.isFinished() && mFlinging) {
      Log.w(GeckoSession.LOGTAG, "FLING OVER");
      mFlinging = false;
      mSession.scroll(0, 0, 0, 0, 2);
      return;
    }

    if (mFlinging) {
      mScroller.computeScrollOffset();
      Log.w(GeckoSession.LOGTAG, "COMPUTE:" + mScroller.getCurrY());
      mCurY = mScroller.getCurrY();
    }

    int delta = mCurY - mLastY;

    mLastY = mCurY;

    Log.w(GeckoSession.LOGTAG, "DELTA:" + delta);

    if (delta != 0) {
      mSession.scroll(0, delta, 0, 0, 1);
    }

    Choreographer.getInstance().postFrameCallback(this);
  }

  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    Log.w(GeckoSession.LOGTAG, "ONFLING ");
    // FIXME: boundaries
    // https://github.com/servo/servo/issues/20361
    mFlinging = true;
    // FIXME: magic value
    mCurY = 40000;
    mLastY = mCurY;
    mScroller.fling(0, mCurY, (int)velocityX, (int)velocityY, 0, mPageWidth, 0, mPageHeight);
    return true;
  }

  public boolean onDown(MotionEvent e) {
    mScroller.forceFinished(true);
    return true;
  }

  public boolean onTouchEvent(final MotionEvent e) {
    mGestureDetector.onTouchEvent(e);

    int action = e.getActionMasked();
    switch(action) {
      case (MotionEvent.ACTION_DOWN):
        Log.w(GeckoSession.LOGTAG, "SCROLL START");
        mCurY = (int)e.getY();
        mLastY = mCurY;
        mScroller.forceFinished(true);
        mSession.scroll(0, 0, 0, 0, 0);
        Choreographer.getInstance().postFrameCallback(this);
        return true;
      case (MotionEvent.ACTION_MOVE):
        mCurY = (int)e.getY();
        return true;
      case (MotionEvent.ACTION_UP):
      case (MotionEvent.ACTION_CANCEL):
        Log.w(GeckoSession.LOGTAG, "SCROLL END");
        if (!mFlinging) {
          mSession.scroll(0, 0, 0, 0, 2);
          Choreographer.getInstance().removeFrameCallback(this);
        }
        return true;
      default:
        return true;
    }
  }

  public boolean onSingleTapUp(MotionEvent e) {
    mSession.click((int)e.getX(), (int)e.getY());
    return false;
  }

  public void onLongPress(MotionEvent e) { } 
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return true; }
  public void onShowPress(MotionEvent e) { }
}
