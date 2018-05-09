package org.mozilla.geckoview;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;


public class ServoSurface {
    private static final String LOGTAG = "java::ServoSurface";

    private EGLConfig[] maEGLconfigs = null;
    private EGLDisplay mEglDisplay = null;
    private EGLContext mEglContext = null;
    private EGLSurface mEglSurface = null;

    public ServoSurface(Surface surface, int width, int height) {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        Log.w(LOGTAG, "ServoSurface()");
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("Error: eglInitialize() Failed " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        maEGLconfigs = new EGLConfig[1];
        int[] configsCount = new int[1];
        int[] configSpec = new int[] {
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        if ((!EGL14.eglChooseConfig(mEglDisplay, configSpec, 0, maEGLconfigs, 0,1, configsCount, 0)) || (configsCount[0] == 0)) {
            throw new IllegalArgumentException("Error: eglChooseConfig() Failed " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        if (maEGLconfigs[0] == null) {
            throw new RuntimeException("Error: eglConfig() not Initialized");
        }
        int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, maEGLconfigs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, maEGLconfigs[0], surface, new int[]{EGL14.EGL_NONE}, 0);
        if (mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE) {
            int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW)  {
                Log.e(LOGTAG, "Error: createWindowSurface() Returned EGL_BAD_NATIVE_WINDOW.");
                return;
            }
            throw new RuntimeException("Error: createWindowSurface() Failed " + GLUtils.getEGLErrorString(error));
        }

        makeCurrent();
    }


    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("Error: eglMakeCurrent() Failed " + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public void swapBuffers() {
        EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
    }
}