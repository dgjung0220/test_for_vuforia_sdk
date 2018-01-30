package com.bearpot.opengles_test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * Created by dg.jung on 2018-01-26.
 */

public class VuforiaAppSurfaceView extends GLSurfaceView {

    private final VuforiaAppRenderer mVuforiaAppRenderer;

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
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
    }

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
