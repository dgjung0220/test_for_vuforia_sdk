package com.bearpot.opengles_test;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by dg.jung on 2018-01-26.
 */

public class Triangle {

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;


    // 1. 삼각형 vertex를 위한 좌표
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = {
            0.0f, 0.622008459f, 0.0f,
            -0.5f, -0.311004243f, 0.0f,
            0.5f, -0.311004243f, 0.0f
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    float color[] = { 0.9f, 0.2f, 0.2f, 1.0f };        // R, G, B, Alpha 값

    public Triangle() {

        Log.d("DONGGOO", "Triangle Constructor");
        // 2. ByteBuffer 할당
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        // 3. endian 값 지정
        bb.order(ByteOrder.nativeOrder());
        // 4. ByteBuffer를 FloatBuffer 로 변환
        vertexBuffer = bb.asFloatBuffer();
        // 5. float 배열에 정의된 좌표들을 FloatBuffer에 저장.
        vertexBuffer.put(triangleCoords);
        // 6. 읽어올 버퍼의 위치를 0으로 설정.
        vertexBuffer.position(0);

        int vertexShader = VuforiaAppRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = VuforiaAppRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw() {

        Log.d("DONGGOO", "Triangle Draw");
        GLES20.glUseProgram(mProgram);
        // vertex shader의 'vPosition' 멤버에 대한 핸들을 가져옴.
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        // Program 객체로부터  fragment shader의 vColor 멤버에 대한 핸들을 가져옴.
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
