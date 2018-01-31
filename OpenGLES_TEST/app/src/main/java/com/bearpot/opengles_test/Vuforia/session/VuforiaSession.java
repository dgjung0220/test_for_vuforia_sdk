package com.bearpot.opengles_test.Vuforia.session;

/**
 * Created by dg.jung on 2018-01-30.
 */
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.bearpot.opengles_test.R;
import com.bearpot.opengles_test.Vuforia.Exception.VuforiaAppException;
import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;

public class VuforiaSession implements UpdateCallbackInterface {

    private static final String LOGTAG = "Vuforia Session";

    private Activity mActivity;
    private VuforiaSessionControl sessionControl;

    private boolean mStarted = false;
    private boolean mCameraRunning = false;
    private final Object mLifecycleLock = new Object();
    private int mVuforiaFlags = 0;

    private InitVuforiaTask mInitVuforiaTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;
    private StartVuforiaTask mStartVuforiaTask;
    private ResumeVuforiaTask mResumeVuforiaTask;

    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;

    public VuforiaSession(VuforiaSessionControl sessionControl) {
        this.sessionControl = sessionControl;
    }
    @Override
    public void Vuforia_onUpdate(State state) {
        sessionControl.onVuforiaUpdate(state);
    }

    public void onConfigrationChanged() {
        if (mStarted) {
            Device.getInstance().setConfigurationChanged();
        }
    }
    public void onResume() {
        if (mResumeVuforiaTask == null || mResumeVuforiaTask.getStatus() == ResumeVuforiaTask.Status.FINISHED) {
            resumeAR();
        }
    }
    public void onPause() { Vuforia.onPause(); }
    public void onSurfaceChanged(int width, int height) { Vuforia.onSurfaceChanged(width, height); }
    public void onSurfaceCreated() { Vuforia.onSurfaceCreated(); }

    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean> {

        private int mProgressValue = -1;
        @Override
        protected Boolean doInBackground(Void... params) {

            Vuforia.setInitParameters(mActivity, mVuforiaFlags, "AZZVMAX/////AAAAmdCcDVFVSk55s7HvuSdYgP55mPZitNdkEi2tG20ecda7zTSr6dNTnagYTUq97VmyitKIXgw5Wwo0v/tFBHoA88TXiyLQvxe1Ct+y9f3IVzC/jXGWSgIVVq+0SdSx0bHG0KL2etNLV8RqNzTSbuXqvZ38E9G6wkhZyVI32alNk8xQnwyjZhY3fRXyrSmyhcx9qs5nc38SG7TcuyCbcfTGj5jhTOnNSwgVnbXCpjRNLLSQ8jfDGQiw4OfOTASwQVm4PTVKwPqaxDsgzFyNpk7JAdbov9qZTvVP05PKIWlM89irZMJ/LHwvnART/HD85nJaWjtmAU32vVQe9qJ3dsbi3/wxFf5BVHtQweIrGNpQT+nq");

            do {
                mProgressValue = Vuforia.init();
            } while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);

            return (mProgressValue > 0);
        }

        protected void onPostExecute(Boolean result) {
            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution " + (result ? "successful" : "failed"));
            VuforiaAppException vuforiaAppException = null;

            if (result) {
                try {
                    mInitTrackerTask = new InitTrackerTask();
                    mInitTrackerTask.execute();
                } catch (Exception e) {
                    String logMessage = "Failed to initialize tracker.";
                    vuforiaAppException = new VuforiaAppException(VuforiaAppException.TRACKERS_INITIALIZATION_FAILURE, logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            } else {
                String logMessage;
                logMessage = getInitializationErrorString(mProgressValue);
                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage + " Exiting.");
                vuforiaAppException = new VuforiaAppException(VuforiaAppException.INITIALIZATION_FAILURE, logMessage);
            }

            if (vuforiaAppException != null) {
                sessionControl.onInitARDone(vuforiaAppException);
            }
        }
    }

    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {

            synchronized (mLifecycleLock) {
                return sessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result) {
            VuforiaAppException vuforiaAppException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution " + (result ? "successful" : "failed"));

            if (result) {
                try {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                } catch(Exception e) {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    vuforiaAppException = new VuforiaAppException(VuforiaAppException.LOADING_TRACKERS_FAILURE, logMessage);
                }
            } else {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);

                vuforiaAppException = new VuforiaAppException(VuforiaAppException.TRACKERS_INITIALIZATION_FAILURE, logMessage);
            }

            if (vuforiaAppException != null) {
                sessionControl.onInitARDone(vuforiaAppException);
            }
        }
    }

    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            synchronized (mLifecycleLock) {
                return sessionControl.doLoadTrackersData();
            }
        }

        protected void onPostExecute(Boolean result) {
            VuforiaAppException vuforiaAppException = null;
            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution " + (result ? "successful" : "failed"));

            if(!result) {
                String logMessage = "Failed to load tracker data";
                Log.e(LOGTAG, logMessage);
                vuforiaAppException = new VuforiaAppException(VuforiaAppException.LOADING_TRACKERS_FAILURE, logMessage);
            } else {
                System.gc();
                Vuforia.registerCallback(VuforiaSession.this);
                mStarted = true;
            }
            sessionControl.onInitARDone(vuforiaAppException);
        }
    }

    private class StartVuforiaTask extends AsyncTask<Void, Void, Boolean> {

        VuforiaAppException vuforiaAppException = null;

        @Override
        protected Boolean doInBackground(Void... voids) {
            synchronized (mLifecycleLock)
            {
                try {
                    startCameraAndTrackers(mCamera);
                }
                catch (VuforiaAppException exception) {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + exception);
                    vuforiaAppException = exception;
                }
            }

            return true;
        }
    }

    private class ResumeVuforiaTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            synchronized (mLifecycleLock) {
                Vuforia.onResume();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute");

            if (mStarted && !mCameraRunning) {
                startAR(mCamera);
                sessionControl.onVuforiaResumed();
            }
        }
    }

    public void initAR(Activity activity, int screenOrientation)
    {
        VuforiaAppException vuforiaAppException = null;
        mActivity = activity;

        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

        OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int i) {
                int activityRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                if(mLastRotation != activityRotation) {
                    mLastRotation = activityRotation;
                }
            }
            int mLastRotation = -1;
        };

        if(orientationEventListener.canDetectOrientation())
            orientationEventListener.enable();

        mActivity.setRequestedOrientation(screenOrientation);
        mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mVuforiaFlags = INIT_FLAGS.GL_20;

        if (mInitVuforiaTask != null) {
            String logMessage = "Cannot initialize SDK twice";
            vuforiaAppException = new VuforiaAppException(VuforiaAppException.VUFORIA_ALREADY_INITIALIZATED, logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaAppException == null) {
            try {
                mInitVuforiaTask = new InitVuforiaTask();
                mInitVuforiaTask.execute();
            }
            catch (Exception e) {
                String logMessage = "Initializing Vuforia SDK failed";
                vuforiaAppException = new VuforiaAppException(VuforiaAppException.INITIALIZATION_FAILURE, logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaAppException != null) {
            sessionControl.onInitARDone(vuforiaAppException);
        }
    }

    private void startCameraAndTrackers(int camera) throws VuforiaAppException {
        String error;

        if(mCameraRunning) {
            error = "Camera already running, unable to open again";
            Log.e(LOGTAG, error);
            throw new VuforiaAppException(VuforiaAppException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mCamera = camera;

        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaAppException(VuforiaAppException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new VuforiaAppException(VuforiaAppException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaAppException(VuforiaAppException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        sessionControl.doStartTrackers();
        mCameraRunning = true;
    }

    public void stopCamera() {
        if (mCameraRunning) {
            sessionControl.doStopTrackers();
            mCameraRunning = false;

            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }

    public void startAR(int camera) {
        mCamera = camera;
        VuforiaAppException vuforiaAppException = null;

        try {
            mStartVuforiaTask = new StartVuforiaTask();
            mStartVuforiaTask.execute();
        } catch (Exception e) {
            String logMessage = "Starting Vuforia failed";
            vuforiaAppException = new VuforiaAppException(VuforiaAppException.CAMERA_INITIALIZATION_FAILURE, logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaAppException != null) {
            sessionControl.onInitARDone(vuforiaAppException);
        }
    }

    public void stopAR() throws VuforiaAppException {
        if (mInitVuforiaTask != null && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED) {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }

        if (mLoadTrackerTask != null && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED) {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        mInitVuforiaTask = null;
        mLoadTrackerTask = null;

        mStarted = false;

        stopCamera();

        synchronized (mLifecycleLock) {
            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            unloadTrackersResult = sessionControl.doUnloadTrackersData();
            deinitTrackersResult = sessionControl.doDeinitTrackers();

            Vuforia.deinit();

            if (!unloadTrackersResult)
                throw new VuforiaAppException(VuforiaAppException.UNLOADING_TRACKERS_FAILURE, "Failed to unload trackers\' data");

            if (!deinitTrackersResult)
                throw new VuforiaAppException(VuforiaAppException.TRACKERS_DEINITIALIZATION_FAILURE, "Failed to deinitialize trackers");
        }
    }

    private void resumeAR() {
        VuforiaAppException vuforiaAppException = null;

        try {
            mResumeVuforiaTask = new ResumeVuforiaTask();
            mResumeVuforiaTask.execute();
        } catch(Exception e) {
            String logMessage = "Resuming Vuforia failed.";
            vuforiaAppException = new VuforiaAppException(VuforiaAppException.INITIALIZATION_FAILURE, logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaAppException != null) {
            sessionControl.onInitARDone(vuforiaAppException);
        }
    }

    public void pauseAR() throws VuforiaAppException {
        if (mStarted) {
            stopCamera();
        }
        Vuforia.onPause();
    }

    private String getInitializationErrorString(int code) {
        if (code == INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED)
            return mActivity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED);
        if (code == INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS)
            return mActivity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
        else
        {
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR);
        }
    }
}
