package org.mozilla.geckoview;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.mozilla.gecko.gfx.GeckoDisplay;
import org.mozilla.gecko.gfx.PanZoomController;

import android.app.Activity;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;
import android.util.Log;
import android.view.Surface;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import com.mozilla.servoview.ServoSurface;
import com.mozilla.servoview.Servo;

public class GeckoSession {
  private static final String LOGTAG = "java::SV::GeckoSession";
  private ServoSurface mServo;
  private Activity mActivity;
  private String mFutureUri;
  private boolean mServoReady = false;

  private int mWidth;
  private int mHeight;

  public void onSurfaceReady(Surface surface, int width, int height) {
      mWidth = width;
      mHeight = height;
      mServo = new ServoSurface(surface, width, height);
      mServo.setClient(new ServoCallbacks());
      mServo.setActivity(mActivity);
      mServo.runLoop();
  }

  class ServoCallbacks implements Servo.Client {
    private String mUrl = "about:blank";

    public void onLoadStarted() {
      Log.d(LOGTAG, "ServoCallback::onLoadStarted()");
      mServoReady = true;
      GeckoSession.this.getProgressDelegate().onPageStart(GeckoSession.this, mUrl);
      if (mFutureUri != null) {
        mServo.loadUri(mFutureUri);
      }
    };
    public void onLoadEnded() {
      Log.d(LOGTAG, "ServoCallback::onLoadEnded()");
      GeckoSession.this.getProgressDelegate().onPageStop(GeckoSession.this, true);
    };
    public void onTitleChanged(final String title) {
      Log.d(LOGTAG, "ServoCallback::onTitleChanged(" + title + ")");
      GeckoSession.this.getContentDelegate().onTitleChange(GeckoSession.this, title);
    };
    public void onUrlChanged(final String url) {
      Log.d(LOGTAG, "ServoCallback::onUrlChanged(" + url + ")");
      mUrl = url;
      GeckoSession.this.mNavigationDelegate.onLocationChange(GeckoSession.this, url);
    };
    public void onHistoryChanged(final boolean canGoBack, final boolean canGoForward) {
      Log.d(LOGTAG, "ServoCallback::onHistoryChanged()");
      GeckoSession.this.mNavigationDelegate.onCanGoBack(GeckoSession.this, canGoBack);
      GeckoSession.this.mNavigationDelegate.onCanGoForward(GeckoSession.this, canGoForward);
    };
    public void onRedrawing(boolean redrawing) {
      Log.d(LOGTAG, "ServoCallback::onRedrawing: " + redrawing);
    };
  }

  public GeckoSession() {
    Log.d(LOGTAG, "GeckoSession()");
  }

  private GeckoSessionSettings mSettings = new GeckoSessionSettings();
  public GeckoSession(GeckoSessionSettings settings) {
    Log.d(LOGTAG, "GeckoSession(settings)");
    mSettings = settings;
  }

  public void close() {
    Log.d(LOGTAG, "close()");
  }

  private SessionTextInput mTextInput = new SessionTextInput();
  public @NonNull SessionTextInput getTextInput() {
    Log.d(LOGTAG, "getTextInput()");
    return mTextInput;
  }

  public GeckoSessionSettings getSettings() {
    Log.d(LOGTAG, "getSettings()");
    return mSettings;
  }

  private GeckoDisplay mDisplay;
  public @NonNull GeckoDisplay acquireDisplay() {
    if (mDisplay == null) {
       mDisplay = new GeckoDisplay(this);
    }
    Log.d(LOGTAG, "acquireDisplay()");
    return mDisplay;
  }
  public void releaseDisplay(GeckoDisplay display) {
    Log.d(LOGTAG, "releaseDisplay()");
  }

  private PanZoomController mPanZoomController;
  public PanZoomController getPanZoomController() {
    if (mPanZoomController == null) {
      mPanZoomController = new PanZoomController(this);
    }
    // Log.d(LOGTAG, "getPanZoomController()");
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
    Log.d(LOGTAG, "setContentDelegate()");
    mContentDelegate = delegate;
  }
  public ContentDelegate getContentDelegate() {
    Log.d(LOGTAG, "getContentDelegate()");
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
    void onCrash(GeckoSession session);
  }

  /* ProgressDelegate */

  private ProgressDelegate mProgressDelegate;
  public void setProgressDelegate(ProgressDelegate delegate) {
    Log.d(LOGTAG, "setProgressDelegate()");
    mProgressDelegate = delegate;
  }
  public ProgressDelegate getProgressDelegate() {
    Log.d(LOGTAG, "getProgressDelegate()");
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
    void onProgressChange(GeckoSession session, int progress);
  }

  /* TrackingProtectionDelegate */

  private TrackingProtectionDelegate mTrackingProtectionDelegate;
  public void setTrackingProtectionDelegate(TrackingProtectionDelegate delegate) {
    Log.d(LOGTAG, "setTrackingProtectionDelegate()");
    mTrackingProtectionDelegate = delegate;
  }
  public TrackingProtectionDelegate getTrackingProtectionDelegate() {
    Log.d(LOGTAG, "getTrackingProtectionDelegate()");
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
    Log.d(LOGTAG, "setNavigationDelegate()");
    mNavigationDelegate = delegate;
  }
  public NavigationDelegate getNavigationDelegate() {
    Log.d(LOGTAG, "getNavigationDelegate()");
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
    GeckoResult<Boolean> onLoadRequest(GeckoSession session, String uri, @TargetWindow int target, int flags);
    GeckoResult<GeckoSession> onNewSession(GeckoSession session, String uri);


    @IntDef(flag = true,
    value = {LOAD_REQUEST_IS_USER_TRIGGERED})
      /* package */ @interface LoadRequestFlags {}

    public static final int LOAD_REQUEST_IS_USER_TRIGGERED = 0x1000;

    @IntDef({ERROR_CATEGORY_UNKNOWN, ERROR_CATEGORY_SECURITY,
    ERROR_CATEGORY_NETWORK, ERROR_CATEGORY_CONTENT,
    ERROR_CATEGORY_URI, ERROR_CATEGORY_PROXY,
    ERROR_CATEGORY_SAFEBROWSING})
      public @interface LoadErrorCategory {}

    @IntDef({ERROR_UNKNOWN, ERROR_SECURITY_SSL, ERROR_SECURITY_BAD_CERT,
    ERROR_NET_RESET, ERROR_NET_INTERRUPT, ERROR_NET_TIMEOUT,
    ERROR_CONNECTION_REFUSED, ERROR_UNKNOWN_PROTOCOL,
    ERROR_UNKNOWN_HOST, ERROR_UNKNOWN_SOCKET_TYPE,
    ERROR_UNKNOWN_PROXY_HOST, ERROR_MALFORMED_URI,
    ERROR_REDIRECT_LOOP, ERROR_SAFEBROWSING_PHISHING_URI,
    ERROR_SAFEBROWSING_MALWARE_URI, ERROR_SAFEBROWSING_UNWANTED_URI,
    ERROR_SAFEBROWSING_HARMFUL_URI, ERROR_CONTENT_CRASHED,
    ERROR_OFFLINE, ERROR_PORT_BLOCKED,
    ERROR_PROXY_CONNECTION_REFUSED, ERROR_FILE_NOT_FOUND,
    ERROR_FILE_ACCESS_DENIED, ERROR_INVALID_CONTENT_ENCODING,
    ERROR_UNSAFE_CONTENT_TYPE, ERROR_CORRUPTED_CONTENT})
      public @interface LoadError {}

    public static final int ERROR_CATEGORY_UNKNOWN = 0x1;
    public static final int ERROR_CATEGORY_SECURITY = 0x2;
    public static final int ERROR_CATEGORY_NETWORK = 0x3;
    public static final int ERROR_CATEGORY_CONTENT = 0x4;
    public static final int ERROR_CATEGORY_URI = 0x5;
    public static final int ERROR_CATEGORY_PROXY = 0x6;
    public static final int ERROR_CATEGORY_SAFEBROWSING = 0x7;

    public static final int ERROR_UNKNOWN = 0x11;

    // Security
    public static final int ERROR_SECURITY_SSL = 0x22;
    public static final int ERROR_SECURITY_BAD_CERT = 0x32;

    // Network
    public static final int ERROR_NET_INTERRUPT = 0x23;
    public static final int ERROR_NET_TIMEOUT = 0x33;
    public static final int ERROR_CONNECTION_REFUSED = 0x43;
    public static final int ERROR_UNKNOWN_SOCKET_TYPE = 0x53;
    public static final int ERROR_REDIRECT_LOOP = 0x63;
    public static final int ERROR_OFFLINE = 0x73;
    public static final int ERROR_PORT_BLOCKED = 0x83;
    public static final int ERROR_NET_RESET = 0x93;

    // Content
    public static final int ERROR_UNSAFE_CONTENT_TYPE = 0x24;
    public static final int ERROR_CORRUPTED_CONTENT = 0x34;
    public static final int ERROR_CONTENT_CRASHED = 0x44;
    public static final int ERROR_INVALID_CONTENT_ENCODING = 0x54;

    // URI
    public static final int ERROR_UNKNOWN_HOST = 0x25;
    public static final int ERROR_MALFORMED_URI = 0x35;
    public static final int ERROR_UNKNOWN_PROTOCOL = 0x45;
    public static final int ERROR_FILE_NOT_FOUND = 0x55;
    public static final int ERROR_FILE_ACCESS_DENIED = 0x65;

    // Proxy
    public static final int ERROR_PROXY_CONNECTION_REFUSED = 0x26;
    public static final int ERROR_UNKNOWN_PROXY_HOST = 0x36;

    // Safebrowsing
    public static final int ERROR_SAFEBROWSING_MALWARE_URI = 0x27;
    public static final int ERROR_SAFEBROWSING_UNWANTED_URI = 0x37;
    public static final int ERROR_SAFEBROWSING_HARMFUL_URI = 0x47;
    public static final int ERROR_SAFEBROWSING_PHISHING_URI = 0x57;
    GeckoResult<String> onLoadError(GeckoSession session, String uri, int category, int error);
  }

  /* PromptDelegate */

  private PromptDelegate mPromptDelegate;
  public void setPromptDelegate(PromptDelegate delegate) {
    Log.d(LOGTAG, "setPromptDelegate()");
    mPromptDelegate = delegate;
  }
  public PromptDelegate getPromptDelegate() {
    Log.d(LOGTAG, "getPromptDelegate()");
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
    Log.d(LOGTAG, "setPermissionDelegate()");
    mPermissionDelegate = delegate;
  }
  public PermissionDelegate getPermissionDelegate() {
    Log.d(LOGTAG, "getPermissionDelegate()");
    return mPermissionDelegate;
  }
  public interface PermissionDelegate {
    public static final int PERMISSION_GEOLOCATION = 0;
    public static final int PERMISSION_DESKTOP_NOTIFICATION = 1;
    public static final int PERMISSION_AUTOPLAY_MEDIA = 2;
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
    void onAndroidPermissionsRequest(GeckoSession session, String[] permissions, Callback callback);
    void onContentPermissionRequest(GeckoSession session, String uri, int type, String access, Callback callback);
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
    Log.d(LOGTAG, "setScrollDelegate()");
    mScrollDelegate = delegate;
  }
  public ScrollDelegate getScrollDelegate() {
    Log.d(LOGTAG, "getScrollDelegate()");
    return mScrollDelegate;
  }

  /**
   *
   * METHODS
   *
   */

  public void loadData(@NonNull final byte[] bytes, @Nullable final String mimeType) {
    Log.d(LOGTAG, "loadData()");
  }
  public void loadData(@NonNull final byte[] bytes, @Nullable final String mimeType, @Nullable final String baseUri) {
    Log.d(LOGTAG, "loadData(baseUri)");
  }
  public void loadUri(Uri uri) {
    this.loadUri(uri.toString());
  }
  public void loadUri(String uri) {
    Log.d(LOGTAG, "loadUri()");
    if (uri.startsWith("resource://")) {
      uri = "data:text/html,<p>Unknown URL scheme:" + uri + "</p><p>Go to: <a href='https://servo.org'>servo.org</a></p>";
    }
    // FIXME
    if (mServoReady) {
      Log.d(LOGTAG, "loadUri: mServo.loadUri()");
      mServo.loadUri(uri);
    } else {
      Log.d(LOGTAG, "loadUri: saving uri for later");
      mFutureUri = uri;
    }
  }

  private boolean mIsOpen = false;
  public boolean isOpen() {
    Log.d(LOGTAG, "isOpen()");
    return mIsOpen;
  }

  public void open(final @NonNull GeckoRuntime runtime) {
    Log.d(LOGTAG, "open()");
    mIsOpen = true;
    mActivity = (Activity) runtime.getContext();
  }

  public void closeWindow() {
    Log.d(LOGTAG, "closeWindow()");
  }
  public void reload() {
    Log.d(LOGTAG, "reload()");
    if (mServoReady) {
      mServo.reload();
    }
  }
  public void stop() {
    Log.d(LOGTAG, "stop()");
    // FIXME
  }
  public void goBack() {
    Log.d(LOGTAG, "goBack()");
    if (mServoReady) {
      mServo.goBack();
    }
  }
  public void goForward() {
    Log.d(LOGTAG, "goForward()");
    if (mServoReady) {
      mServo.goForward();
    }
  }
  public void click(final int x, final int y) {
    Log.d(LOGTAG, "click()");
    if (mServoReady) {
      mServo.click(x, y);
    }
  }

    public void scrollStart(final int deltaX, final int deltaY, final int x, final int y) {
      if (mServoReady) {
            mServo.scrollStart(deltaX, deltaY, x, y);
        }
    }
    public void scroll(final int deltaX, final int deltaY, final int x, final int y) {
      if (mServoReady) {
            mServo.scroll(deltaX, deltaY, x, y);
        }
    }
    public void scrollEnd(final int deltaX, final int deltaY, final int x, final int y) {
      if (mServoReady) {
            mServo.scrollEnd(deltaX, deltaY, x, y);
        }
    }


  public void setActive(boolean active) {
    Log.d(LOGTAG, "setActive()");
    // FIXME
  }
  public void importScript(final String url) {
    Log.d(LOGTAG, "importScript()");
    // FIXME
  }
  public void exitFullScreen() {
    Log.d(LOGTAG, "exitFullScreen()");
    // FIXME
  }
  public void enableTrackingProtection(int categories) {
    Log.d(LOGTAG, "enableTrackingProtection()");
    // FIXME
  }
  public void disableTrackingProtection() {
    Log.d(LOGTAG, "disableTrackingProtection()");
    // FIXME
  }

  public void getSurfaceBounds(@NonNull final Rect rect) {
    rect.set(0, 0, mWidth, mHeight);
  }

  public interface Response<T> {
    void respond(T val);
  }

  public interface TextInputDelegate {
      @Retention(RetentionPolicy.SOURCE)
      @IntDef({RESTART_REASON_FOCUS, RESTART_REASON_BLUR, RESTART_REASON_CONTENT_CHANGE})
      @interface RestartReason {}
      int RESTART_REASON_FOCUS = 0;
      int RESTART_REASON_BLUR = 1;
      int RESTART_REASON_CONTENT_CHANGE = 2;
      void restartInput(@NonNull GeckoSession session, @RestartReason int reason);
      void showSoftInput(@NonNull GeckoSession session);
      void hideSoftInput(@NonNull GeckoSession session);
      void updateSelection(@NonNull GeckoSession session, int selStart, int selEnd,
                           int compositionStart, int compositionEnd);
      void updateExtractedText(@NonNull GeckoSession session,
                               @NonNull ExtractedTextRequest request,
                               @NonNull ExtractedText text);
      void updateCursorAnchorInfo(@NonNull GeckoSession session, @NonNull CursorAnchorInfo info);
      void notifyAutoFill(@NonNull GeckoSession session, int notification, int virtualId);
  }


  public static class SessionState {
  }

  public void restoreState(final SessionState state) {
  }

  public GeckoResult<SessionState> saveState() {
    return null;
  }

}
