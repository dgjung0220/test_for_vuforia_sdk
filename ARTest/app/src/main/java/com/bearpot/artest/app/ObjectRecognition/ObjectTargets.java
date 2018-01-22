package com.bearpot.artest.app.ObjectRecognition;

import android.app.Activity;

import com.bearpot.artest.app.ObjectRecognition.utils.ARApplicationGLView;
import com.bearpot.artest.app.ObjectRecognition.utils.LoadingDialogHandler;
import com.bearpot.artest.app.ObjectRecognition.utils.Texture;
import com.vuforia.DataSet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.GestureDetector;
import android.view.View;
import android.widget.RelativeLayout;

import com.bearpot.artest.ARApplicationControl;
import com.bearpot.artest.ARApplicationException;
import com.bearpot.artest.ARApplicationSession;
import com.vuforia.State;

import java.util.Vector;

/**
 * Created by dg.jung on 2018-01-22.
 */

public class ObjectTargets extends Activity implements ARApplicationControl {

    private static final String LOGTAG = "ObjectRecognition";
    ARApplicationSession vuforiaSession;

    private DataSet mCurrentDataset;

    // OpenGL View
    private ARApplicationGLView mGLView;

    // Renderer
    private ObjectTargetRenderer mRenderer;
    private Vector<Texture> mTextures;

    private GestureDetector mGestureDetector;

    private boolean mFlash = false;
    private boolean mExtendedTracking = false;

    private View mFlashOptionView;
    private RelativeLayout mUILayout;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    boolean mIsDroidDevice = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onInitARDone(ARApplicationException e) {

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
}
