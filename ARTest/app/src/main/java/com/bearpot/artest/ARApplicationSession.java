package com.bearpot.artest;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vuforia.CameraDevice;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.State;
import com.vuforia.Vuforia;

/**
 * Created by dg.jung on 2018-01-22.
 */

public class ARApplicationSession implements Vuforia.UpdateCallbackInterface {

    private static final String LOGTAG = "ARApplicationSession";

    // Reference to the current activity
    private Activity mActivity;
    private ARApplicationControl mSessionControl;

    // Flags
    private boolean mStarted = false;
    private boolean mCameraRunning = false;

    // The async tasks to initialize the Vuforia SDK
    private InitVuforiaTask mInitVuforiaTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;
    private StartVuforiaTask mStartVuforiaTask;
    private ResumeVuforiaTask mResumeVuforiaTask;

    private final Object mLifecycleLock = new Object();
    // Vuforia initialization flags:
    private int mVuforiaFlags = 0;
    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;

    @Override
    public void Vuforia_onUpdate(State state) {
        mSessionControl.onVuforiaUpdate(state);
    }

    // An async task to initialize Vuforia asynchronously.
    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;


        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mLifecycleLock)
            {
                Vuforia.setInitParameters(mActivity, mVuforiaFlags, "AZZVMAX/////AAAAmdCcDVFVSk55s7HvuSdYgP55mPZitNdkEi2tG20ecda7zTSr6dNTnagYTUq97VmyitKIXgw5Wwo0v/tFBHoA88TXiyLQvxe1Ct+y9f3IVzC/jXGWSgIVVq+0SdSx0bHG0KL2etNLV8RqNzTSbuXqvZ38E9G6wkhZyVI32alNk8xQnwyjZhY3fRXyrSmyhcx9qs5nc38SG7TcuyCbcfTGj5jhTOnNSwgVnbXCpjRNLLSQ8jfDGQiw4OfOTASwQVm4PTVKwPqaxDsgzFyNpk7JAdbov9qZTvVP05PKIWlM89irZMJ/LHwvnART/HD85nJaWjtmAU32vVQe9qJ3dsbi3/wxFf5BVHtQweIrGNpQT+nq");

                do
                {
                    mProgressValue = Vuforia.init();
                    publishProgress(mProgressValue);
                } while (!isCancelled() && mProgressValue >= 0
                        && mProgressValue < 100);
                return (mProgressValue > 0);
            }
        }

        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }

        protected void onPostExecute(Boolean result)
        {
            // Done initializing Vuforia, proceed to next application
            // initialization status:
            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution " + (result ? "successful" : "failed"));

            ARApplicationException vuforiaException = null;

            if (result)
            {
                try {
                    mInitTrackerTask = new InitTrackerTask();
                    mInitTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to initialize tracker.";
                    vuforiaException = new ARApplicationException(ARApplicationException.TRACKERS_INITIALIZATION_FAILURE, logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            } else
            {
                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                logMessage = getInitializationErrorString(mProgressValue);

                // Log error:
                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage + " Exiting.");

                vuforiaException = new ARApplicationException(ARApplicationException.INITIALIZATION_FAILURE, logMessage);
            }

            if (vuforiaException != null)
            {
                // Send Vuforia Exception to the application and call initDone to stop initialization process
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    // An async task to resume Vuforia asynchronously
    private class ResumeVuforiaTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                Vuforia.onResume();
            }

            return null;
        }

        protected void onPostExecute(Void result)
        {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute");

            // We may start the camera only if the Vuforia SDK has already been initialized
            if (mStarted && !mCameraRunning)
            {
                startAR(mCamera);
                mSessionControl.onVuforiaResumed();
            }
        }
    }

    // An async task to initialize trackers asynchronously
    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected  Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result)
        {

            ARApplicationException vuforiaException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution " + (result ? "successful" : "failed"));

            if (result)
            {
                try {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    vuforiaException = new ARApplicationException(ARApplicationException.LOADING_TRACKERS_FAILURE, logMessage);
                }
            }
            else
            {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);

                // Error loading dataset
                vuforiaException = new ARApplicationException(ARApplicationException.TRACKERS_INITIALIZATION_FAILURE, logMessage);
            }

            if (vuforiaException != null)
            {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    // An async task to load the tracker data asynchronously.
    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doLoadTrackersData();
            }
        }

        protected void onPostExecute(Boolean result)
        {

            ARApplicationException vuforiaException = null;

            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            if (!result)
            {
                String logMessage = "Failed to load tracker data.";
                // Error loading dataset
                Log.e(LOGTAG, logMessage);
                vuforiaException = new ARApplicationException(ARApplicationException.LOADING_TRACKERS_FAILURE,
                        logMessage);
            } else
            {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                Vuforia.registerCallback(ARApplicationSession.this);

                mStarted = true;
            }

            // Done loading the tracker, update application status, send the
            // exception to check errors
            mSessionControl.onInitARDone(vuforiaException);
        }
    }

    // An async task to start the camera and trackers
    private class StartVuforiaTask extends AsyncTask<Void, Void, Boolean>
    {
        ARApplicationException vuforiaException = null;
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                try {
                    startCameraAndTrackers(mCamera);
                }
                catch (ARApplicationException e)
                {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + e);
                    vuforiaException = e;
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            Log.d(LOGTAG, "StartVuforiaTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            mSessionControl.onVuforiaStarted();

            if (vuforiaException != null)
            {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }


    // Returns the error message for each error code
    private String getInitializationErrorString(int code)
    {
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

    // Starts Vuforia, initialize and starts the camera and start the trackers
    private void startCameraAndTrackers(int camera) throws ARApplicationException
    {
        String error;
        if(mCameraRunning) {
            error = "Camera already running, unable to open again";
            Log.e(LOGTAG, error);
            throw new ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mCamera = camera;
        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mSessionControl.doStartTrackers();

        mCameraRunning = true;
    }

    public void startAR(int camera)
    {
        mCamera = camera;
        ARApplicationException vuforiaException = null;

        try {
            mStartVuforiaTask = new StartVuforiaTask();
            mStartVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Starting Vuforia failed";
            vuforiaException = new ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(vuforiaException);
        }
    }


    // Stops any ongoing initialization, stops Vuforia
    public void stopAR() throws ARApplicationException
    {
        // Cancel potentially running tasks
        if (mInitVuforiaTask != null
                && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED)
        {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }

        if (mLoadTrackerTask != null
                && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        mInitVuforiaTask = null;
        mLoadTrackerTask = null;

        mStarted = false;

        stopCamera();

        // Ensure that all asynchronous operations to initialize Vuforia
        // and loading the tracker datasets do not overlap:
        synchronized (mLifecycleLock)
        {

            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            // Destroy the tracking data set:
            unloadTrackersResult = mSessionControl.doUnloadTrackersData();

            // Deinitialize the trackers:
            deinitTrackersResult = mSessionControl.doDeinitTrackers();

            // Deinitialize Vuforia SDK:
            Vuforia.deinit();

            if (!unloadTrackersResult)
                throw new ARApplicationException(ARApplicationException.UNLOADING_TRACKERS_FAILURE, "Failed to unload trackers' data");

            if (!deinitTrackersResult)
                throw new ARApplicationException(ARApplicationException.TRACKERS_DEINITIALIZATION_FAILURE, "Failed to deinitialize trackers");

        }
    }


    // Resumes Vuforia, restarts the trackers and the camera
    private void resumeAR()
    {
        ARApplicationException vuforiaException = null;

        try {
            mResumeVuforiaTask = new ResumeVuforiaTask();
            mResumeVuforiaTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Resuming Vuforia failed";
            vuforiaException = new ARApplicationException(ARApplicationException.INITIALIZATION_FAILURE, logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null)
        {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(vuforiaException);
        }
    }


    // Pauses Vuforia and stops the camera
    public void pauseAR() throws ARApplicationException
    {
        if (mStarted)
        {
            stopCamera();
        }

        Vuforia.onPause();
    }

    public void stopCamera()
    {
        if (mCameraRunning)
        {
            mSessionControl.doStopTrackers();
            mCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }

    // Returns true if Vuforia is initialized, the trackers started and the
    // tracker data loaded
    private boolean isARRunning()
    {
        return mStarted;
    }
}