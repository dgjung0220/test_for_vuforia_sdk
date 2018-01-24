package com.bearpot.artest.app.MultiTargets;

/**
 * Created by dg.jung on 2018-01-24.
 */

public class TextObject {

    public String text;
    public float x;
    public float y;
    public float[] color;

    public TextObject() {
        text = "default";
        x = 0f;
        y = 0f;
        color = new float[] {1f, 1f, 1f, 1.0f};
    }

    public TextObject(String text, float xcoord, float ycoord) {
        this.text = text;
        this.x = xcoord;
        this.y = ycoord;
        this.color = new float[]{1f, 1f, 1f, 1.0f};
    }
}
