package com.example.imagelabellingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxImageView extends AppCompatImageView {
    // Paint object for drawing the bounding box
    private Paint paint;

    private String label;

    private DBHelper dbHelper;

    private boolean allowTouch = true;

    private long projectId;

    private int color;

    // List to store bounding boxes
    private List<BoundingBox> boundingBoxes;

    // Array to store bounding box coordinates [left, top, right, bottom]
    private float[] boundingBox;

    // Constructors and initialization method
    public BoundingBoxImageView(Context context) {
        super(context);
        dbHelper = new DBHelper(context);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dbHelper = new DBHelper(context);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dbHelper = new DBHelper(context);
        init();
    }

    public boolean isAllowTouch() {
        return allowTouch;
    }

    private void init() {
        boundingBoxes = new ArrayList<>();
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    // Method to check if a bounding box is currently drawn
    public boolean hasBoundingBox() {
        return boundingBox != null && boundingBoxes.size() != 0;
    }


    // method to draw the bounding box on the canvas.
    public void drawBoundingBox(float[] boundingBox, String label, long projectId, int color) {
        Log.d("BoundingBoxIV", "drawBoundingBox: label:" + label);
        this.boundingBox = boundingBox;
        this.label = label;
        this.projectId = projectId;
        this.color = color;
        invalidate();
    }

    // method to handle drawing the canvas and bounding box
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the existing bounding boxes
        for (BoundingBox boundingBox : boundingBoxes) {
            float[] coordinates = new float[]{boundingBox.getLeft(), boundingBox.getTop(), boundingBox.getRight(), boundingBox.getBottom()};

            int color = boundingBox.getColor();
            String label = boundingBox.getLabel();
            Log.d("BoundingBoxIV", "onDraw: label:" + label);
            drawLabel(canvas,coordinates,label,color);
        }
    }

    @Override
    public boolean performClick() {
        // method to satisfy accessibility requirements
        return super.performClick();
    }

    // Method to add a new bounding box with a specific color
    public void addBoundingBox(float[] coordinates, String label, long bbox_id, long label_id, int color) {
        Log.d("BoundingBoxIV", "addBoundingBox: label added:" + label);
       // int color = dbHelper.getBoundingBoxColor(label,projectId,coordinates[0],coordinates[1],coordinates[2],coordinates[3]);
       // long bbox_id = dbHelper.insertBoundingBox(imageId, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], label);
        BoundingBox newBoundingBox = new BoundingBox(coordinates,label,bbox_id, label_id,color);
        boundingBoxes.add(newBoundingBox);
        Log.d("BoundingBoxIV", "addBoundingBox: number of bounding boxes =" +  boundingBoxes.size());
        invalidate(); // Redraw the view
    }


    // Helper method to draw the label on the canvas
    private void drawLabel(Canvas canvas, float[] coordinates, String label, int color) {
        if (coordinates != null && coordinates.length == 4) {
            float left = coordinates[0];
            float top = coordinates[1];
            float right = coordinates[2];
            float bottom = coordinates[3];

            // Calculate the position to draw the label (adjust the values as needed)
            float x = left;
            float y = top - 20; // Adjust the vertical position

            if (label != null) {

                // Draw the label text
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE); // Set your desired color
                // paint.setColor(highlighted ? Color.YELLOW : Color.RED); // Change color if highlighted
                textPaint.setTextSize(30); // Set your desired text size
                canvas.drawText(label, x, y, textPaint);

                // Draw the background rectangle
                Paint backgroundPaint = new Paint();
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);
                int alpha = Color.alpha(color);
                int colour = Color.argb(alpha,red,green,blue);
                backgroundPaint.setColor(colour);
                float textWidth = textPaint.measureText(label);
                float textHeight = 40;

                // Adjust the rectangle position and size as needed
                float rectLeft = x - 6;
                float rectTop = y - textHeight;
                float rectRight = x + textWidth + 8;
                float rectBottom = y + 17;

                canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, backgroundPaint);

                // Draw the label text
                Paint textPaint2 = new Paint();
                backgroundPaint.setColor(colour);
                textPaint2.setColor(Color.WHITE); // Set your desired color
               // paint.setColor(highlighted ? Color.YELLOW : Color.RED); // Change color if highlighted
                textPaint2.setTextSize(30); // Set your desired text size
                canvas.drawText(label, x, y, textPaint2);


                // Draw the bounding box
                Paint paint = new Paint();
                paint.setColor(colour);
               // paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8);
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }

    // Inside BoundingBoxImageView class
    public void removeLastBoundingBox() {
        if (!boundingBoxes.isEmpty()) {
            Log.d("BoundingBoxList", "Before Deletion: " + boundingBoxes);
            boundingBoxes.remove(boundingBoxes.size()-1);
            Log.d("BoundingBoxList", "After Deletion: " + boundingBoxes);
            invalidate(); // Redraw the view after removing the bounding box
        }
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
        invalidate(); // Redraw the canvas
    }


}


