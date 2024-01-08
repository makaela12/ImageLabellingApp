package com.example.imagelabellingapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.List;

public class EditImage extends AppCompatActivity {

    private static final int CROP_IMAGE_REQUEST_CODE = 203; // Define a custom request code
    private ImageView imageView;
    private String originalImagePath;
    private String imagePath;
    private DBHelper dbHelper;
    private long projectId;
    private Spinner labelSpinner;
    private Button saveButton;
    private long imageId;
    private String newLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        saveButton = findViewById(R.id.saveButton);
        dbHelper = new DBHelper(this);

        // retrieve extras from intent
        imagePath = getIntent().getStringExtra("imagePath");
        originalImagePath = getIntent().getStringExtra("originalImagePath");
        projectId = getIntent().getLongExtra("projectId", -1); // -1 is the default value if not found

        // get original_image_path id
        //imageId = dbHelper.getImageIdFromOPath(originalImagePath);
        imageId = dbHelper.getImageIdFromPath(imagePath);

        // Populate spinner with labels associated with the project_id
        List<String> labels = dbHelper.getLabelsForProject(projectId);
        ArrayAdapter<String> labelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        labelSpinner.setAdapter(labelAdapter);

        // Set the selected label for the current image
        String currentLabel = dbHelper.getCurrentLabelForImage(imageId);
        int selectedLabelPosition = labels.indexOf(currentLabel);
        labelSpinner.setSelection(selectedLabelPosition);

        // Load the cropped image into the ImageView
        loadImageIntoImageView(imagePath);

        Log.d("EditImage", "ImageId = " + imageId);
        Log.d("EditImage", "project_id " + projectId);
        Log.d("EditImage", "Original Image Path: " + originalImagePath);
        Log.d("EditImage", "Cropped Image Path: " + imagePath);

        // Set click listener for the ImageView
        imageView.setOnClickListener(v -> startCroppingActivity(originalImagePath));

        // set click listener for the save button
        saveButton.setOnClickListener(v -> {
            // Get the selected label from the spinner
            newLabel = labelSpinner.getSelectedItem().toString();
            Log.d("EditImage", "New label = " + newLabel);

            // Update the label_name in the database for the current image
            dbHelper.updateLabelForImage(imageId, newLabel); // Implement this method in your DBHelper

            Intent labelChangedIntent = new Intent("label_changed");
            labelChangedIntent.putExtra("imageId", imageId);
            labelChangedIntent.putExtra("newLabel", newLabel);
            sendBroadcast(labelChangedIntent);

            // Navigate back to MainActivity2
            //Intent intent = new Intent(EditImage.this, MainActivity2.class);
            //startActivity(intent);
            finish(); // Close the current activity
        });
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
            imageView.setImageDrawable(null);
        }
    }

    private void startCroppingActivity(String originalImagePath) {
        if (originalImagePath != null && !originalImagePath.isEmpty()) {
            Log.d("EditImage", "Starting cropping activity with Original Image Path: " + originalImagePath);

            //Uri uri = Uri.parse(originalImagePath);
            Uri uri = Uri.fromFile(new File(originalImagePath));
            Log.d("EditImage", "Uri for cropping activity: " + uri);

            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        } else {
            // Handle the case where originalImagePath is null or empty
            Log.e("EditImage", "Invalid originalImagePath");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("EditImage", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        // Handle the result from the cropping activity
        if (requestCode == CROP_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the result of the cropping activity
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            // Check if the cropping was successful
            if (result != null) {
                if (result.isSuccessful()) {
                    // Handle the cropped image result
                    Uri croppedImageUri = result.getUri();
                    // Convert the URI to the actual path
                    String croppedImagePath = getRealPathFromURI(croppedImageUri);
                    Log.d("EditImage", "Cropped Image Path!!!???: " + croppedImagePath);

                    if (croppedImagePath != null) {
                        // Get the corresponding imageId from the database
                        imageId = dbHelper.getImageIdFromOPath(originalImagePath);

                        if (imageId != -1) {
                            // Update the database with the cropped image path
                            dbHelper.updateCroppedImagePath(imageId, croppedImagePath);
                            Log.d("EditImage", "Cropped Image Path: " + croppedImagePath);
                            // Load the cropped image into the ImageView
                            loadImageIntoImageView(croppedImagePath.toString());
                        } else {
                            Log.e("ImageEditingActivity", "ImageId not found for path: " + croppedImagePath);
                        }
                    } else {
                        Log.e("ImageEditingActivity", "Cropped image path is null");
                    }


                    // Get the corresponding imageId from the database
                    //  long imageId = dbHelper.getImageIdFromOPath(croppedImagePath);
                    // Update the database with the cropped image path
                    // dbHelper.updateCroppedImagePath(imageId, croppedImagePath);
                    // Load the cropped image into the ImageView
                    //  loadImageIntoImageView(croppedImagePath.toString());
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    // Handle cropping error
                    Exception error = result.getError();
                    if (error != null) {
                        error.printStackTrace();
                    }

                } else {
                    // Handle the case where CropImage result is null
                    Log.e("ImageEditingActivity", "CropImage result is null");
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri == null) {
            Log.e("EditImage", "Content URI is null");
            return null;
        }

        String scheme = contentUri.getScheme();
        Log.d("EditImage", "URI Scheme: " + scheme);

        if ("file".equals(scheme)) {
            // Handle file scheme directly
            return contentUri.getPath();
        } else if ("content".equals(scheme)) {
            // Handle content scheme using content resolver
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(contentUri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String filePath = cursor.getString(column_index);
                    Log.d("EditImage", "Real Path from URI: " + filePath);
                    return filePath;
                } else {
                    Log.e("EditImage", "Cursor is null or empty while retrieving real path from URI");
                }
            } catch (Exception e) {
                Log.e("EditImage", "Error retrieving real path from URI: " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            Log.e("EditImage", "Unsupported URI scheme: " + scheme);
        }

        return null;
    }
}

