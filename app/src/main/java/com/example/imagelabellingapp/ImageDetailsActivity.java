package com.example.imagelabellingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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

    private ImageButton recropButton, deleteButton;


    DBHelper dbHelper;

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
        deleteButton = findViewById(R.id.deleteButton);

        // Retrieve image URI from the intent
        String imageUriString = getIntent().getStringExtra("imageUri");
        Uri imageUri = Uri.parse(imageUriString);

        // Retrieve image path, project_id and imageId from the Intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1);
        imageId = getIntent().getLongExtra("imageId", -1);
        loadImageIntoImageView(imagePath);

        String og_image_path = dbHelper.getOriginalImagePath(imagePath);

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
                int color = getLabelColor(selectedLabel, projectId);
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
                                imageView.drawBoundingBox(boundingBox, selectedLabel, projectId, color);
                                break;
                        case MotionEvent.ACTION_UP:
                                long lid = dbHelper.getLabelIdForProjectAndLabel(projectId,selectedLabel);
                                long bbox_id = dbHelper.insertBoundingBox(imageId, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], selectedLabel, projectId,lid, color);
                                long label_id = dbHelper.getLabelIdForBoundingBox(selectedLabel,bbox_id);
                                imageView.addBoundingBox(boundingBox, selectedLabel,bbox_id,label_id,color);
                                // Add the corresponding label to the list
                                boundingBoxLabels.add(selectedLabel);
                                imageView.performClick();
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

    }

    private void deleteLastBoundingBox() {
        List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);
        // Remove the last drawn bounding box

        if (existingBoundingBoxes.isEmpty()) {
            // No bounding boxes to delete, show a message
            Toast.makeText(this, "No bounding boxes to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("EditImageActivity", "exisiting boundingBOX u823y94 before delete" + existingBoundingBoxes);
        BoundingBox lastBoundingBox = existingBoundingBoxes.get(existingBoundingBoxes.size() - 1);

        long lastBoundingBoxId = lastBoundingBox.getId();
        Log.d("Edit Image Activity", "deleteLastBoundingBox: lastBoundingBox = " + lastBoundingBoxId);

        dbHelper.deleteBoundingBoxById(lastBoundingBoxId);
        imageView.removeLastBoundingBox();
        existingBoundingBoxes.remove(existingBoundingBoxes.size() - 1);
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

        // Save all bounding boxes information to the bboxes table
        //dbHelper.insertBoundingBoxes(imageId,boundingBoxLabels, imageView.getBoundingBoxes());
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
        // get the image width and height from the database based on the project ID
        int imageWidth = dbHelper.getImageWidth(projectId);
        int imageHeight = dbHelper.getImageHeight(projectId);
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(imageWidth, imageHeight) // crop the image with user specified aspect ratio
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

    private int getLabelColor(String label, long projectId) {
        // Get the list of labels for the project from the database
        List<String> projectLabels = dbHelper.getLabelsForProject(projectId);

        // Find the index of the label in the projectLabels list
        int labelIndex = projectLabels.indexOf(label);
        Log.d("IMAGEDETAILSACTVIITY TEST", "getLabelColor: INDEX ="+labelIndex +"project labels" + projectLabels);

        // Assign colors based on the index
        switch (labelIndex) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.GREEN;
            case 2:
                return Color.RED;
            case 3:
                return Color.YELLOW;
            case 4:
                return Color.rgb(0, 128, 128);
            case 5:
                return Color.rgb(0, 100, 0);
            case 6:
                return Color.rgb(255, 165, 0);
            case 7:
                return Color.rgb(255, 20, 147);
            case 8:
                return Color.rgb(0, 0, 128); // Navy
            case 9:
                return Color.rgb(128, 0, 0);
            case 10:
                return Color.rgb(169, 169, 169);
            // Add more cases as needed
            default:
                // Use a default color for labels beyond the defined cases
                return Color.rgb(169, 169, 169); // Dark Gray
        }
    }

    private void setBoundingBoxImageViewSize(long projectId) {
        DBHelper dbHelper = new DBHelper(this);

        // Fetch the image width and height from the database based on the project ID
        int imageWidth = dbHelper.getImageWidth(projectId);
        int imageHeight = dbHelper.getImageHeight(projectId);

        // Find the BoundingBoxImageView in your layout
        BoundingBoxImageView imageView = findViewById(R.id.imageView);

        // Set the dimensions of the BoundingBoxImageView programmatically
        imageView.getLayoutParams().width = imageWidth;
        imageView.getLayoutParams().height = imageHeight;
        imageView.requestLayout(); // Ensure the changes take effect
    }

}