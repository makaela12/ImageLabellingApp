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

    DBHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        saveButton = findViewById(R.id.saveButton);
        dbHelper = new DBHelper(this);


        // Retrieve image path from the Intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1); // -1 is the default value if not found

        loadImageIntoImageView(imagePath);

        // Set up label spinner
        loadLabels();

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

/*
    private void loadImageIntoImageView(String imagePath) {
        // TODO: Use an image loading library like Glide or Picasso for efficient image loading
        // Log the image path to check if it's correct
       // Log.d("ImageDetailsActivity", "Image Path: " + imagePath);
        // Example with Glide:
        //Glide.with(this).load(new File(imagePath)).into(imageView);

        // **** new code *****
        // Remove the leading "/file:" from the imagePath
        if (imagePath.startsWith("/file:")) {
            imagePath = imagePath.substring(6);
        }

        // Prepend "file://" to create a valid URI
        String imageUri = "file://" + imagePath;

        // Create a File object from the imagePath
        File imageFile = new File(imagePath);


        // Extract the filename from the imagePath
      //  String filename = new File(imagePath).getName(); ***** Old

        // Use the correct filename to load the image ***** old
      //  File filesDir = getFilesDir();
        //File imageFile = new File(filesDir, filename);


        Log.d("Glide", "Original Image Path: " + imagePath);
   //     Log.d("Glide", "File Name: " + filename);
        Log.d("Glide", "Complete File Path: " + imageFile.getAbsolutePath());


        // old *******
        // Create a File object from the imagePath
        //File imageFile = new File(imagePath);

        if (imageFile.exists()) {
            // Load the image with Glide
           // Glide.with(this).load(imageFile).into(imageView);
            Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable disk caching
                    .skipMemoryCache(true) // Disable memory caching
                    .into(imageView);
        } else {
            // Log an error or handle the missing file condition
            Log.e("Glide", "File does not exist: " + imageFile.getAbsolutePath());
            // You might want to set a placeholder image or handle the missing file condition in another way
            // For now, we'll clear the ImageView
            imageView.setImageDrawable(null);
        }
    }*/

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
        // TODO: Implement logic to save image details (e.g., selected label) to the database.

        // Get selected label from the spinner
        String selectedLabel = labelSpinner.getSelectedItem().toString();

        // Save image details to the database using dbHelper
        dbHelper.saveImageDetails(imagePath, projectId, selectedLabel);

       // Send broadcast to notify MainActivity2 about the new image
        Intent intent = new Intent("new_image_saved");
        sendBroadcast(intent);

        // Finish the activity and return to MainActivity2
        finish();
    }


}