package com.example.imagelabellingapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private Button saveButton;
    private long imageId;
    private Spinner labelSpinner;
    private long projectId;
    private String imagePath;
    private String selectedLabel;
    private float[] boundingBox = new float[4];
    private DBHelper dbHelper;
    private ImageButton recropButton, deleteButton, helpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details); // Use the same layout as ImageDetailsActivity

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        helpButton = findViewById(R.id.helpButton);
        labelSpinner = findViewById(R.id.labelSpinner);
        recropButton = findViewById(R.id.recropButton);
        dbHelper = new DBHelper(this);

        // Initialize the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);

        // Enable the home button (back arrow)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the click listener for the back arrow
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the back arrow click
                onBackPressed();
            }
        });
        // set onClickListener for help button
        helpButton.setOnClickListener(v -> showHelpPopup("Add Bounding Box\nTo add a bounding box, first select a label to define the bounding box. Click the box above the image to view the list of label names. Once a desired label name is selected, press down and drag on the image in the direction you would like the box to be drawn.\n\nDelete Bounding Box\nClicking the 'trash can' icon below the image will delete the last bounding box drawn on the image.\n\nResize Image\nTo resize the image, select the 'crop' icon below the image."));

        // Retrieve image details from the intent
        imagePath = getIntent().getStringExtra("imagePath");
        projectId = getIntent().getLongExtra("projectId", -1);
        imageId = dbHelper.getImageIdFromPath(imagePath);
        Log.d("EditImage", "Imageid " + imageId);

        int imageWidth = dbHelper.getImageWidth(projectId);
        int imageHeight = dbHelper.getImageHeight(projectId);
       /* // Load existing bounding boxes for the image
        List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);

        Log.d("IMAGE VIEW W", "width" + imageView.getWidth());
        Log.d("IMAGE VIEW H", "height" + imageView.getHeight());
        // Multiply each coordinate by 2 for all existing bounding boxes
        for (BoundingBox bbox : existingBoundingBoxes) {
            float[] coordinates = bbox.getCoordinates();
            for (int i = 0; i < coordinates.length; i+=2) {
                Log.d("width coordinates BEFORE", "onCreate: " + coordinates[i]);
                Log.d("image width ", "onCreate: " + imageWidth);
                coordinates[i] = (coordinates[i]/imageWidth) * imageWidth;
                Log.d("width coordinates AFTER", "onCreate: " + coordinates[i]);
            }
            for (int i = 1; i < coordinates.length; i+=2) {
                coordinates[i] *= (coordinates[i]/imageHeight) * imageHeight;
                Log.d("height coordinates", "onCreate: " + coordinates[i]);
            }

        }
        Log.d("EditImage", "loaded existing bboxes " + existingBoundingBoxes);
        Log.d("EditImage", "CURRENT LABELS ON CREATE" + boundingBoxLabels);

        // Update label names in existing bounding boxes, incase any label names have been edited
        updateBoundingBoxLabels(existingBoundingBoxes);
        //updateBoundingBoxColors(existingBoundingBoxes);
        imageView.setBoundingBoxes(existingBoundingBoxes);
        // Manually trigger a redraw of the BoundingBoxImageView
        imageView.invalidate();

        */
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

            // Calculate the scale factors to map touch coordinates to image coordinates
            float scaleX = (float) imageWidth / (float) imageView.getWidth();
            float scaleY = (float) imageHeight / (float) imageView.getHeight();

            if (labelSpinner.getSelectedItem() != null) {
                String selectedLabel = labelSpinner.getSelectedItem().toString();
                int color = getLabelColor(selectedLabel, projectId);
                if (!imageView.hasBoundingBox() || imageView.isAllowTouch()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Handle touch down event
                            boundingBox = new float[4];
                            boundingBox[0] = ((event.getX() * scaleX)/imageWidth) * imageView.getWidth();
                            boundingBox[1] = ((event.getY() * scaleY)/imageHeight) * imageView.getHeight();
                            Log.d("getX1r", "onCreate: "+ boundingBox[0]);
                            Log.d("getY1r", "onCreate: "+ boundingBox[1]);
                            //boundingBox[0] = event.getX();
                           // boundingBox[1] = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // Handle touch move event
                            //boundingBox[2] = event.getX();
                           // boundingBox[3] = event.getY();
                            boundingBox[2] = ((event.getX() * scaleX)/imageWidth) * imageView.getWidth();
                            boundingBox[3] = ((event.getY() * scaleY)/imageHeight) * imageView.getHeight();
                            Log.d("getX2r", "onCreate: "+ boundingBox[2]);
                            Log.d("getY2r", "onCreate: "+ boundingBox[3]);
                            imageView.drawBoundingBox(boundingBox, selectedLabel, projectId,color);
                            break;
                        case MotionEvent.ACTION_UP:
                            long lid = dbHelper.getLabelIdForProjectAndLabel(projectId,selectedLabel);
                            Log.d("EditImage", "" + lid);
                            //long bbox_id = dbHelper.insertBoundingBox(imageId, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], selectedLabel,projectId,lid, color);
                            float x1 = (boundingBox[0]/imageView.getWidth()) * imageWidth;
                            float y1 = (boundingBox[1]/imageView.getHeight()) * imageHeight;
                            float x2 = (boundingBox[2]/imageView.getWidth()) * imageWidth;
                            float y2 = (boundingBox[3]/imageView.getHeight()) * imageHeight;
                            Log.d("x1 in DATABASE", "onCreate: "+ x1);
                            Log.d("y1 in DATABASE", "onCreate: "+ y1);
                            Log.d("x2 in DATABASE", "onCreate: "+ x2);
                            Log.d("y2 in DATABASE", "onCreate: "+ y2);

                            long bbox_id = dbHelper.insertBoundingBox(imageId, x1, y1, x2, y2, selectedLabel, projectId,lid, color);
                            long label_id = dbHelper.getLabelIdForBoundingBox(selectedLabel,bbox_id);
                            imageView.addBoundingBox(boundingBox, selectedLabel, bbox_id, label_id,color);
                            // Add the corresponding label to the list
                            boundingBoxLabels.add(selectedLabel);
                            imageView.performClick();
                            Log.d("EditImage", "CURRENT LABELS after adding boundingbox" + boundingBoxLabels);
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
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Load existing bounding boxes for the image
            List<BoundingBox> existingBoundingBoxes = dbHelper.getBoundingBoxesForImage(imageId);

            int imageWidth = dbHelper.getImageWidth(projectId);
            int imageHeight = dbHelper.getImageHeight(projectId);

            // Multiply each coordinate by the ratio of the ImageView dimensions to the image dimensions
            for (BoundingBox bbox : existingBoundingBoxes) {
                float[] coordinates = bbox.getCoordinates();

                // Adjust left and right coordinates
                coordinates[0] *= (float) imageView.getWidth() / imageWidth;
                coordinates[2] *= (float) imageView.getWidth() / imageWidth;

                // Adjust top and bottom coordinates
                coordinates[1] *= (float) imageView.getHeight() / imageHeight;
                coordinates[3] *= (float) imageView.getHeight() / imageHeight;
            }

            // Update label names in existing bounding boxes, in case any label names have been edited
            updateBoundingBoxLabels(existingBoundingBoxes);
            imageView.setBoundingBoxes(existingBoundingBoxes);
            imageView.invalidate(); // Manually trigger a redraw of the BoundingBoxImageView
        }
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
        // Remove the last label from the list
        if (!boundingBoxLabels.isEmpty()) {
            boundingBoxLabels.remove(boundingBoxLabels.size() - 1);
        }
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

    private void startImageCropper(String image) {
        // get the image width and height from the database based on the project ID
        int imageWidth = dbHelper.getImageWidth(projectId);
        int imageHeight = dbHelper.getImageHeight(projectId);
        CropImage.activity(Uri.fromFile(new File(image)))
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
                // Update the image path with the cropped image
                imagePath = result.getUri().getPath();
                if (imagePath != null) {
                    loadImageIntoImageView(imagePath);
                    //  imagePath to update the database
                    dbHelper.updateCroppedImagePath(imageId, imagePath);
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

    // Helper method to get color based on the order of the label for a project
    private int getLabelColor(String label, long projectId) {
        // Get the list of labels for the project from the database
        List<String> projectLabels = dbHelper.getLabelsForProject(projectId);

        // Find the index of the label in the projectLabels list
        int labelIndex = projectLabels.indexOf(label);
        Log.d("EDITIMAGEACTIVITY TEST", "getLabelColor: INDEX ="+labelIndex +"project labels" + projectLabels);

        // Assign colors based on the index
        switch (labelIndex) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.GREEN;
            case 2:
                return Color.RED;
            // Add more cases as needed
            default:
                // Use a default color for labels beyond the defined cases
                return Color.YELLOW;
        }
    }
    private void updateBoundingBoxColors(List<BoundingBox> boundingBoxes) {
        for (BoundingBox boundingBox : boundingBoxes) {
            String label = boundingBox.getLabel();
            int color = getLabelColor(label, projectId);
            boundingBox.setColor(color);
        }
    }

    private void showHelpPopup(String message){
        // creates a custom dialog
        Dialog helpDialog = new Dialog(this);
        helpDialog.setContentView(R.layout.popup_layout); // Create a layout for your popup

        // sets the message in the popup
        TextView popupMessage = helpDialog.findViewById(R.id.popupMessage);
        popupMessage.setText(message);

        // sets click listener for the close button in the popup
        ImageView closeButton = helpDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> helpDialog.dismiss());

        // show the popup
        helpDialog.show();
    }
}
