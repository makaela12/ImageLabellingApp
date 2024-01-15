package com.example.imagelabellingapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import java.io.File;
import java.util.List;

public class EditImage extends AppCompatActivity implements BoundingBoxImageView.BoundingBoxListener{

    private static final int CROP_IMAGE_REQUEST_CODE = 203; // Define a custom request code
    //private ImageView imageView;
    private BoundingBoxImageView imageView;
    private TextView text1, text2;
    private String originalImagePath;
    private String imagePath;
    private DBHelper dbHelper;
    private long projectId;
    private Spinner labelSpinner;
    private Button saveButton;
    private long imageId;
    private String newLabel;

    private ImageButton helpButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);

        // Initialize UI elements
        imageView = findViewById(R.id.imageView);
        labelSpinner = findViewById(R.id.labelSpinner);
        saveButton = findViewById(R.id.saveButton);
        text1 = findViewById(R.id.textView);
        text2= findViewById(R.id.textView2);
        helpButton = findViewById(R.id.helpButton);


        // Initialize the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);

        // Enable the home button (back arrow)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set onClicklisteners for both help buttons
        helpButton.setOnClickListener(v -> showHelpPopup("To resize the bounding box,\ntap the side of the box\nyou would like to adjust\nand drag it to a new position\n\nTo move the bounding box,\ntap anywhere inside the box\nand drag it to a new position.\n\nTo change the label,\n select a new label from the list.\n\nClick the save button to\nsave your changes."));

        dbHelper = new DBHelper(this);

        // retrieve extras from intent
        imagePath = getIntent().getStringExtra("imagePath");
        originalImagePath = getIntent().getStringExtra("originalImagePath");
        projectId = getIntent().getLongExtra("projectId", -1); // -1 is the default value if not found

        // get original_image_path id
        //imageId = dbHelper.getImageIdFromOPath(originalImagePath);
        imageId = dbHelper.getImageIdFromPath(imagePath);

        Log.d("EditImage", "ImageId before drawBoundingBox: " + imageId);
        // Draw bounding box on the ImageView
        drawBoundingBox(imageId);

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

        // Set BoundingBoxListener to receive callbacks for saving bounding box coordinates
        imageView.setBoundingBoxListener(this);

        // set click listener for the save button
        saveButton.setOnClickListener(v -> {
            // Get the selected label from the spinner
            newLabel = labelSpinner.getSelectedItem().toString();
            Log.d("EditImage", "New label = " + newLabel);

            // Update the label_name in the database for the current image
            dbHelper.updateLabelForImage(imageId, newLabel); // Implement this method in your DBHelper

            // Save the final bounding box coordinates
            if (imageView != null) {
                imageView.saveBoundingBox();
            }

            Intent labelChangedIntent = new Intent("label_changed");
            labelChangedIntent.putExtra("imageId", imageId);
            labelChangedIntent.putExtra("newLabel", newLabel);
            sendBroadcast(labelChangedIntent);

            finish(); // Close the current activity
        });
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

    private void loadImageIntoImageView(String imagePath) {
        // Remove the leading "/file:" from the imagePath
        if (imagePath.startsWith("file://")) {
            imagePath = imagePath.substring(6);
        }
        // Create a File object from the imagePath
        File imageFile = new File(imagePath);

        // Log the file path to check if it's correct
        Log.d("EditImage", "Complete File Path: " + imageFile.getAbsolutePath());

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
                            Log.e("EditImage", "ImageId not found for path: " + croppedImagePath);
                        }
                    } else {
                        Log.e("EditImage", "Cropped image path is null");
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
                    Log.e("EditImage", "CropImage result is null");
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Add this method to draw bounding box on BoundingBoxImageView
    private void drawBoundingBox(long imageId) {
        Log.d("EditImage", "drawBoundingBox: Called for imageId = " + imageId);
        Cursor cursor = dbHelper.getBoundingBoxForImage(imageId);

        if (cursor != null && cursor.moveToFirst()) {
            float xMin = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_BBOX_X_MIN));
            float yMin = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_BBOX_Y_MIN));
            float xMax = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_BBOX_X_MAX));
            float yMax = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_BBOX_Y_MAX));

            Log.d("EditImage", "BoundingBox Coordinates: xMin=" + xMin + ", yMin=" + yMin + ", xMax=" + xMax + ", yMax=" + yMax);

            // Create a float array with bounding box coordinates
           float[] boundingBox = {xMin, yMin, xMax, yMax};

            // Call drawBoundingBox on BoundingBoxImageView
            imageView.drawBoundingBox(boundingBox);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    // Implement the onSaveBoundingBox method from BoundingBoxListener
    @Override
    public void onSaveBoundingBox(float xMin, float yMin, float xMax, float yMax) {
        // Save the bounding box coordinates to the database
        // You should implement this method in your DBHelper
        dbHelper.updateBoundingBoxCoordinates(imageId, xMin, yMin, xMax, yMax);
    }


}

