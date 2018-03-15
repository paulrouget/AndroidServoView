package org.mozilla.geckoview;

import android.util.Log;

public class LibServo {
    public native String version();
    public native void init(String url, String resources_path, WakeupCallback wakeup, ServoCallbacks callbacks);
    public native void performUpdates();

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
