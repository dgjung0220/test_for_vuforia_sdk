package com.bearpot.opengles_test.Vuforia.gl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by dg.jung on 2018-01-26.
 */

public class VuforiaAppSurfaceView extends GLSurfaceView {

    private static final String LOGTAG = "Vuforia GLSurfaceView";

    public VuforiaAppSurfaceView(Context context) {
        super(context);

        /* mVuforiaAppRenderer = new VuforiaAppRenderer();
        setRenderer(mVuforiaAppRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); */
    }

    public void init(boolean translucent, int depth, int stencil) {
        Log.i(LOGTAG, "Using OpenGL ES 2.0");
        Log.i(LOGTAG, "Using " + (translucent ? "translucent" : "opaque") + " GLView, depth buffer size: " + depth + ", stencil size: " + stencil);

        if (translucent) {
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth, stencil) : new ConfigChooser(5, 6, 5, 0, depth, stencil));
    }

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {

        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            EGLContext context;

            Log.i(LOGTAG, "Creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);

            int[] attrib_list_gl20 = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            context = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list_gl20);

            checkEglError("After eglCreateContext", egl);
            return context;
        }

        @Override
        public void destroyContext(EGL10 egl10, EGLDisplay eglDisplay, EGLContext eglContext) {
            egl10.eglDestroyContext(eglDisplay, eglContext);
        }
    }

    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];

        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
        }

        private EGLConfig getMatchingConfig(EGL10 egl, EGLDisplay display, int[] configAttribs) {

            int[] num_config = new int[1];
            egl.eglChooseConfig(display, configAttribs, null, 0, num_config);

            int numConfigs = num_config[0];
            if (numConfigs <= 0)
                throw new IllegalArgumentException("No matching EGL configs");

            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, configAttribs, configs, numConfigs, num_config);

            return chooseConfig(egl, display, configs);
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay eglDisplay) {
            final int EGL_OPENGL_ES2_BIT = 0x0004;
            final int[] s_configAttribs_gl20 = { EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };

            return getMatchingConfig(egl, eglDisplay, s_configAttribs_gl20);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {

            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

                if (d < mDepthSize || s < mStencilSize)
                    continue;

                int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config;
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue))
                return mValue[0];

            return defaultValue;
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(LOGTAG, String.format("%s: EGL error: 0x%x", prompt, error));
        }
    }

/*    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch(event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - mPreviousX;
                float dy = y = mPreviousY;

                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }

                mVuforiaAppRenderer.setAngle(mVuforiaAppRenderer.getAngle() + ((dx + dy) * TOUCH_SCALE_FACTOR));
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    } */
}
