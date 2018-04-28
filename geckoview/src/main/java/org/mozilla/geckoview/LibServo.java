package org.mozilla.geckoview;

public class LibServo {
    public native String version();
    public native void init(String url, WakeupCallback wakeup, ServoCallbacks callbacks, int width, int height);
    public native void performUpdates();
    public native void resize(int width, int height);
    public native void reload();
    public native void stop();
    public native void goBack();
    public native void goForward();
    public native void loadUri(String uri);
    public native void scroll(int dx, int dy, int x, int y, int phase);
    public native void click(int x, int y);

    public LibServo() {
        System.loadLibrary("c++_shared");
        System.loadLibrary("servobridge");
    }

    public interface WakeupCallback {
        void wakeup();
    }

    public interface ServoCallbacks {
        void flush();
        void onLoadStarted();
        void onLoadEnded();
        void onTitleChanged(String title);
        void onUrlChanged(String url);
        void onHistoryChanged(boolean canGoBack, boolean canGoForward);
    }
}
