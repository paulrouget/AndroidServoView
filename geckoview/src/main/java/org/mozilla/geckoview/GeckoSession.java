package org.mozilla.geckoview;

import org.mozilla.gecko.gfx.GeckoDisplay;
import org.mozilla.gecko.gfx.PanZoomController;

import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class GeckoSession {
  public static final String LOGTAG = "ServoView";

  private static String mUrl;
  private static String mFutureUri = "about:blank";

  static class WakeupCallback implements LibServo.WakeupCallback {
    public void wakeup(){
      Log.w(LOGTAG, "java:wakeup");
      mView.queueEvent(new Runnable() {
        public void run() {
          mServo.performUpdates();
        }
      });
    };
  }

  class ServoCallbacks implements LibServo.ServoCallbacks {
    public void flush() {
      Log.w(LOGTAG, "java:flush");
      mView.requestRender();
    };
    public void onLoadStarted() {
      Log.w(LOGTAG, "java:onLoadStarted");
      mView.post(new Runnable() {
        public void run() {
          // FIXME: mFutureUri
          GeckoSession.this.getProgressDelegate().onPageStart(GeckoSession.this, mUrl != null ? mUrl : mFutureUri);
        }
      });
    };
    public void onLoadEnded() {
      Log.w(LOGTAG, "java:onLoadEnded");
      mView.post(new Runnable() {
        public void run() {
          // FIXME: no error support yet
          GeckoSession.this.getProgressDelegate().onPageStop(GeckoSession.this, true);
        }
      });
    };
    public void onTitleChanged(final String title) {
      Log.w(LOGTAG, "java:onTitleChanged: " + title);
      mView.post(new Runnable() {
        public void run() {
          GeckoSession.this.getContentDelegate().onTitleChange(GeckoSession.this, title);
        }
      });
    };
    public void onUrlChanged(final String url) {
      Log.w(LOGTAG, "java:onUrlChanged: " + url);
      mView.post(new Runnable() {
        public void run() {
          mUrl = url;
          GeckoSession.this.mNavigationDelegate.onLocationChange(GeckoSession.this, url);
        }
      });
    };
    public void onHistoryChanged(final boolean canGoBack, final boolean canGoForward) {
      Log.w(LOGTAG, "java:onHistoryChanged: " + canGoBack + ", " + canGoForward);
      mView.post(new Runnable() {
        public void run() {
          GeckoSession.this.mNavigationDelegate.onCanGoBack(GeckoSession.this, canGoBack);
          GeckoSession.this.mNavigationDelegate.onCanGoForward(GeckoSession.this, canGoForward);
        }
      });
    };
  }

  public GeckoSession() {
  }

  private GeckoSessionSettings mSettings = new GeckoSessionSettings();
  public GeckoSession(GeckoSessionSettings settings) {
    mSettings = settings;
  }

  public static void preload(final @NonNull Context context) {
    preload(context, null, null, false);
  }

  private static LibServo mServo;
  private static WakeupCallback mWakeupCallback;
  public static void preload(final @NonNull Context context, final @Nullable String[] geckoArgs, final @Nullable Bundle extras, final boolean multiprocess) {
  }

  public void close() {
  }

  private static GeckoView mView;
  public void setView(GeckoView view) {
    mView = view;
  }

  public void onGLReady() {
    Log.w(LOGTAG, "Loading libservo");
    mServo = new LibServo();
    Log.w(LOGTAG, mServo.version());
    final WakeupCallback c1 = new WakeupCallback();
    final ServoCallbacks c2 = new ServoCallbacks();
    mView.queueEvent(new Runnable() {
      public void run() {
        int width = mView.getWidth();
        int height = mView.getHeight();
        mServo.init(mFutureUri, "/sdcard/servo/resources/", c1, c2, width, height);
      }
    });
  }

  public void onViewResized(final int width, final int height) {
    Log.w(LOGTAG, "onViewResized");
    mView.queueEvent(new Runnable() {
      public void run() {
        mServo.resize(width, height);
      }
    });
  }

  private SessionTextInput mTextInput = new SessionTextInput();
  public @NonNull SessionTextInput getTextInput() {
    return mTextInput;
  }

  public GeckoSessionSettings getSettings() {
    return mSettings;
  }

  private GeckoDisplay mDisplay = new GeckoDisplay();
  public @NonNull GeckoDisplay acquireDisplay() {
    Log.w(LOGTAG, "GeckoSession::acquireDisplay");
    return mDisplay;
  }
  public void releaseDisplay(GeckoDisplay display) {
  }

  private PanZoomController mPanZoomController = new PanZoomController();
  public PanZoomController getPanZoomController() {
    return mPanZoomController;
  }


  /**
   *
   * DELEGATES
   *
   */

  /* WebResponseInfo */
  public class WebResponseInfo {
    public final String uri = "";
    public final String contentType = "";
    public final long contentLength = 0;
    public final String filename = "";
  }

  /* ContentDelegate */

  private ContentDelegate mContentDelegate;
  public void setContentDelegate(ContentDelegate delegate) {
    mContentDelegate = delegate;
  }
  public ContentDelegate getContentDelegate() {
    return mContentDelegate;
  }
  public interface ContentDelegate {
    @IntDef({ELEMENT_TYPE_NONE, ELEMENT_TYPE_IMAGE, ELEMENT_TYPE_VIDEO,
             ELEMENT_TYPE_AUDIO})
    public @interface ElementType {}
    static final int ELEMENT_TYPE_NONE = 0;
    static final int ELEMENT_TYPE_IMAGE = 1;
    static final int ELEMENT_TYPE_VIDEO = 2;
    static final int ELEMENT_TYPE_AUDIO = 3;

    void onTitleChange(GeckoSession session, String title);
    void onFocusRequest(GeckoSession session);
    void onCloseRequest(GeckoSession session);
    void onFullScreen(GeckoSession session, boolean fullScreen);
    void onContextMenu(GeckoSession session, int screenX, int screenY, String uri, @ElementType int elementType, String elementSrc);
    void onExternalResponse(GeckoSession session, WebResponseInfo response);
  }

  /* ProgressDelegate */

  private ProgressDelegate mProgressDelegate;
  public void setProgressDelegate(ProgressDelegate delegate) {
    mProgressDelegate = delegate;
  }
  public ProgressDelegate getProgressDelegate() {
    return mProgressDelegate;
  }
  public interface ProgressDelegate {
    public class SecurityInformation {
      public static final int SECURITY_MODE_UNKNOWN = 0;
      public static final int SECURITY_MODE_IDENTIFIED = 1;
      public static final int SECURITY_MODE_VERIFIED = 2;
      public static final int CONTENT_UNKNOWN = 0;
      public static final int CONTENT_BLOCKED = 1;
      public static final int CONTENT_LOADED = 2;
      public final boolean isSecure = false;
      public final boolean isException = false;
      public final String origin = null;
      public final String host = null;
      public final String organization = null;
      public final String subjectName = null;
      public final String issuerCommonName = null;
      public final String issuerOrganization = null;
      public final int securityMode = 0;
      public final int mixedModePassive = 0;
      public final int mixedModeActive = 0;
      public final int trackingMode = 0;
    }
    void onPageStart(GeckoSession session, String url);
    void onPageStop(GeckoSession session, boolean success);
    void onSecurityChange(GeckoSession session, SecurityInformation securityInfo);
  }

  /* TrackingProtectionDelegate */

  private TrackingProtectionDelegate mTrackingProtectionDelegate;
  public void setTrackingProtectionDelegate(TrackingProtectionDelegate delegate) {
    mTrackingProtectionDelegate = delegate;
  }
  public TrackingProtectionDelegate getTrackingProtectionDelegate() {
    return mTrackingProtectionDelegate;
  }
  public interface TrackingProtectionDelegate {
    static final int CATEGORY_AD = 1 << 0;
    static final int CATEGORY_ANALYTIC = 1 << 1;
    static final int CATEGORY_SOCIAL = 1 << 2;
    static final int CATEGORY_CONTENT = 1 << 3;
    void onTrackerBlocked(GeckoSession session, String uri, int categories);
  }

  /* NavigationDelegate */

  private NavigationDelegate mNavigationDelegate;
  public void setNavigationDelegate(NavigationDelegate delegate) {
    mNavigationDelegate = delegate;
  }
  public NavigationDelegate getNavigationDelegate() {
    return mNavigationDelegate;
  }
  public interface NavigationDelegate {
    @IntDef({TARGET_WINDOW_NONE, TARGET_WINDOW_CURRENT, TARGET_WINDOW_NEW})
    public @interface TargetWindow {}
    public static final int TARGET_WINDOW_NONE = 0;
    public static final int TARGET_WINDOW_CURRENT = 1;
    public static final int TARGET_WINDOW_NEW = 2;

    void onLocationChange(GeckoSession session, String url);
    void onCanGoBack(GeckoSession session, boolean canGoBack);
    void onCanGoForward(GeckoSession session, boolean canGoForward);
    // FIXME: which one is the right one? Focus and Crow don't use the same API
    // boolean onLoadRequest(GeckoSession session, String uri, @TargetWindow int target);
    void onLoadRequest(GeckoSession session, String uri, @TargetWindow int target, Response<Boolean> response);
    void onNewSession(GeckoSession session, String uri, Response<GeckoSession> response);
  }

  /* PromptDelegate */

  private PromptDelegate mPromptDelegate;
  public void setPromptDelegate(PromptDelegate delegate) {
    mPromptDelegate = delegate;
  }
  public PromptDelegate getPromptDelegate() {
    return mPromptDelegate;
  }
  public interface PromptDelegate {
    interface AlertCallback {
      void dismiss();
      boolean hasCheckbox();
      String getCheckboxMessage();
      boolean getCheckboxValue();
      void setCheckboxValue(boolean value);
    }
    void onAlert(GeckoSession session, String title, String msg, AlertCallback callback);
    interface ButtonCallback extends AlertCallback {
      void confirm(int button);
    }
    static final int BUTTON_TYPE_POSITIVE = 0;
    static final int BUTTON_TYPE_NEUTRAL = 1;
    static final int BUTTON_TYPE_NEGATIVE = 2;
    void onButtonPrompt(GeckoSession session, String title, String msg, String[] btnMsg, ButtonCallback callback);
    interface TextCallback extends AlertCallback {
      void confirm(String text);
    }
    void onTextPrompt(GeckoSession session, String title, String msg, String value, TextCallback callback);
    interface AuthCallback extends AlertCallback {
      void confirm(String password);
      void confirm(String username, String password);
    }
    class AuthOptions {
      public static final int AUTH_FLAG_HOST = 1;
      public static final int AUTH_FLAG_PROXY = 2;
      public static final int AUTH_FLAG_ONLY_PASSWORD = 8;
      public static final int AUTH_FLAG_PREVIOUS_FAILED = 16;
      public static final int AUTH_FLAG_CROSS_ORIGIN_SUB_RESOURCE = 32;
      public static final int AUTH_LEVEL_NONE = 0;
      public static final int AUTH_LEVEL_PW_ENCRYPTED = 1;
      public static final int AUTH_LEVEL_SECURE = 2;
      public int flags = 0;
      public String uri = null;
      public int level = 0;
      public String username = null;
      public String password = null;
    }
    void onAuthPrompt(GeckoSession session, String title, String msg, AuthOptions options, AuthCallback callback);
    class Choice {
      public static final int CHOICE_TYPE_MENU = 1;
      public static final int CHOICE_TYPE_SINGLE = 2;
      public static final int CHOICE_TYPE_MULTIPLE = 3;
      public final boolean disabled = false;
      public final String icon = null;
      public final String id = null;
      public final Choice[] items = null;
      public final String label = null;
      public final boolean selected = false;
      public final boolean separator = false;
    }
    interface ChoiceCallback extends AlertCallback {
      void confirm(String id);
      void confirm(String[] ids);
      void confirm(Choice item);
      void confirm(Choice[] items);
    }
    void onChoicePrompt(GeckoSession session, String title, String msg, int type, Choice[] choices, ChoiceCallback callback);
    void onColorPrompt(GeckoSession session, String title, String value, TextCallback callback);
    static final int DATETIME_TYPE_DATE = 1;
    static final int DATETIME_TYPE_MONTH = 2;
    static final int DATETIME_TYPE_WEEK = 3;
    static final int DATETIME_TYPE_TIME = 4;
    static final int DATETIME_TYPE_DATETIME_LOCAL = 5;
    void onDateTimePrompt(GeckoSession session, String title, int type, String value, String min, String max, TextCallback callback);
    interface FileCallback extends AlertCallback {
      void confirm(Context context, Uri uri);
      void confirm(Context context, Uri[] uris);
    }
    static final int FILE_TYPE_SINGLE = 1;
    static final int FILE_TYPE_MULTIPLE = 2;
    void onFilePrompt(GeckoSession session, String title, int type, String[] mimeTypes, FileCallback callback);
  }

  /* PermissionDelegate */

  private PermissionDelegate mPermissionDelegate;
  public void setPermissionDelegate(PermissionDelegate delegate) {
    mPermissionDelegate = delegate;
  }
  public PermissionDelegate getPermissionDelegate() {
    return mPermissionDelegate;
  }
  public interface PermissionDelegate {
    interface Callback {
      void grant();
      void reject();
    }
    interface MediaCallback {
      void grant(final String video, final String audio);
      void grant(final MediaSource video, final MediaSource audio);
      void reject();
    }
    void onMediaPermissionRequest(GeckoSession session, String uri, MediaSource[] video, MediaSource[] audio, MediaCallback callback);
    class MediaSource {
      public static final int SOURCE_CAMERA = 0;
      public static final int SOURCE_SCREEN  = 1;
      public static final int SOURCE_APPLICATION = 2;
      public static final int SOURCE_WINDOW = 3;
      public static final int SOURCE_BROWSER = 4;
      public static final int SOURCE_MICROPHONE = 5;
      public static final int SOURCE_AUDIOCAPTURE = 6;
      public static final int SOURCE_OTHER = 7;
      public static final int TYPE_VIDEO = 0;
      public static final int TYPE_AUDIO = 1;
      public final String id = null;
      public final String rawId = null;
      public final String name = null;
      public final int source = 7;
      public final int type = 0;
    }
  }

  /* ScrollDelegate */

  public interface ScrollDelegate {
    public void onScrollChanged(GeckoSession session, int scrollX, int scrollY);
  }
  private ScrollDelegate mScrollDelegate;
  public void setScrollDelegate(ScrollDelegate delegate) {
    mScrollDelegate = delegate;
  }
  public ScrollDelegate getScrollDelegate() {
    return mScrollDelegate;
  }

  /**
   *
   * METHODS
   *
   */

  public void loadData(@NonNull final byte[] bytes, @Nullable final String mimeType) {
    Log.w(LOGTAG, "GeckoSession::loadData()");
  }
  public void loadData(@NonNull final byte[] bytes, @Nullable final String mimeType, @Nullable final String baseUri) {
    Log.w(LOGTAG, "GeckoSession::loadData() with baseUri");
  }
  public void loadUri(Uri uri) {
    this.loadUri(uri.toString());
  }
  public void loadUri(final String uri) {
    Log.w(LOGTAG, "GeckoSession::loadUri()");
    if (mView == null) {
      Log.w(LOGTAG, "GeckoSession::loadUri() mView is null");
      return;
    }
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.loadUri(uri);
        } else {
          mFutureUri = uri;
        }
      }
    });
  }

  private boolean mIsOpen = false;
  public boolean isOpen() {
    Log.w(LOGTAG, "GeckoSession::isOpen()");
    return mIsOpen;
  }

  public void open(final @NonNull GeckoRuntime runtime) {
    Log.w(LOGTAG, "GeckoSession::open()");
    mIsOpen = true;
  }

  public void closeWindow() {
    Log.w(LOGTAG, "GeckoSession::closeWindow()");
  }
  public void reload() {
    Log.w(LOGTAG, "GeckoSession::reload()");
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.reload();
        }
      }
    });
  }
  public void stop() {
    Log.w(LOGTAG, "GeckoSession::stop()");
    // FIXME
  }
  public void goBack() {
    Log.w(LOGTAG, "GeckoSession::goBack()");
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.goBack();
        }
      }
    });
  }
  public void goForward() {
    Log.w(LOGTAG, "GeckoSession::goForward()");
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.goForward();
        }
      }
    });
  }
  public void click(final int x, final int y) {
    Log.w(LOGTAG, "GeckoSession::click");
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.click(x, y);
        }
      }
    });
  }
  public void scroll(final int deltaX, final int deltaY, final int x, final int y, final int phase) {
    Log.w(LOGTAG, "GeckoSession::scroll(" + deltaX + "," + deltaY + "," + phase + ")");
    mView.queueEvent(new Runnable() {
      public void run() {
        if (mServo != null)  {
          mServo.scroll(deltaX, deltaY, x, y, phase);
          mServo.performUpdates();
        }
      }
    });
  }
  public void setActive(boolean active) {
    Log.w(LOGTAG, "GeckoSession::setActive()");
    // FIXME
  }
  public void importScript(final String url) {
    Log.w(LOGTAG, "GeckoSession::importScript()");
    // FIXME
  }
  public void exitFullScreen() {
    Log.w(LOGTAG, "GeckoSession::exitFullScreen()");
    // FIXME
  }
  public void enableTrackingProtection(int categories) {
    Log.w(LOGTAG, "GeckoSession::enableTrackingProtection()");
    // FIXME
  }
  public void disableTrackingProtection() {
    Log.w(LOGTAG, "GeckoSession::disableTrackingProtection()");
    // FIXME
  }

  public interface Response<T> {
    void respond(T val);
  }
}
