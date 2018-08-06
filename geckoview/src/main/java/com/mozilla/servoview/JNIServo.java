package com.mozilla.servoview;

import android.util.Log;
import android.app.Activity;

public class JNIServo {
  private static final String LOGTAG = "java::ServoView::JNIServo";

  public JNIServo() {
    Log.d(LOGTAG, "JNIServo()" + " THREAD: " + Thread.currentThread().getName());
    System.loadLibrary("c++_shared");
    System.loadLibrary("simpleservo");
  }

  public native String version();
  public native void init(Activity activity, String args, String url, Callbacks callbacks, int width, int height, boolean log);
  public native void setBatchMode(boolean mode);
  public native void performUpdates();
  public native void resize(int width, int height);
  public native void reload();
  public native void stop();
  public native void goBack();
  public native void goForward();
  public native void loadUri(String uri);
  public native void scrollStart(int dx, int dy, int x, int y);
  public native void scroll(int dx, int dy, int x, int y);
  public native void scrollEnd(int dx, int dy, int x, int y);
  public native void click(int x, int y);
  public interface Callbacks {
      void wakeup();
      void flush();
      void makeCurrent();
      void onAnimatingChanged(boolean animating);
      void onLoadStarted();
      void onLoadEnded();
      void onTitleChanged(String title);
      void onUrlChanged(String url);
      void onHistoryChanged(boolean canGoBack, boolean canGoForward);
      byte[] readfile(String file);
  }
}
