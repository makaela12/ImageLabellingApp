package com.example.imagelabellingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxImageView extends AppCompatImageView {
    // Paint object for drawing the bounding box
    private Paint paint;

    private String label;

    private DBHelper dbHelper;

    // Flag to indicate whether the user is in highlight mode
    private boolean highlightMode = false;

    private BoundingBox highlightedBoundingBox;


    private boolean allowTouch = true;


    // List to store bounding boxes
    private List<BoundingBox> boundingBoxes;

    // Array to store bounding box coordinates [left, top, right, bottom]
    private float[] boundingBox;

    // Last touch coordinates
    private float lastTouchX, lastTouchY;

    // Listener for notifying about bounding box changes
    private BoundingBoxListener boundingBoxListener;

    // Flags for tracking whether the user is moving or resizing the bounding box
    private boolean isMoving = false;
    private boolean isResizing = false;

    // Active pointer id for tracking touch events
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    // Callback interface for saving bounding box coordinates
    public interface BoundingBoxListener {
        void onSaveBoundingBox(float xMin, float yMin, float xMax, float yMax);
    }

    // Constructors and initialization method
    public BoundingBoxImageView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoundingBoxImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    // Setter method to toggle highlight mode
    public void setHighlightMode(boolean highlightMode) {
        this.highlightMode = highlightMode;
    }

    // Method to set whether touch events are allowed
    public void setAllowTouch(boolean allowTouch) {
        this.allowTouch = allowTouch;
    }
    // Method to check whether touch events are allowed
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

    // Method to get the coordinates of the current bounding box
    public float[] getBoundingBox() {
        return boundingBox;
    }

    // Method to clear the current bounding box
    public void clearBoundingBox() {
        boundingBox = null;
        invalidate(); // Redraw the view
    }

    // Sets the bounding box listener to receive updates when the bounding box changes.
    public void setBoundingBoxListener(BoundingBoxListener listener) {
        this.boundingBoxListener = listener;
    }

    // method to draw the bounding box on the canvas.
    public void drawBoundingBox(float[] boundingBox, String label) {
        this.boundingBox = boundingBox;
        this.label = label;
        invalidate();
    }

    // method to handle drawing the canvas and bounding box
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the existing bounding boxes
        for (BoundingBox boundingBox : boundingBoxes) {
            float[] coordinates = new float[]{boundingBox.getLeft(), boundingBox.getTop(), boundingBox.getRight(), boundingBox.getBottom()};

            drawLabel(canvas,coordinates,boundingBox.getLabel(),boundingBox.isHighlighted());
        }

    }

    @Override
    public boolean performClick() {
        // method to satisfy accessibility requirements
        return super.performClick();
    }

    // Handles touch events on the view, allowing users to move or resize the bounding box. returns true if event was hnadled
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Handle touch down event
                activePointerId = event.getPointerId(0);
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isMoving = isTouchInsideBoundingBox(lastTouchX, lastTouchY);
                isResizing = !isMoving && isTouchNearBoundingBox(lastTouchX, lastTouchY);
                // Check if touch is inside any bounding box center
                highlightedBoundingBox = getClickedBoundingBox(lastTouchX, lastTouchY);
                if (highlightedBoundingBox != null) {
                    // Highlight the bounding box (e.g., change its color)
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // Handle touch move event
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        float newX = event.getX(pointerIndex);
                        float newY = event.getY(pointerIndex);

                        float deltaX = newX - lastTouchX;
                        float deltaY = newY - lastTouchY;

                        if (isMoving) {
                            // Move the bounding box if touched inside
                            moveBoundingBox(deltaX, deltaY);
                        } else if (isResizing) {
                            // Resize the bounding box if touched outside
                            resizeBoundingBox(newX, newY);
                        }

                        lastTouchX = newX;
                        lastTouchY = newY;

                        invalidate(); // Redraw the view
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Handle touch up or cancel event
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                isMoving = false;
                isResizing = false;
                break;
        }
        return true;
    }

    // checks if the touch event is near the bounding box, indicating the user would like to resize the bounding box
    // returns true if the touch event is near the bounding box
    private boolean isTouchNearBoundingBox(float x, float y) {
        if (boundingBox != null && boundingBox.length == 4) {
            float left = boundingBox[0];
            float top = boundingBox[1];
            float right = boundingBox[2];
            float bottom = boundingBox[3];
            // defines a margin for resizing
            float margin = 20;

            return x >= left - margin && x <= right + margin &&
                    y >= top - margin && y <= bottom + margin &&
                    !isTouchInsideBoundingBox(x, y);
        }
        return false;
    }

    // Checks if the touch event is inside the bounding box, indicating the user would like to move the bounding box
    // returns true if the touch event is inside the bounding box.
    private boolean isTouchInsideBoundingBox(float x, float y) {
        // Check if the touch event is inside the bounding box
        return boundingBox != null && boundingBox.length == 4 &&
                x >= boundingBox[0] && x <= boundingBox[2] &&
                y >= boundingBox[1] && y <= boundingBox[3];
    }

    // Moves the bounding box based on the user's touch movement.
    private void moveBoundingBox(float deltaX, float deltaY) {
        // Move bounding box coordinates based on user's movement
        if (boundingBox != null && boundingBox.length == 4) {
            boundingBox[0] += deltaX; // Update left
            boundingBox[1] += deltaY; // Update top
            boundingBox[2] += deltaX; // Update right
            boundingBox[3] += deltaY; // Update bottom
        }
    }

    // Resizes the bounding box based on the user's touch movement.
    private void resizeBoundingBox(float newX, float newY) {
        // Resize bounding box based on user's movement
        if (boundingBox != null && boundingBox.length == 4) {
            // Determine the closest edge or corner
            float left = boundingBox[0];
            float top = boundingBox[1];
            float right = boundingBox[2];
            float bottom = boundingBox[3];

            float closestX = Math.min(Math.abs(left - newX), Math.abs(right - newX));
            float closestY = Math.min(Math.abs(top - newY), Math.abs(bottom - newY));

            if (closestX < closestY) {
                // Resize horizontally
                if (newX < (left + right) / 2) {
                    boundingBox[0] = newX; // Update left
                } else {
                    boundingBox[2] = newX; // Update right
                }
            } else {
                // Resize vertically
                if (newY < (top + bottom) / 2) {
                    boundingBox[1] = newY; // Update top
                } else {
                    boundingBox[3] = newY; // Update bottom
                }
            }
        }
    }

    // Saves the final bounding box coordinates by notifying the listener.
    public void saveBoundingBox() {
        if (boundingBoxListener != null && boundingBox != null && boundingBox.length == 4) {
            float xMin = boundingBox[0];
            float yMin = boundingBox[1];
            float xMax = boundingBox[2];
            float yMax = boundingBox[3];

            // Notify the listener to save the bounding box coordinates
            boundingBoxListener.onSaveBoundingBox(xMin, yMin, xMax, yMax);
        }
    }

    // Method to add a new bounding box with a specific color
    public void addBoundingBox(float[] coordinates,String label) {
        BoundingBox newBoundingBox = new BoundingBox(coordinates,label);
        boundingBoxes.add(newBoundingBox);
        Log.d("BoundingBoxIV", "addBoundingBox: number of bounding boxes =" +  boundingBoxes.size());
        invalidate(); // Redraw the view
    }


    // Helper method to draw the label on the canvas
    private void drawLabel(Canvas canvas,float[] coordinates, String label, Boolean highlighted) {
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
                textPaint.setColor(Color.RED); // Set your desired color
               // paint.setColor(highlighted ? Color.YELLOW : Color.RED); // Change color if highlighted
                textPaint.setTextSize(30); // Set your desired text size
                canvas.drawText(label, x, y, textPaint);

                // Draw the bounding box
                Paint paint = new Paint();
                paint.setColor(highlighted ? Color.BLUE : Color.RED); // Change color if highlighted
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }

    // Add a method to get the clicked bounding box
    private BoundingBox getClickedBoundingBox(float x, float y) {
        for (BoundingBox boundingBox : boundingBoxes) {
            float centerX = (boundingBox.getLeft() + boundingBox.getRight()) / 2;
            float centerY = (boundingBox.getTop() + boundingBox.getBottom()) / 2;

            // Define a click radius around the center
            float clickRadius = 50; // Adjust as needed

            // Check if the click is near the center of the bounding box
            if (Math.abs(x - centerX) <= clickRadius && Math.abs(y - centerY) <= clickRadius) {
                return boundingBox;
            }
        }
        return null;
    }

    // Add a method to delete the highlighted bounding box
    public void deleteHighlightedBoundingBox() {
        if (highlightedBoundingBox != null) {
            // Remove from the list
            boundingBoxes.remove(highlightedBoundingBox);
            // Remove from the database
            dbHelper.deleteBoundingBox(highlightedBoundingBox); // You need to implement this method in your DBHelper
            // Reset highlightedBoundingBox
            highlightedBoundingBox = null;
            // Redraw the view
            invalidate();
        }
    }

    // Method to find the highlighted bounding box at a given touch location
    BoundingBox getHighlightedBoundingBox(float x, float y) {
        for (BoundingBox boundingBox : boundingBoxes) {
            if (isTouchInsideBoundingBox(x, y, boundingBox)) {
                return boundingBox;
            }
        }
        return null;
    }

    // Method to check if the touch event is inside a bounding box
    private boolean isTouchInsideBoundingBox(float x, float y, BoundingBox boundingBox) {
        return x >= boundingBox.getLeft() && x <= boundingBox.getRight() &&
                y >= boundingBox.getTop() && y <= boundingBox.getBottom();
    }

    // Method to highlight a bounding box
    public void highlightBoundingBox(BoundingBox boundingBox) {
        boundingBox.setHighlighted(true);
        invalidate(); // Redraw the view to reflect the highlighting change
    }

}


