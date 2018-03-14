package org.mozilla.geckoview;

import android.util.Log;

public class LibServo {
    public native String version();
    public native void init(String url, String resources_path, ServoCallbacks callbacks);

    public LibServo() {
        System.loadLibrary("c++_shared");
        System.loadLibrary("servobridge");
    }

    public interface ServoCallbacks {
        void wakeup();
        void flush();
        void log(String log);
        void onLoadStarted();
        void onLoadEnded();
        void onTitleChanged(String title);
        void onUrlChanged(String url);
        void onHistoryChanged(boolean canGoBack, boolean canGoForward);
    }
}
