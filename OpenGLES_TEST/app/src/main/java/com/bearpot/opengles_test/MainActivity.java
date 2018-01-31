package com.bearpot.opengles_test;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bearpot.opengles_test.Vuforia.Exception.VuforiaAppException;
import com.bearpot.opengles_test.Vuforia.gl.VuforiaAppSurfaceView;
import com.bearpot.opengles_test.Vuforia.session.VuforiaSession;
import com.bearpot.opengles_test.Vuforia.session.VuforiaSessionControl;
import com.bearpot.opengles_test.Vuforia.utils.LoadingDialogHandler;
import com.bearpot.opengles_test.Vuforia.utils.Texture;
import com.vuforia.DataSet;
import com.vuforia.State;
import com.vuforia.Vuforia;

import java.util.Vector;

public class MainActivity extends AppCompatActivity implements VuforiaSessionControl {

    private static final String LOGTAG = "MainActivity";
    VuforiaSession vuforiaSession;

    private VuforiaAppSurfaceView mGLView;
    private VuMarkRenderer mRenderer;

    private RelativeLayout mUILayout;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    private Vector<Texture> mTextures;
    private DataSet dataSet = null;

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    boolean mIsDroidDevice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vuforiaSession = new VuforiaSession(this);
        startLoadingAnimation();
        vuforiaSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTextures = new Vector<Texture>();
        loadTextures();
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");
    }

    private void loadTextures() {
        mTextures.add(Texture.loadTextureFromApk("TextureWireframe.png", getAssets()));
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGLView != null) {
            mGLView.setVisibility(View.INVISIBLE);
            mGLView.onPause();
        }

        try {
            vuforiaSession.pauseAR();
        } catch (VuforiaAppException e) {
            Log.e(LOGTAG, e.getString());
        }

    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        vuforiaSession.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaSession.stopAR();
        } catch (VuforiaAppException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;
        System.gc();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaSession.onConfigrationChanged();
    }

    private void initApplicationAR() {

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGLView = new VuforiaAppSurfaceView(this);
        mGLView.init(translucent, depthSize, stencilSize);

        mRenderer = new VuMarkRenderer(this, vuforiaSession);
        mRenderer.setTextures(mTextures);
        mGLView.setRenderer(mRenderer);

    }

    @Override
    public boolean doInitTrackers() {
        return false;
    }

    @Override
    public boolean doLoadTrackersData() {
        return false;
    }

    @Override
    public boolean doStartTrackers() {
        return false;
    }

    @Override
    public boolean doStopTrackers() {
        return false;
    }

    @Override
    public boolean doUnloadTrackersData() {
        return false;
    }

    @Override
    public boolean doDeinitTrackers() {
        return false;
    }

    @Override
    public void onInitARDone(VuforiaAppException e) {

    }

    @Override
    public void onVuforiaUpdate(State state) {

    }

    @Override
    public void onVuforiaResumed() {

    }

    @Override
    public void onVuforiaStarted() {

    }

    public void showProgressIndicator(boolean show) {
        if (loadingDialogHandler != null) {
            if (show) {
                loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            } else {
                loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }

    private void startLoadingAnimation() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay, null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
