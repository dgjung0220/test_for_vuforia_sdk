package com.bearpot.opengles_test.Vuforia.session;

/**
 * Created by dg.jung on 2018-01-30.
 */
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.bearpot.opengles_test.Vuforia.Exception.VuforiaAppException;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;

public class VuforiaSession implements UpdateCallbackInterface {

    private static final String LOGTAG = "Vuforia Session";

    private Activity mActivity;
    private VuforiaSessionControl sessionControl;

    private boolean mStarted = false;
    private boolean mCameraRunning = false;
    private final Object mLifeCycleLock = new Object();
    private int mVuforiaFlags = 0;

    private InitVuforiaTask mInitVuforiaTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;

    @Override
    public void Vuforia_onUpdate(State state) {

    }

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
    }

    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {

            synchronized (mLifeCycleLock) {
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
            synchronized (mLifeCycleLock) {
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

}
