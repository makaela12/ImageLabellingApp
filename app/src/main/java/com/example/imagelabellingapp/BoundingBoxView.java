package com.example.imagelabellingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;

public class BoundingBoxView extends View {
    private List<Rect> boundingBoxes;

    public BoundingBoxView(Context context) {
        super(context);
        init();
    }

    public BoundingBoxView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoundingBoxView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialization, if needed
    }

    public void setBoundingBoxes(List<Rect> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
        invalidate(); // Trigger a redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boundingBoxes != null) {
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            for (Rect boundingBox : boundingBoxes) {
                canvas.drawRect(boundingBox, paint);
            }
        }
    }
}
