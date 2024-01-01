package com.example.imagelabellingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

// LabelImageActivity.java
public class LabelImageActivity extends AppCompatActivity {

    private ImageView croppedImageView;
    private Spinner labelSpinner;
    private Button labelButton;

    private long projectId; // Project ID passed from MainActivity2
    private String imagePath; // File path of the cropped image

    // ... (other imports and class declaration)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_image);

        croppedImageView = findViewById(R.id.croppedImageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        labelButton = findViewById(R.id.labelButton);

        // Retrieve project ID and image path from the Intent
        projectId = getIntent().getLongExtra("projectId", -1);
        imagePath = getIntent().getStringExtra("imagePath");

        // Load labels for the project and populate the Spinner
        loadLabels(projectId);


        // Set the cropped image to the ImageView
        Bitmap croppedBitmap = BitmapFactory.decodeFile(imagePath);
        croppedImageView.setImageBitmap(croppedBitmap);

        // Set a click listener for the "Label" button
        labelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected label from the Spinner
                String selectedLabel = labelSpinner.getSelectedItem().toString();

                // Save the image and its corresponding label to the "Images" table
                saveImageAndLabel(projectId, imagePath, selectedLabel);

                // You may want to navigate back to MainActivity2 or perform other actions
                // ...

                // Finish the current activity
                finish();
            }
        });
    }

    private void loadLabels(long projectId) {
        // ... (existing code to load labels into the Spinner)
    }

    private void saveImageAndLabel(long projectId, String imagePath, String label) {
        // ... (existing code to save image and label to the "Images" table)
    }
}
