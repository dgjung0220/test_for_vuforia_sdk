/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.bearpot.artest.app.MultiTargets;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.bearpot.artest.ARAppRenderer;
import com.bearpot.artest.ARAppRendererControl;
import com.bearpot.artest.ARApplicationSession;
import com.bearpot.artest.R;
import com.bearpot.artest.utils.CubeObject;
import com.bearpot.artest.utils.CubeShaders;
import com.bearpot.artest.utils.SampleUtils;
import com.bearpot.artest.utils.Texture;
import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.MultiTargetResult;
import com.vuforia.ObjectTarget;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MultiTargetRenderer implements GLSurfaceView.Renderer, ARAppRendererControl
{
    private static final String LOGTAG = "MultiTargetRenderer";
    
    private ARApplicationSession vuforiaAppSession;
    private MultiTargets mActivity;
    private ARAppRenderer mSampleAppRenderer;

    private boolean mIsActive = false;
    
    private int shaderProgramID;

    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    private Vector<Texture> mTextures;
    private int opacityHandle;
    private int colorHandle;
    
    private double prevTime;
    private float rotateAngle;
    
    private CubeObject cubeObject = new CubeObject();
    private BowlAndSpoonObject bowlAndSpoonObject = new BowlAndSpoonObject();

    final static float kBowlScaleX = 0.12f * 0.15f;
    final static float kBowlScaleY = 0.12f * 0.15f;
    final static float kBowlScaleZ = 0.12f * 0.15f;

    public MultiTargetRenderer(MultiTargets activity, ARApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new ARAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.010f, 5f);
    }

    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
        mSampleAppRenderer.onSurfaceCreated();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }
    
    
    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }
    
    
    private void initRendering()
    {
        Log.d(LOGTAG, "initRendering");
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);
        
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(CubeShaders.CUBE_MESH_VERTEX_SHADER, CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");
        opacityHandle = GLES20.glGetUniformLocation(shaderProgramID, "opacity");
        colorHandle = GLES20.glGetUniformLocation(shaderProgramID, "color");
    }

    public void renderFrame(State state, float[] projectionMatrix)
    {
        SampleUtils.checkGLError("Check gl errors prior render Frame");

        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Did we find any trackables this frame?
        if (state.getNumTrackableResults() != 0)
        {
            // Get the trackable:
            TrackableResult result = null;
            int numResults = state.getNumTrackableResults();

            // Browse results searching for the MultiTarget
            for (int j = 0; j < numResults; j++)
            {
                result = state.getTrackableResult(j);
                printUserData(result.getTrackable());

                if (result.isOfType(MultiTargetResult.getClassType()))
                    break;
            }

            // If it was not found exit
            if (result == null)
            {
                // Clean up and leave
                GLES20.glDisable(GLES20.GL_BLEND);
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);

                Renderer.getInstance().end();
                return;
            }

            ObjectTarget objectTarget = (ObjectTarget) result.getTrackable();

            float[] objectSize = objectTarget.getSize().getData();

            Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            float[] modelViewProjection = new float[16];
            Matrix.translateM(modelViewMatrix, 0, objectSize[0]/2, objectSize[1]/2, objectSize[2]/2);
            Matrix.scaleM(modelViewMatrix, 0, objectSize[0]/2, objectSize[1]/2, objectSize[2]/2);
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            GLES20.glUseProgram(shaderProgramID);

            // Draw the cube:
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, cubeObject.getVertices());
            GLES20.glUniform1f(opacityHandle, 0.3f);
            GLES20.glUniform3f(colorHandle, 0.0f, 0.0f, 0.0f);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, cubeObject.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, cubeObject.getIndices());

            GLES20.glDisable(GLES20.GL_CULL_FACE);

            // Draw the bowl:
            modelViewMatrix = modelViewMatrix_Vuforia.getData();
            animateBowl(modelViewMatrix);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, -0.50f * 0.12f, 0.00135f * 0.12f);
            Matrix.rotateM(modelViewMatrix, 0, -90.0f, 1.0f, 0, 0);
            Matrix.scaleM(modelViewMatrix, 0, kBowlScaleX, kBowlScaleY, kBowlScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, bowlAndSpoonObject.getVertices());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, bowlAndSpoonObject.getTexCoords());

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(1).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, bowlAndSpoonObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, bowlAndSpoonObject.getIndices());

            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            SampleUtils.checkGLError("MultiTargets renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
        
    }
    
    
    private void animateBowl(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis(); // Get real time difference
        float dt = (float) (time - prevTime) / 1000; // from frame to frame
        
        rotateAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
        rotateAngle %= 360;
        Log.d(LOGTAG, "Delta animation time: " + rotateAngle);
        
        Matrix.rotateM(modelViewMatrix, 0, rotateAngle, 0.0f, 1.0f, 0.0f);
        
        prevTime = time;
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }

    private void printUserData(Trackable trackable)
    {
        String userData = (String) trackable.getUserData(); // "moomin"
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }
}
