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

    private boolean allowTouch = true;

    private long projectId;

    private int color;

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
               // int labelColor = getLabelColor(label,projectId); // Get color based on label
                //int labelColor = dbHelper.getBoundingBoxColor(label,projectId,left,top,right,bottom);
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);
                int alpha = Color.alpha(color);
                int colour = Color.argb(alpha,red,green,blue);
                //backgroundPaint.setColor(color);
                backgroundPaint.setColor(colour);
                //backgroundPaint.setColor(Color.RED); // Set your desired background color
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

    // Helper method to get color based on the order of the label for a project
    private int getLabelColor(String label, long projectId) {
        // Get the list of labels for the project from the database
        List<String> projectLabels = dbHelper.getLabelsForProject(projectId);

        // Find the index of the label in the projectLabels list
        int labelIndex = projectLabels.indexOf(label);
        Log.d("BOUNDINGBOXIMAGEVIEW TEST", "getLabelColor: INDEX ="+labelIndex +"project labels" + projectLabels);

        // Assign colors based on the index
        switch (labelIndex) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.GREEN;
            case 2:
                return Color.RED;
            // Add more cases as needed
            default:
                // Use a default color for labels beyond the defined cases
                return Color.YELLOW;
        }
    }

}


