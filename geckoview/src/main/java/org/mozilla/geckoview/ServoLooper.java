package org.mozilla.geckoview;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

public class ServoLooper extends Thread {
    public Handler mHandler;
    public ServoSurface mSSurface;
    public Surface mASurface;
    public int mWidth;
    public int mHeight;
    private GeckoSession mSession;

    public ServoLooper(GeckoSession session, Surface surface, int width, int height) {
        super();
        mSession = session;
        mWidth = width;
        mHeight = height;
        mASurface = surface;
        session.setView(this);
    }

    public void queueEvent(Runnable r) {
        mHandler.post(r);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void requestRender() {
        mHandler.post(new Runnable() {
            public void run() {
                mSSurface.swapBuffers();
            }
        });
    }

    // FIXME
    @SuppressLint("HandlerLeak")
    @Override
    public void run() {
        Looper.prepare();

        mSSurface = new ServoSurface(mASurface, mWidth, mHeight);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                mSession.onGLReady();
            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
            }
        };

        Looper.loop();
    }
}
