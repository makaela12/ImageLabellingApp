package com.example.imagelabellingapp;

public class BoundingBox {
    private float[] coordinates; // [left, top, right, bottom]
    private String label;
    private boolean highlighted;


    public BoundingBox(float[] coordinates, String label) {
        this.coordinates = coordinates;
        this.label = label;
        this.highlighted = false; // Initialize as not highlighted

    }

    public float getLeft() {
        return coordinates[0];
    }

    public float getTop() {
        return coordinates[1];
    }

    public float getRight() {
        return coordinates[2];
    }

    public float getBottom() {
        return coordinates[3];
    }
    public String getLabel() {
        return label;
    }
    public boolean isHighlighted() {
        return highlighted;
    }
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
}
