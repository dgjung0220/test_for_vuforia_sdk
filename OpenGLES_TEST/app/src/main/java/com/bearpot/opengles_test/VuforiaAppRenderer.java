package com.bearpot.opengles_test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by dg.jung on 2018-01-26.
 */

public class VuforiaAppRenderer implements GLSurfaceView.Renderer {

    private final float[] mMVPMatrix = new float[16];               // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private Triangle mTriangle;
    public volatile float mAngle;

    public static int loadShader(int type, String shaderCode) {
        // type - vertex shader (GLES20.GL_VERTEX_SHADER
        //      - fragment shader (GLES20.GL_FRAGMENT_SHADER
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);

        GLES20.glCompileShader(shader);
        return shader;
    }

    // GLSurfaceView가 생성되었을 때 한 번 호출되는 메소드.
    // OpenGL 환경 설정, OpenGL 그래픽 객체 초기화 등과 같은 처리를 할 때 사용함.
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Shape 이 정의된 Triangle 클래스의 인스턴스를 생성.
        mTriangle = new Triangle();
    }

    // GLSurfaceView 가 다시 그렬질 때마다 호출되는 메소드
    @Override
    public void onDrawFrame(GL10 gl10) {
        // glClearColor 에서 셋팅한 색으로 buffer clear
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // 기타
        // depth buffer : GL_DEPTH_BFFER_BIT
        // stencil buffer : GL_STENCIL_BFFER_BIT

        // 카메라 위치를 나타내는 Camera view Matrix를 정의
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        float[] scratch = new float[16];
        float[] mRotationMatrix = new float[16];

        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        mTriangle.draw(scratch);
    }

    // GLSurfaceView의 크기 변경 또는 디바이스 화면의 방향 전환 등으로 인해 GLSurfaceView의 geometry가 바뀔 때 호출되는 메소드.
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // Viewport 설정
        GLES20.glViewport(0,0, width, height);

        // GLSurfaceView 너비와 높이 사이의 비율 계산.
        float ratio = (float) width / height;
        // 3차원 공간의 점을 2차원 화면에 보여주기 위해 사용되는 projection matrix
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public float getAngle() {
        return mAngle;
    }
    public void setAngle(float angle) {
        mAngle = angle;
    }
}
