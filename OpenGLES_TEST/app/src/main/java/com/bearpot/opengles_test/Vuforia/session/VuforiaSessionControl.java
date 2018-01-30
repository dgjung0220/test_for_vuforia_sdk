package com.bearpot.opengles_test.Vuforia.session;

import com.bearpot.opengles_test.Vuforia.Exception.VuforiaAppException;
import com.vuforia.State;

/**
 * Created by dg.jung on 2018-01-30.
 */

public interface VuforiaSessionControl {
    // To be called to initialize the trackers
    boolean doInitTrackers();

    // To be called to load the trackers' data
    boolean doLoadTrackersData();

    // To be called to start tracking with the initialized trackers and their
    // loaded data
    boolean doStartTrackers();


    // To be called to stop the trackers
    boolean doStopTrackers();


    // To be called to destroy the trackers' data
    boolean doUnloadTrackersData();


    // To be called to deinitialize the trackers
    boolean doDeinitTrackers();


    // This callback is called after the Vuforia initialization is complete,
    // the trackers are initialized, their data loaded and
    // tracking is ready to start
    void onInitARDone(VuforiaAppException e);


    // This callback is called every cycle
    void onVuforiaUpdate(State state);


    // This callback is called on Vuforia resume
    void onVuforiaResumed();


    // This callback is called once Vuforia has been started
    void onVuforiaStarted();
}
