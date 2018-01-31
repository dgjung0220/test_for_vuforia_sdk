package com.bearpot.opengles_test.Vuforia.gl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.bearpot.opengles_test.Vuforia.utils.SampleUtils;
import com.bearpot.opengles_test.Vuforia.utils.VideoBackgroundShader;
import com.vuforia.COORDINATE_SYSTEM_TYPE;
import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.GLTextureUnit;
import com.vuforia.Matrix34F;
import com.vuforia.Mesh;
import com.vuforia.Renderer;
import com.vuforia.RenderingPrimitives;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackerManager;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.VIEW;
import com.vuforia.Vec2F;
import com.vuforia.Vec2I;
import com.vuforia.Vec4I;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.VideoMode;
import com.vuforia.ViewList;

/**
 * Created by dg.jung on 2018-01-31.
 */

public class VuforiaAppRenderer {

    private static final String LOGTAG = "ARAppRenderer";

    private RenderingPrimitives mRenderingPrimitives = null;
    private VuforiaAppRendererControl mRenderingInterface = null;
    private Activity mActivity = null;

    private Renderer mRenderer = null;
    private int currentView = VIEW.VIEW_SINGULAR;
    private float mNearPlane = -1.0f;
    private float mFarPlane = -1.0f;

    private GLTextureUnit videoBackgroundTex = null;

    private int vbShaderProgramID = 0;
    private int vbTexSampler2DHandle = 0;
    private int vbVertexHandle = 0;
    private int vbTexCoordHandle = 0;
    private int vbProjectionMatrixHandle = 0;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private boolean mIsPortrait = false;

    public VuforiaAppRenderer(VuforiaAppRendererControl renderingInterface, Activity activity, int deviceMode, boolean stereo, float nearPlane, float farPlane) {
        mActivity = activity;
        mRenderingInterface = renderingInterface;
        mRenderer = Renderer.getInstance();

        if(farPlane < nearPlane) {
            Log.e(LOGTAG, "Far plane should be greater than near plane");
            throw new IllegalArgumentException();
        }

        setNearFarPlanes(nearPlane, farPlane);

        if(deviceMode != Device.MODE.MODE_AR && deviceMode != Device.MODE.MODE_VR) {
            Log.e(LOGTAG, "Device mode should be Device.MODE.MODE_AR or Device.MODE.MODE_VR");
            throw new IllegalArgumentException();
        }

        Device device = Device.getInstance();
        device.setViewerActive(stereo); // Indicates if the app will be using a viewer, stereo mode and initializes the rendering primitives
        device.setMode(deviceMode); // Select if we will be in AR or VR mode
    }

    public void onSurfaceCreated()
    {
        initRendering();
    }

    public void onConfigurationChanged(boolean isARActive) {
        updateActivityOrientation();
        storeScreenDimensions();

        if(isARActive)
            configureVideoBackground();

        mRenderingPrimitives = Device.getInstance().getRenderingPrimitives();
    }

    void initRendering() {
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(VideoBackgroundShader.VB_VERTEX_SHADER, VideoBackgroundShader.VB_FRAGMENT_SHADER);

        // Rendering configuration for video background
        if (vbShaderProgramID > 0) {
            // Activate shader:
            GLES20.glUseProgram(vbShaderProgramID);
            // Retrieve handler for texture sampler shader uniform variable:
            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D");
            // Retrieve handler for projection matrix shader uniform variable:
            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix");
            vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexPosition");
            vbTexCoordHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexTexCoord");
            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix");
            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D");

            // Stop using the program
            GLES20.glUseProgram(0);
        }
        videoBackgroundTex = new GLTextureUnit();
    }

    public void render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        State state;

        state = TrackerManager.getInstance().getStateUpdater().updateState();
        mRenderer.begin(state);

        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW);
        else
            GLES20.glFrontFace(GLES20.GL_CCW);

        ViewList viewList = mRenderingPrimitives.getRenderingViews();

        for (int v = 0; v < viewList.getNumViews(); v++) {
            int viewID = viewList.getView(v);
            Vec4I viewport;
            // Get the viewport for that specific view
            viewport = mRenderingPrimitives.getViewport(viewID);

            // Set viewport for current view
            GLES20.glViewport(viewport.getData()[0], viewport.getData()[1], viewport.getData()[2], viewport.getData()[3]);

            // Set scissor
            GLES20.glScissor(viewport.getData()[0], viewport.getData()[1], viewport.getData()[2], viewport.getData()[3]);

            // Get projection matrix for the current view. COORDINATE_SYSTEM_CAMERA used for AR and
            // COORDINATE_SYSTEM_WORLD for VR
            Matrix34F projMatrix = mRenderingPrimitives.getProjectionMatrix(viewID, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA,
                    state.getCameraCalibration());

            // Create GL matrix setting up the near and far planes
            float rawProjectionMatrixGL[] = Tool.convertPerspectiveProjection2GLMatrix(
                    projMatrix,
                    mNearPlane,
                    mFarPlane)
                    .getData();

            // Apply the appropriate eye adjustment to the raw projection matrix, and assign to the global variable
            float eyeAdjustmentGL[] = Tool.convert2GLMatrix(mRenderingPrimitives.getEyeDisplayAdjustmentMatrix(viewID)).getData();

            float projectionMatrix[] = new float[16];
            // Apply the adjustment to the projection matrix
            Matrix.multiplyMM(projectionMatrix, 0, rawProjectionMatrixGL, 0, eyeAdjustmentGL, 0);

            currentView = viewID;
            if(currentView != VIEW.VIEW_POSTPROCESS)
                mRenderingInterface.renderFrame(state, projectionMatrix);
        }

        mRenderer.end();
    }

    public void setNearFarPlanes(float near, float far) {
        mNearPlane = near;
        mFarPlane = far;
    }

    public void renderVideoBackground() {
        if(currentView == VIEW.VIEW_POSTPROCESS)
            return;

        int vbVideoTextureUnit = 0;
        videoBackgroundTex.setTextureUnit(vbVideoTextureUnit);
        if (!mRenderer.updateVideoBackgroundTexture(videoBackgroundTex)) {
            Log.e(LOGTAG, "Unable to update video background texture");
            return;
        }

        float[] vbProjectionMatrix = Tool.convert2GLMatrix(mRenderingPrimitives.getVideoBackgroundProjectionMatrix(currentView, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA)).getData();

        if (Device.getInstance().isViewerActive()) {
            float sceneScaleFactor = (float)getSceneScaleFactor();
            Matrix.scaleM(vbProjectionMatrix, 0, sceneScaleFactor, sceneScaleFactor, 1.0f);
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        Mesh vbMesh = mRenderingPrimitives.getVideoBackgroundMesh(currentView);
        GLES20.glUseProgram(vbShaderProgramID);
        GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0, vbMesh.getPositions().asFloatBuffer());
        GLES20.glVertexAttribPointer(vbTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, vbMesh.getUVs().asFloatBuffer());
        GLES20.glUniform1i(vbTexSampler2DHandle, vbVideoTextureUnit);

        // First, we enable the vertex arrays
        GLES20.glEnableVertexAttribArray(vbVertexHandle);
        GLES20.glEnableVertexAttribArray(vbTexCoordHandle);
        // Pass the projection matrix to OpenGL
        GLES20.glUniformMatrix4fv(vbProjectionMatrixHandle, 1, false, vbProjectionMatrix, 0);
        // Then, we issue the render call
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, vbMesh.getNumTriangles() * 3, GLES20.GL_UNSIGNED_SHORT, vbMesh.getTriangles().asShortBuffer());
        // Finally, we disable the vertex arrays
        GLES20.glDisableVertexAttribArray(vbVertexHandle);
        GLES20.glDisableVertexAttribArray(vbTexCoordHandle);

        SampleUtils.checkGLError("Rendering of the video background failed");
    }

    static final float VIRTUAL_FOV_Y_DEGS = 85.0f;
    static final float M_PI = 3.14159f;

    double getSceneScaleFactor() {
        Vec2F fovVector = CameraDevice.getInstance().getCameraCalibration().getFieldOfViewRads();
        float cameraFovYRads = fovVector.getData()[1];

        // Get the y-dimension of the virtual camera field of view
        float virtualFovYRads = VIRTUAL_FOV_Y_DEGS * M_PI / 180;

        return Math.tan(cameraFovYRads / 2) / Math.tan(virtualFovYRads / 2);
    }

    public void configureVideoBackground() {
        CameraDevice cameraDevice = CameraDevice.getInstance();
        VideoMode vm = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT);

        VideoBackgroundConfig config = new VideoBackgroundConfig();
        config.setEnabled(true);
        config.setPosition(new Vec2I(0, 0));

        int xSize = 0, ySize = 0;

        if (mIsPortrait) {
            xSize = (int) (vm.getHeight() * (mScreenHeight / (float) vm.getWidth()));
            ySize = mScreenHeight;

            if (xSize < mScreenWidth) {
                xSize = mScreenWidth;
                ySize = (int) (mScreenWidth * (vm.getWidth() / (float) vm.getHeight()));
            }
        } else  {
            xSize = mScreenWidth;
            ySize = (int) (vm.getHeight() * (mScreenWidth / (float) vm.getWidth()));

            if (ySize < mScreenHeight) {
                xSize = (int) (mScreenHeight * (vm.getWidth() / (float) vm.getHeight()));
                ySize = mScreenHeight;
            }
        }

        config.setSize(new Vec2I(xSize, ySize));

        Log.i(LOGTAG, "Configure Video Background : Video (" + vm.getWidth() + " , " + vm.getHeight() + "), Screen (" + mScreenWidth + " , " + mScreenHeight + "), mSize (" + xSize + " , " + ySize + ")");
        Renderer.getInstance().setVideoBackgroundConfig(config);
    }

    @SuppressLint("NewApi")
    private void storeScreenDimensions() {
        // Query display dimensions:
        Point size = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getRealSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
    }

    private void updateActivityOrientation() {
        Configuration config = mActivity.getResources().getConfiguration();

        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                mIsPortrait = true;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                mIsPortrait = false;
                break;
            case Configuration.ORIENTATION_UNDEFINED:
            default:
                break;
        }

        Log.i(LOGTAG, "Activity is in " + (mIsPortrait ? "PORTRAIT" : "LANDSCAPE"));
    }
}
