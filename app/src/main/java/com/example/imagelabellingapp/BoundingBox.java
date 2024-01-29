package com.example.imagelabellingapp;

public class BoundingBox {
    private float[] coordinates; // [left, top, right, bottom]
    private String label;
    private float left;
    private float top;
    private float right;
    private float bottom;
    private long id;
    private long labelId;

    private int color;




    public BoundingBox(float[] coordinates, String label, long id, long labelId, int color) {
        this.coordinates = coordinates;
        this.label = label;
        this.id = id;
        this.labelId = labelId;
        this.color = color;


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

    public void setCoordinates(float[] coordinates) {
        if (coordinates != null && coordinates.length == 4) {
            this.left = coordinates[0];
            this.top = coordinates[1];
            this.right = coordinates[2];
            this.bottom = coordinates[3];
        }
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public long getId() {
        return id;
    }

    public long getLabelId() {
        return labelId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

}
