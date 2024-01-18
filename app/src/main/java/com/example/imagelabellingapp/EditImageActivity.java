package com.example.imagelabellingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

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
    private boolean changesMade = false;

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
                // Insert the bounding box into the database
                long boundingBoxId = dbHelper.insertBoundingBox(imageId, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], selectedLabel);

                if (boundingBoxId != -1) {
                    Toast.makeText(this, "Bounding box inserted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to insert bounding box", Toast.LENGTH_SHORT).show();
                }

                // Allow touch events on the image view
                imageView.setAllowTouch(true);
                changesMade = true;
            } else {
                // Handle the case where no bounding box is drawn
                Toast.makeText(this, "Draw a bounding box first", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void deleteLastBoundingBox() {
        List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);
        // Remove the last drawn bounding box
        Log.d("EditImageActivity", "exisiting boundingBOX u823y94 before delete" + existingBoundingBoxes);
        BoundingBox lastBoundingBox = existingBoundingBoxes.get(existingBoundingBoxes.size() - 1);
        long lastBoundingBoxId = lastBoundingBox.getId();
        imageView.removeLastBoundingBox();


        // Delete the bounding box from the database
        dbHelper.deleteBoundingBoxById(lastBoundingBoxId);

        existingBoundingBoxes.remove(existingBoundingBoxes.size() - 1);


        // Remove the last label from the list
        if (!boundingBoxLabels.isEmpty()) {
            boundingBoxLabels.remove(boundingBoxLabels.size() - 1);
        }
        // Enable the "Add" button
        addButton.setEnabled(false);
        imageView.setAllowTouch(true);
        changesMade = true;
        Log.d("EditImageActivity", "exisiting boundingBOX u823y94 after delete" + existingBoundingBoxes);

        // Delete the last bounding box from the database
        /*if (!existingBoundingBoxes.isEmpty()) {
            long lastBoundingBoxId = existingBoundingBoxes.get(existingBoundingBoxes.size() - 1).getId();
            dbHelper.deleteLastBoundingBox(lastBoundingBoxId);
            // Remove the last bounding box from the existingBoundingBoxes list
            existingBoundingBoxes.remove(existingBoundingBoxes.size() - 1);
        }*/
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
        // Get the current list of bounding boxes from your BoundingBoxImageView
        //List<BoundingBox> currentBoundingBoxes = imageView.getBoundingBoxes();
       // Log.d("EditImage", "****!?!?loaded current bboxes " + currentBoundingBoxes);
        Log.d("EditImage", "CURRENT LABELS " + boundingBoxLabels);

        if (changesMade) {
            //dbHelper.updateBoundingBoxesForImage(imageId, currentBoundingBoxes, boundingBoxLabels);
            Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
        }
        finish();
    }



}
