package com.bearpot.opengles_test;

import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * Created by dg.jung on 2018-01-26.
 */

public class VuforiaAppSurfaceView extends GLSurfaceView {

    private final VuforiaAppRenderer mVuforiaAppRenderer;

    public VuforiaAppSurfaceView(Context context) {
        super(context);

        // OpenGL ES 2.0 context 생성
        setEGLContextClientVersion(2);

        mVuforiaAppRenderer = new VuforiaAppRenderer();
        setRenderer(mVuforiaAppRenderer);

        // Surface 생성될 때와, GLSurfaceView 클래스의 requestRender 메소드 호출시만 화면을 그림
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
