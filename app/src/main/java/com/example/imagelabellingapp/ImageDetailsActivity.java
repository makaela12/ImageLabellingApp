package com.example.imagelabellingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageDetailsActivity extends AppCompatActivity {


    //private ImageView imageView;
    private List<String> boundingBoxLabels = new ArrayList<>();
    private Spinner labelSpinner;
    private Button saveButton;
    private String imagePath;
    private long projectId;
    private String selectedLabel;
    private long imageId;
    private BoundingBoxImageView imageView;
    private float[] boundingBox = new float[4];

    private ImageButton recropButton;

    private Button addButton, deleteButton;

    DBHelper dbHelper;

    // TODO: when a user clicks on an item in the MainActivity2 ListView, need a way to allow the user to edit the label or recrop the image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Initialize UI elements
        //imageView = findViewById(R.id.imageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        saveButton = findViewById(R.id.saveButton);
        imageView = findViewById(R.id.imageView);
        dbHelper = new DBHelper(this);
        recropButton = findViewById(R.id.recropButton);
        addButton = findViewById(R.id.addButton);
        deleteButton = findViewById(R.id.deleteButton);

        // In ImageDetailsActivity onCreate
        int position = getIntent().getIntExtra("position", -1);

        // Retrieve image URI from the intent
        String imageUriString = getIntent().getStringExtra("imageUri");
        Uri imageUri = Uri.parse(imageUriString);

        // Retrieve image path from the Intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1); // -1 is the default value if not found

        // Retrieve imageId from the Intent
        imageId = getIntent().getLongExtra("imageId", -1);
        loadImageIntoImageView(imagePath);

        recropButton.setOnClickListener(v -> recropImage(imageUri));
        // Set up label spinner
        loadLabels();
        selectedLabel = labelSpinner.getSelectedItem().toString();

        // Inside onCreate method
        deleteButton.setOnClickListener(v -> deleteLastBoundingBox());
        // Set click listener for the saveButton
        saveButton.setOnClickListener(v -> saveImageDetails());

        // Set up touch listener for drawing bounding box on the image
        imageView.setOnTouchListener((v, event) -> {
            if (labelSpinner.getSelectedItem() != null) {
                String selectedLabel = labelSpinner.getSelectedItem().toString();
                if (!imageView.hasBoundingBox() || imageView.isAllowTouch()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                                // Handle touch down event
                                boundingBox = new float[4];
                                boundingBox[0] = event.getX();
                                boundingBox[1] = event.getY();
                                break;
                        case MotionEvent.ACTION_MOVE:
                            // Handle touch move event
                                boundingBox[2] = event.getX();
                                boundingBox[3] = event.getY();
                                imageView.drawBoundingBox(boundingBox, selectedLabel);
                                break;
                        case MotionEvent.ACTION_UP:
                                imageView.addBoundingBox(boundingBox, selectedLabel);
                                // Add the corresponding label to the list
                                boundingBoxLabels.add(selectedLabel);

                                // Disallow further touch events until "Add" button is pressed
                                imageView.setAllowTouch(false);
                                addButton.setEnabled(true);  // Enable the "Add" button
                                imageView.performClick();
                                // Handle touch up event

                            break;
                    }
                    return true;
                }
            } else {
                // Display a message to the user that a label must be selected
                Toast.makeText(ImageDetailsActivity.this, "Select a label from the spinner first", Toast.LENGTH_SHORT).show();
            }
            return false; // Ignore touch events
        });



        // Set up click listener for the "Add" button
        addButton.setOnClickListener(v -> {
            // Disable the "Add" button until the user draws a new bounding box
            addButton.setEnabled(false);

            // Get the selected label from the spinner
            String selectedLabel = labelSpinner.getSelectedItem().toString();

            // Check if a bounding box has been drawn
            if (imageView.hasBoundingBox()) {
                // Get the coordinates of the bounding box
                float[] boundingBox = imageView.getBoundingBox();

                // Save bounding box information to the bboxes table
               // dbHelper.insertBBoxInfo(imageId, selectedLabel, boundingBox);

                // Add the bounding box to BoundingBoxImageView
               // imageView.addBoundingBox(boundingBox,selectedLabel);


                // Allow touch events on the image view
                imageView.setAllowTouch(true);
                } else {
                     // Handle the case where no bounding box is drawn
                     Toast.makeText(this, "Draw a bounding box first", Toast.LENGTH_SHORT).show();
                }

        });
    }

    private void deleteLastBoundingBox() {
            // Remove the last drawn bounding box
            imageView.removeLastBoundingBox();
            // Enable the "Add" button
            addButton.setEnabled(false);
            imageView.setAllowTouch(true);

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

        // Save all bounding boxes information to the bboxes table
        dbHelper.insertBoundingBoxes(imageId,boundingBoxLabels, imageView.getBoundingBoxes());
        // Save bounding box information to the bboxes table
        //dbHelper.insertBBoxInfo(imageId, selectedLabel, boundingBox);

        dbHelper.getProjectName(projectId);

        // Show a toast message indicating success
        Toast.makeText(this, "New image saved successfully to \"" + dbHelper.getProjectName(projectId) + "\"", Toast.LENGTH_SHORT).show();

        // Save and finish the activity
        saveAndFinish();
    }

    private void saveAndFinish() {
        // Set the result and toast message
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedLabel", selectedLabel);
        resultIntent.putExtra("imageId", imageId);
        setResult(RESULT_OK, resultIntent);

        // Finish the activity
        finish();
    }

    private void recropImage(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                // Get the cropped image URI
                Uri croppedUri = result.getUri();

                // Now you can use the croppedUri to get the Bitmap or perform any further operations
                Bitmap croppedBitmap = null;
                try {
                    croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Save the cropped image to a file in the cache directory
                File cacheDir = getCacheDir();
                String timestamp = String.valueOf(System.currentTimeMillis());
                String filename = "image_" + timestamp + ".jpg";
                File imageFile = new File(cacheDir, filename);

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(imageFile);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                try {
                    fos.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();

                //  imagePath to update the database
                dbHelper.updateCroppedImagePath(imageId, imagePath);
                // Reload the image into the ImageView
                loadImageIntoImageView(imagePath);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                // Handle cropping error
                Exception error = result.getError();
                error.printStackTrace();
            }
        }
    }

}