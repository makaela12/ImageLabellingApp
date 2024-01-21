package com.example.imagelabellingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class EditImageActivity extends AppCompatActivity {
    private List<String> boundingBoxLabels = new ArrayList<>();

    private BoundingBoxImageView imageView;
    private Button saveButton, addButton, deleteButton;
    private long imageId;
    private Spinner labelSpinner;
    private long projectId;
    private String imagePath;
    private String selectedLabel;
    private float[] boundingBox = new float[4];
    private DBHelper dbHelper;
    private ImageButton recropButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details); // Use the same layout as ImageDetailsActivity

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        saveButton = findViewById(R.id.saveButton);
        addButton = findViewById(R.id.addButton);
        deleteButton = findViewById(R.id.deleteButton);
        labelSpinner = findViewById(R.id.labelSpinner);
        recropButton = findViewById(R.id.recropButton);
        dbHelper = new DBHelper(this);

        // Retrieve image details from the intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1);
        imageId = dbHelper.getImageIdFromPath(imagePath);
        Log.d("EditImage", "Imageid " + imageId);

        // Load existing bounding boxes for the image
        List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);
        Log.d("EditImage", "loaded existing bboxes " + existingBoundingBoxes);
        Log.d("EditImage", "CURRENT LABELS ON CREATE" + boundingBoxLabels);

        // Update label names in existing bounding boxes, incase any label names have been edited
        updateBoundingBoxLabels(existingBoundingBoxes);
        imageView.setBoundingBoxes(existingBoundingBoxes);
        // Manually trigger a redraw of the BoundingBoxImageView
        imageView.invalidate();
        loadImageIntoImageView(imagePath);
        // Set up label spinner
        loadLabels();
        selectedLabel = labelSpinner.getSelectedItem().toString();

        // Inside onCreate method
        deleteButton.setOnClickListener(v -> deleteLastBoundingBox());
        // Set click listener for the saveButton
        saveButton.setOnClickListener(v -> saveImageDetails());

        String og_image_path = dbHelper.getOriginalImagePath(imagePath);

        recropButton.setOnClickListener(v -> startImageCropper(og_image_path));

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
                            long lid = dbHelper.getLabelIdForProjectAndLabel(projectId,selectedLabel);
                            Log.d("EditImage", "" + lid);
                            long bbox_id = dbHelper.insertBoundingBox(imageId, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], selectedLabel,projectId,lid);
                            long label_id = dbHelper.getLabelIdForBoundingBox(selectedLabel,bbox_id);
                            imageView.addBoundingBox(boundingBox, selectedLabel, bbox_id, label_id);
                            // Add the corresponding label to the list
                            boundingBoxLabels.add(selectedLabel);
                            // Disallow further touch events until "Add" button is pressed
                            //imageView.setAllowTouch(false);
                            addButton.setEnabled(true);  // Enable the "Add" button
                            imageView.performClick();
                            Log.d("EditImage", "CURRENT LABELS after adding boundingbox" + boundingBoxLabels);
                            // Handle touch up event

                            break;
                    }
                    return true;
                }
            } else {
                // Display a message to the user that a label must be selected
                Toast.makeText(EditImageActivity.this, "Select a label from the spinner first", Toast.LENGTH_SHORT).show();
            }
            return false; // Ignore touch events
        });

    }

    private void deleteLastBoundingBox() {
        List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);
        // Remove the last drawn bounding box
        Log.d("EditImageActivity", "exisiting boundingBOX u823y94 before delete" + existingBoundingBoxes);
        BoundingBox lastBoundingBox = existingBoundingBoxes.get(existingBoundingBoxes.size() - 1);

        long lastBoundingBoxId = lastBoundingBox.getId();
        Log.d("Edit Image Activity", "deleteLastBoundingBox: lastBoundingBox = " + lastBoundingBoxId);

        dbHelper.deleteBoundingBoxById(lastBoundingBoxId);

        imageView.removeLastBoundingBox();

        existingBoundingBoxes.remove(existingBoundingBoxes.size() - 1);

        // Remove the last label from the list
        if (!boundingBoxLabels.isEmpty()) {
            boundingBoxLabels.remove(boundingBoxLabels.size() - 1);
        }
        Log.d("EditImageActivity", "exisiting boundingBOX u823y94 after delete" + existingBoundingBoxes);

        Log.d("EditImage", "CURRENT LABELS after deletion" + boundingBoxLabels);

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
        finish();
    }

    private void startImageCropper(String imagePath) {
        CropImage.activity(Uri.fromFile(new File(imagePath)))
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                // Update the image path with the cropped image
                imagePath = result.getUri().getPath();
                if (imagePath != null) {
                    loadImageIntoImageView(imagePath);
                }
                // Clear existing bounding boxes as the image has changed
                //imageView.clearBoundingBoxes();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Error cropping image: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to update label names in existing bounding boxes
    private void updateBoundingBoxLabels(List<BoundingBox> boundingBoxes) {
        for (BoundingBox boundingBox : boundingBoxes) {
            String updatedLabel = dbHelper.getLabelNameById(boundingBox.getLabelId());
            boundingBox.setLabel(updatedLabel);
        }
    }

}
