package com.example.imagelabellingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.List;

public class ImageDetailsActivity extends AppCompatActivity {


    private ImageView imageView;
    private Spinner labelSpinner;
    private Button saveButton;
    private String imagePath;
    private long projectId;
    private String selectedLabel;
    private long imageId;

    DBHelper dbHelper;

    // TODO: when a user clicks on an item in the MainActivity2 ListView, need a way to allow the user to edit the label or recrop the image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        saveButton = findViewById(R.id.saveButton);
        dbHelper = new DBHelper(this);
        // In ImageDetailsActivity onCreate
        int position = getIntent().getIntExtra("position", -1);


        // Retrieve image path from the Intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1); // -1 is the default value if not found

        // Retrieve imageId from the Intent
        imageId = getIntent().getLongExtra("imageId", -1);
        loadImageIntoImageView(imagePath);

        // Set up label spinner
        loadLabels();

        // Get the selected label from the intent
        Intent intent = getIntent();
        if (intent.hasExtra("selectedLabel")) {
            selectedLabel = intent.getStringExtra("selectedLabel");
            Log.d("ImageDetailsActivity", "Selected label " + selectedLabel);
        } else {
            Log.e("ImageDetailsActivity", "Selected label is null");
        }

        // Set click listener for the saveButton
        saveButton.setOnClickListener(v -> saveImageDetails());
    }
    private void loadImageIntoImageView(String imagePath) {
        // Remove the leading "/file:" from the imagePath
        if (imagePath.startsWith("file://")) {
            imagePath = imagePath.substring(6);
        }

        // Create a File object from the imagePath
        File imageFile = new File(imagePath);


        // Log the file path to check if it's correct
        Log.d("ImageDetailsActivity", "Complete File Path: " + imageFile.getAbsolutePath());


        if (imageFile.exists()) {
            // Load the image with Glide
            Glide.with(this)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView);
        } else {
            // Log an error or handle the missing file condition
            Log.e("Glide", "File does not exist: " + imageFile.getAbsolutePath());
            // You might want to set a placeholder image or handle the missing file condition in another way
            // For now, we'll clear the ImageView
            imageView.setImageDrawable(null);
        }
    }

    private void loadLabels() {
        // Retrieve the available labels for the selected project
        List<String> labels = dbHelper.getLabelsForProject(projectId);

        // Create an ArrayAdapter for the spinner
        ArrayAdapter<String> labelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the ArrayAdapter as the adapter for the Spinner
        labelSpinner.setAdapter(labelAdapter);
    }

    private void saveImageDetails() {
        // Get selected label from the spinner
        selectedLabel = labelSpinner.getSelectedItem().toString();
        Log.d("ImageDetailsActivity", "Selected label " + selectedLabel);
        dbHelper.updateLabelForImage(imageId, selectedLabel);

        // Prepare the result intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedLabel", selectedLabel);
        // Include the imageId in the result intent
        resultIntent.putExtra("imageId", imageId);

        // Finish the activity and return to MainActivity2
        setResult(RESULT_OK, resultIntent);
        finish();
    }


}