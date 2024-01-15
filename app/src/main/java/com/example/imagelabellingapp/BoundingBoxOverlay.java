package com.example.imagelabellingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BoundingBoxOverlay extends View {
    private float xMin, yMin, xMax, yMax;
    private Paint paint;

    public BoundingBoxOverlay(Context context) {
        super(context);
        init();
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    public void setBoundingBox(float xMin, float yMin, float xMax, float yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
        invalidate(); // Trigger a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the bounding box on the canvas
        canvas.drawRect(xMin, yMin, xMax, yMax, paint);
    }
}
