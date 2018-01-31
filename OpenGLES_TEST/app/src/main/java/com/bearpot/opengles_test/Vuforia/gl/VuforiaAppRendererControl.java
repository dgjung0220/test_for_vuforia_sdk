package com.bearpot.opengles_test.Vuforia.gl;

/**
 * Created by dg.jung on 2018-01-31.
 */
import com.vuforia.State;

public interface VuforiaAppRendererControl {
    void renderFrame(State state, float[] projectionMatrix);
}
