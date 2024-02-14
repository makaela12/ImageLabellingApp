package com.example.imagelabellingapp;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity2 extends AppCompatActivity {
    ImageButton selectButton, takeButton, helpButton, exportButton;
    Bitmap bitmap;
    private ListView imageListView;
    private ImageAdapter imageAdapter;
    private
    DBHelper dbHelper;
    private long projectId;
    private BroadcastReceiver newImageSavedReceiver;
    private BroadcastReceiver labelChangedReceiver;
    private static final int REQUEST_IMAGE_DETAILS = 203;
    // Declare variables at the beginning of onActivityResult
    long imageId; // Initialize with a default value
    String selectedLabel;
    String originalImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        dbHelper = new DBHelper(this);
        // initalize UI elements
        selectButton = findViewById(R.id.selectButton);
        takeButton = findViewById(R.id.takeButton);
        imageListView = findViewById(R.id.imageListView);
        helpButton = findViewById(R.id.helpButton);
        exportButton = findViewById(R.id.exportButton);

        // set onClickListener for help button
        helpButton.setOnClickListener(v -> showHelpPopup("Add Image\nTo add an image to your project, click the 'Capture' or 'Import' buttons below." +
                "\n\nDelete Image\nTo delete an image from your project, click and hold on the image you would like to delete." +
                "\n\nResize Image\nTo resize the image or add/delete bounding boxes, simply click on the image to be redirected to the image editing page."+
                "\n\nEdit Project\nTo edit your project details, click the menu icon (i.e., the three dots) in the top right corner and select \"Edit Project\"."+
                "\n\nView Project Details\nTo view your project details, click the menu icon (i.e., the three dots) in the top right corner and select \"Project Details\"."+
                "\n\nExport Project\nTo export your project, click the 'Export' icon below."));

        // Check if there is saved instance state; allows the ListView with contents to show
        if (savedInstanceState != null) {
            // Restore necessary data
            projectId = savedInstanceState.getLong("projectId");
        } else {
            // Handle the normal creation of the activity
            Intent intent = getIntent();
            if (intent.hasExtra("projectId")) {
                projectId = intent.getLongExtra("projectId", -1);
            }
        }
        // permission to let the user access the camera
        getPermission();

        // Initialize and set up the image ListView
        setupImageListView();

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the home button (back button)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Inflate the menu
        toolbar.inflateMenu(R.menu.menu_main);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (labelNamesExistForProject()) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 10);
                } else {
                    showLabelErrorDialog();
                }
            }
        });

        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (labelNamesExistForProject()) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, 12);
                } else {
                    showLabelErrorDialog();
                }
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showExportDialog();
            }
        });

        // Initialize the BroadcastReceiver
        newImageSavedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Refresh the data in the imageAdapter
                setupImageListView();
            }
        };
        // Register the BroadcastReceiver to listen for the "new_image_saved" broadcast
        registerReceiver(newImageSavedReceiver, new IntentFilter("new_image_saved"));

        // Initialize the BroadcastReceiver for label changes
        labelChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long changedImageId = intent.getLongExtra("imageId", -1);
                String newLabel = intent.getStringExtra("newLabel");

                // Update the label in the imageAdapter
                imageAdapter.setSelectedLabel(changedImageId, newLabel);
                imageAdapter.notifyDataSetChanged();
            }
        };

        // Register the BroadcastReceiver to listen for the "label_changed" broadcast
        registerReceiver(labelChangedReceiver, new IntentFilter("label_changed"));


        // Set up menu item click listener
        toolbar.setOnMenuItemClickListener(item -> {
            // handles edit project action
            if (item.getItemId() == R.id.menu_edit_project) {
                if (projectId != -1) {
                    Intent editProjectIntent = new Intent(MainActivity2.this, EditProjectActivity.class);
                    editProjectIntent.putExtra("projectId", projectId);
                    startActivity(editProjectIntent);
                    //startActivityForResult(editProjectIntent, REQUEST_CODE);
                } else {
                    Log.e("MainActivity2", "Invalid projectId: " + projectId);
                }
                return true;
            }
            if (item.getItemId() == R.id.menu_project_details) {
                if (projectId != -1) {
                    Intent projectDetailsIntent = new Intent(MainActivity2.this, projectDetails.class);
                    projectDetailsIntent.putExtra("projectId", projectId);
                    startActivity(projectDetailsIntent);
                    //startActivityForResult(editProjectIntent, REQUEST_CODE);
                } else {
                    Log.e("MainActivity2", "Invalid projectId: " + projectId);
                }
                return true;
            }
            return super.onOptionsItemSelected(item);
        });
    }

    // method to show export dialog
    private void showExportDialog() {
        // Inflate the layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.export_dialog, null);

        // Retrieve project details
        String projectName = dbHelper.getProjectName(projectId);
        List<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

        // Set image count
        int imageCount = imagePaths.size();

        // Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Labelling Format");
        builder.setView(dialogView);
        //builder.setMessage(projectName + ".zip\n\nName                                       Type\n---------------------------------------------------------------\nclasses.txt                                  Plain Text\nimages                                              Folder\nlabels                                                 Folder\n---------------------------------------------------------------\nNo. of Images: " + imageCount);
        builder.setMessage("Project Name: " + projectName + "\nNo. of Images: " + imageCount);
        // Find the spinner in the dialog layout
        Spinner exportFormatSpinner = dialogView.findViewById(R.id.exportFormatSpinner);

        // Create an array adapter for the spinner with YOLO and COCO options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"YOLO (You Only Look Once)", "COCO (Common Objects in Context)"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exportFormatSpinner.setAdapter(adapter);

        builder.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle export button click based on the selected format
                String selectedFormat = exportFormatSpinner.getSelectedItem().toString();
                if ("YOLO (You Only Look Once)".equals(selectedFormat)) {
                    exportProjectYOLO();
                } else if ("COCO (Common Objects in Context)".equals(selectedFormat)) {
                    exportProjectCOCO();
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle cancel button click
                dialogInterface.dismiss();
            }
        });

        // Show the alert dialog
        builder.show();
    }

    // Check if label names exist for the current project
    private boolean labelNamesExistForProject() {
        // Retrieve label names for the current project using dbHelper
        List<String> labelNames = dbHelper.getLabelsForProject(projectId);

        // Return true if at least one label name exists
        return labelNames != null && !labelNames.isEmpty();
    }
    private void showLabelErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("Labels must be defined before adding images.\n\nDo you want to create labels now?");
        builder.setPositiveButton("Create Labels", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Start EditProjectActivity for label creation
                Intent intent = new Intent(MainActivity2.this, EditProjectActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing (stay on MainActivity2)
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity2", "onCreateOptionsMenu called");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d("MainActivity2", "Menu items count: " + menu.size());
        return true;
    }


    @Override
    protected void onDestroy() {
        // Unregister the BroadcastReceiver when the activity is destroyed
        unregisterReceiver(newImageSavedReceiver);
        unregisterReceiver(labelChangedReceiver);
        super.onDestroy();
    }

    void getPermission() {
        // Asks the user for permission to access the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Allows access to the user's camera to take a photo if the user gave access permission
        if (requestCode == 11) {
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity2", "onActivityResult called with requestCode: " + requestCode);

        if (data != null && data.getExtras() != null) {
            Bundle extras = data.getExtras();
            for (String key : extras.keySet()) {
                Log.d("MainActivity2", "Extra key: " + key + ", Extra value: " + extras.get(key));
            }
        }
        Uri resultUri = null;

        // if the user wants to select an image from their photo gallery
        if (requestCode == 10 && resultCode == RESULT_OK) {
            // retrieve the URI of the selected image
            Uri uri = data.getData();
            // Get the originalImagePath for gallery-selected images
            originalImagePath = getOriginalImagePathFromUri(uri);
            // continue with the cropping function
            startCropActivity(uri);
            // if the user wants to capture an image with the camera
        } else if (requestCode == 12 && resultCode == RESULT_OK) {
            // get the captured image bitmap from the extras
            bitmap = (Bitmap) data.getExtras().get("data");
            // Get the originalImagePath for camera-captured images
            originalImagePath = getOriginalImagePathFromBitmap(bitmap);
            // continue with cropping function
            startCropActivity(getImageUri(this, bitmap));

        }

        // After the image is cropped
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            // retrieve the result of the image cropping activity
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            // check if the cropping was successful
            if (result != null) {
                if (resultCode == RESULT_OK) {
                    // gets the URI of the cropped image
                    resultUri = result.getUri();
                    // Save the cropped image to a file and get the imageId
                    imageId = saveCroppedImageToFile(resultUri, projectId, selectedLabel, originalImagePath);
                    // Start ImageDetailsActivity
                    openImageDetailsActivity(resultUri.toString(),resultUri);

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                    if (error != null) {
                        error.printStackTrace();
                    }
                }
            } else {
                // Handle the case where CropImage result is null
                Log.e("MainActivity2", "CropImage result is null");
            }
        }
            // Handle the result from ImageDetailsActivity
            if (requestCode == REQUEST_IMAGE_DETAILS && resultCode == RESULT_OK) {
                // Handle the result data, which contains the selected label
                String selectedLabel = data.getStringExtra("selectedLabel");
                long imageId = data.getLongExtra("imageId", -1);

                // Pass the selected label to the ImageAdapter
                Log.d("MainActivity2", "ImageId: " + imageId + ", SelectedLabel: " + selectedLabel);
                imageAdapter.setSelectedLabel(imageId, selectedLabel);
                imageAdapter.notifyDataSetChanged();
            }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // Method to get the original image path from URI for gallery-selected images
    private String getOriginalImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String originalImagePath = cursor.getString(columnIndex);
            cursor.close();
            return originalImagePath;
        }
        return null;
    }

    // Method to get the original image path from Bitmap for camera-captured images
    private String getOriginalImagePathFromBitmap(Bitmap bitmap) {
        // Save the original image to a temporary file
        try {
            File cacheDir = getCacheDir();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "original_" + timestamp + ".jpg";
            File imageFile = new File(cacheDir, filename);

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to get the image URI from the bitmap
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private long saveCroppedImageToFile(Uri resultUri, long projectId, String selectedLabel, String originalImagePath) {
        // Ensure that projectId is not -1
        if (projectId != -1) {
            try {
                Bitmap croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                // Save the cropped image to a file in the cache directory
                File cacheDir = getCacheDir();
                String timestamp = String.valueOf(System.currentTimeMillis());
                String filename = "image_" + timestamp + ".jpg";
                File imageFile = new File(cacheDir, filename);

                FileOutputStream fos = new FileOutputStream(imageFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                // Set imagePath to the absolute path of the saved image
                String imagePath = imageFile.getAbsolutePath();
                // Save the image path to the images table and get the imageId
               long imageId = dbHelper.insertImagePath(originalImagePath, imagePath, projectId);

                Log.e("CORRECT IMAGEID?????", "Invalid projectId: " + imageId);

                // Notify that a new image has been saved
                sendBroadcast(new Intent("new_image_saved"));

                // Update the ListView
                updateListView();

                return imageId;

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where projectId is -1
            Log.e("MainActivity2", "Invalid projectId: " + projectId);
        }
        return -1;
    }

    private void updateListView() {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> updatedImagePaths = dbHelper.getImagePathsForProject(projectId);

            // Retrieve list of labels for a specific project using getLabelsForProject method from dbHelper instance
            List<String> labels = dbHelper.getLabelsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(this, R.layout.image_list_item, updatedImagePaths, dbHelper);
                imageListView.setAdapter(imageAdapter);
            } else {
                // Clear the existing data in the adapter
                imageAdapter.clear();
                // Add all the new image paths
                imageAdapter.addAll(updatedImagePaths);
            }
            // Notify the adapter that the data has changed
            imageAdapter.notifyDataSetChanged();
        }


    private void startCropActivity(Uri sourceUri) {
        // get the image width and height from the database based on the project ID
        int imageWidth = dbHelper.getImageWidth(projectId);
        int imageHeight = dbHelper.getImageHeight(projectId);
        CropImage.activity(sourceUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(imageWidth, imageHeight) // crop the image with user specified aspect ratio
                .start(this);
    }

    private void setupImageListView() {
        runOnUiThread(() -> {
            // Retrieve list of image paths for a specific project using getImagePathsForProject method from dbHelper instance
            ArrayList<String> imagePaths = dbHelper.getImagePathsForProject(projectId);

            // Retrieve list of labels for a specific project using getLabelsForProject method from dbHelper instance
            List<String> labels = dbHelper.getLabelsForProject(projectId);

            // If the adapter is not set, initialize it
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(this, R.layout.image_list_item, imagePaths, dbHelper);
                imageListView.setAdapter(imageAdapter);
            } else {
                // Clear the existing data in the adapter
                imageAdapter.clear();
                // Add all the new image paths
                imageAdapter.addAll(imagePaths);
            }

            // Notify the adapter that the data has changed
            imageAdapter.notifyDataSetChanged();

            // Set click listener for the ListView items
            imageListView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedImagePath = imageAdapter.getItem(position);
                String originalImagePath = getOriginalImagePath(selectedImagePath);
                openEditImage(selectedImagePath, originalImagePath);
            });

            // Set long-click listener for the ListView items to handle deletions
            imageListView.setOnItemLongClickListener((parent, view, position, id) -> {
                // Display a confirmation dialog
                showDeleteImageDialog(position);
                return true; // Consume the long-click event
            });
        });
    }

    // method to start the ImageDetailsActivity
    private void openImageDetailsActivity(String imagePath, Uri resultUri) {
        Log.d("MainActivity2", "Opening ImageDetailsActivity with Image Path: " + imagePath);
        Intent intent = new Intent(MainActivity2.this, ImageDetailsActivity.class);
        intent.putExtra("imagePath", imagePath);
        intent.putExtra("originalImagePath", originalImagePath);
        intent.putExtra("projectId", projectId);
        intent.putExtra("imageId", imageId);
        intent.putExtra("selectedLabel", selectedLabel);
        intent.putExtra("imageUri", resultUri.toString());
        startActivityForResult(intent, REQUEST_IMAGE_DETAILS);
    }

    // method to start the EditImage activity
    private void openEditImage(String imagePath, String originalImagePath) {
        Log.d("MainActivity2", "Opening EditImage activity with Image Path: " + imagePath);
        Intent editImageIntent = new Intent(MainActivity2.this, EditImageActivity.class);
        editImageIntent.putExtra("imagePath", imagePath);
        editImageIntent.putExtra("originalImagePath", originalImagePath);
        editImageIntent.putExtra("imageIdh", imageId);
        editImageIntent.putExtra("projectId", projectId);
        editImageIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(editImageIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("projectId", projectId);
        outState.putString("selectedLabel", selectedLabel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Handle the back button click (navigate up)
                onBackPressed();
                navigateToSelectProject();
                return true;
            // Add other menu item cases if needed
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // used to refresh the project list in selectProject after making changes to project name
    @Override
    protected void onResume() {
        super.onResume();
        setupImageListView();
        // Refresh the project list in selectProject activity
        refreshProjectListInSelectProject();
        // Register a BroadcastReceiver to listen for data changes
        IntentFilter intentFilter = new IntentFilter("data_changed");
        BroadcastReceiver dataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Update the ListView when data changes
                updateListView();
                setupImageListView();
            }
        };
        registerReceiver(dataChangedReceiver, intentFilter);
    }

    private void refreshProjectListInSelectProject() {
        // Send a broadcast to notify selectProject activity to refresh the project list
        sendBroadcast(new Intent("refresh_project_list"));
    }

    private void navigateToSelectProject() {
        // Navigate to the selectProject activity
        Intent intent = new Intent(MainActivity2.this, selectProject.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private String getOriginalImagePath(String selectedImagePath) {
        // Use your DBHelper to query the original image path from the database
        String originalImagePath = dbHelper.getOriginalImagePath(selectedImagePath);
        return originalImagePath;
    }

    private void showDeleteImageDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Image");
        builder.setMessage("Are you sure you want to delete this image?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the image path from the adapter based on the position
                String selectedImagePath = imageAdapter.getItem(position);
                // Retrieve the corresponding image ID from the database
                long imageID = dbHelper.getImageIdFromPath(selectedImagePath);
                // Delete the corresponding row in the images database
                dbHelper.deleteImage(projectId, imageID);
                // Remove the item from the adapter
                imageAdapter.remove(selectedImagePath);
                imageAdapter.notifyDataSetChanged();
                // Delete the image file from storage
                deleteImageFile(selectedImagePath);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
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


    private void exportProjectYOLO() {
        // Get project details
        String projectName = dbHelper.getProjectName(projectId);
        List<String> imagePaths = dbHelper.getImagePathsForProject(projectId);
        List<String> labelNames = dbHelper.getLabelsForProject(projectId);

        // Create a zip file
        File exportZipFile = new File(getExternalFilesDir(null), projectName + ".zip");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(exportZipFile))) {
            // Create 'labels' folder
            zipOutputStream.putNextEntry(new ZipEntry("labels/"));
            zipOutputStream.closeEntry();

            // Create 'images' folder
            zipOutputStream.putNextEntry(new ZipEntry("images/"));
            zipOutputStream.closeEntry();

            // Create 'classes.txt' file
            zipOutputStream.putNextEntry(new ZipEntry("classes.txt"));

            for (String label : labelNames) {
                zipOutputStream.write((label + "\n").getBytes());
            }
            zipOutputStream.closeEntry();

            // Process each image
            for (String imagePath : imagePaths) {
                // Retrieve bounding box information from the database
                List<float[]> boundingBoxes = dbHelper.getBoundingBoxesForExport(dbHelper.getImageIdFromPath(imagePath));

                // Prepare YOLO label file name
                String labelFileName = getFileNameWithoutExtension(imagePath) + ".txt";
                zipOutputStream.putNextEntry(new ZipEntry("labels/" + labelFileName));

                // Process each bounding box for the current image
                for (float[] boundingBox : boundingBoxes) {
                    long i_id = dbHelper.getImageIdFromPath(imagePath);
                    long labelId = dbHelper.getLabelIdForBBox(i_id, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);

                    Log.d("*******IMAGE ID", "exportProjectYOLO: "+ i_id + " ");
                    Log.d("*******LABEL ID", "exportProjectYOLO: "+ labelId + " ");
                    // Load the image dimensions
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imagePath, options);
                    int imageWidth = dbHelper.getImageWidth(projectId);
                    int imageHeight = dbHelper.getImageHeight(projectId);
                    Log.d("*******HEIGHT", "exportProjectYOLO: "+ imageHeight + " ");
                    Log.d("*******WIDTH", "exportProjectYOLO: "+ imageWidth + " ");
                    Log.d("BoundingBox Coords", "exportProjectYOLO: "+ boundingBox[0] + " "+ boundingBox[1] + " "+ boundingBox[2] + " "+ boundingBox[3] + " ");
                    // Normalize the bounding box coordinates
                    float nX1 = boundingBox[0] / imageWidth;
                    float nY1 = boundingBox[1] / imageHeight;
                    float nX2 = boundingBox[2] / imageWidth;
                    float nY2 = boundingBox[3] / imageHeight;
                    Log.d("BoundingBox CoordsAFTER*****", "exportProjectYOLO: "+ nX1 + " "+ nY1 + " "+ nX2 + " "+ nY2 + " ");

                    // Normalize the bounding box coordinates
                    float normalizedXCenter = (nX1 + nX2) / 2;
                    float normalizedYCenter = (nY1 + nY2) / 2;
                    float normalizedWidth = nX2 - nX1;
                    float normalizedHeight = nY2 - nY1;

                  /*  // Normalize the bounding box coordinates
                    float normalizedXCenter = (boundingBox[0] + boundingBox[2]) / (2 * imageWidth);
                    float normalizedYCenter = (boundingBox[1] + boundingBox[3]) / (2 * imageHeight);
                    float normalizedWidth = (boundingBox[2] - boundingBox[0]) / imageWidth;
                    float normalizedHeight = (boundingBox[3] - boundingBox[1]) / imageHeight;*/
                    Log.d("NORMALIZED COORDS", "exportProjectYOLO: "+ normalizedXCenter + " "+ normalizedYCenter + " "+ normalizedWidth + " "+ normalizedHeight + " ");

                    // Write YOLO format annotation for the current bounding box
                    String yoloAnnotation = String.format(Locale.US, "%d %.6f %.6f %.6f %.6f\n",
                            labelId,
                            normalizedXCenter,
                            normalizedYCenter,
                            normalizedWidth,
                            normalizedHeight);
                    zipOutputStream.write(yoloAnnotation.getBytes());
                }

                // Close the YOLO label file entry
                zipOutputStream.closeEntry();

                // Copy image file
                String imageFileName = getFileNameWithoutExtension(imagePath) + ".jpg";
                zipOutputStream.putNextEntry(new ZipEntry("images/" + imageFileName));
                zipOutputStream.write(getFileBytes(new File(imagePath)));
                zipOutputStream.closeEntry();
            }

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error exporting project", Toast.LENGTH_SHORT).show();
            });
        }
        // Create a URI for the exported ZIP file
        Uri uri = FileProvider.getUriForFile(this, "com.example.imagelabellingapp.fileprovider", exportZipFile);

        // Create a sharing intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);


        // Check if there is any application that can handle the sharing intent
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            // Start the sharing activity
            startActivity(Intent.createChooser(shareIntent, "Share via..."));
        } else {
            // Display a message that no app can handle the sharing intent
            Toast.makeText(this, "No app can handle the sharing intent", Toast.LENGTH_SHORT).show();
        }

    }
    private void exportProjectCOCO() {
        // Get project details
        String projectName = dbHelper.getProjectName(projectId);
        List<String> imagePaths = dbHelper.getImagePathsForProject(projectId);
        List<String> labelNames = dbHelper.getLabelsForProject(projectId);

        try {

            // Create COCO JSON structure
            JSONObject cocoJson = new JSONObject();

            // Create 'info' section
            JSONObject info = new JSONObject();
            info.put("description", "Your project description");
            info.put("url", "Your project URL");
            info.put("version", "1.0");
            info.put("year", 2024);
            info.put("date_created", getCurrentDate());
            cocoJson.put("info", info);


            // Create 'categories' section
            JSONArray categories = new JSONArray();
            for (int i = 0; i < labelNames.size(); i++) {
                JSONObject category = new JSONObject();
                category.put("id", i + 1); // IDs start from 1 in COCO format
                category.put("name", labelNames.get(i));
                category.put("supercategory", "object");
                categories.put(category);
            }
            cocoJson.put("categories", categories);

            // Create 'images' section
            JSONArray images = new JSONArray();
            for (String imagePath : imagePaths) {
                JSONObject image = new JSONObject();
                image.put("id", images.length() + 1); // IDs start from 1 in COCO format
                image.put("width", getImageWidth(imagePath));
                image.put("height", getImageHeight(imagePath));
                image.put("file_name", getFileNameWithoutExtension(imagePath));
                images.put(image);
            }
            cocoJson.put("images", images);

            // Create 'annotations' section
            JSONArray annotations = new JSONArray();
            for (String imagePath : imagePaths) {
                float[] boundingBox = dbHelper.getBoundingBoxForExport(dbHelper.getImageIdFromPath(imagePath));
                long i_id = dbHelper.getImageIdFromPath(imagePath);
                String label = dbHelper.getLabelNameForBoundingBox(i_id,boundingBox[0],boundingBox[1],boundingBox[2],boundingBox[3]);

                JSONObject annotation = new JSONObject();
                annotation.put("id", annotations.length() + 1); // IDs start from 1 in COCO format
                annotation.put("image_id", getImageId(imagePath, images));
                annotation.put("category_id", labelNames.indexOf(label) + 1); // IDs start from 1 in COCO format
                annotation.put("area", calculateArea(boundingBox));
                annotation.put("bbox", new JSONArray(boundingBox)); // x, y, width, height
                annotations.put(annotation);
            }
            cocoJson.put("annotations", annotations);

            // Save the JSON to a file
            File exportJsonFile = new File(getExternalFilesDir(null), projectName + "_coco.json");
            try (FileWriter fileWriter = new FileWriter(exportJsonFile)) {
                fileWriter.write(cocoJson.toString());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error exporting project in COCO format", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Create a URI for the exported JSON file
            Uri uri = FileProvider.getUriForFile(this, "com.example.imagelabellingapp.fileprovider", exportJsonFile);

            // Create a sharing intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            // Change the intent type to "text/plain"
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

            // Check if there is any application that can handle the sharing intent
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                // Start the sharing activity
                startActivity(Intent.createChooser(shareIntent, "Share via..."));
            } else {
                // Display a message that no app can handle the sharing intent
                Toast.makeText(this, "No app can handle the sharing intent", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error exporting project in COCO format: JSON error", Toast.LENGTH_SHORT).show();
            });
        }
    }


    // Helper method to get image width
    private int getImageWidth(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        return options.outWidth;
    }

    // Helper method to get image height
    private int getImageHeight(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        return options.outHeight;
    }

    // Helper method to get the current date
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    // Helper method to calculate area from bounding box
    private float calculateArea(float[] boundingBox) {
        return (boundingBox[2] - boundingBox[0]) * (boundingBox[3] - boundingBox[1]);
    }

    // Helper method to get image ID from image path
    private int getImageId(String imagePath, JSONArray images) {
        try {
            String fileName = getFileNameWithoutExtension(imagePath);
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = images.getJSONObject(i);
                if (image.getString("file_name").equals(fileName)) {
                    return image.getInt("id");
                }
            }
            return -1; // Image ID not found
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Error getting image ID: JSON error", Toast.LENGTH_SHORT).show();
            });
            return -1;
        }
    }

    // Helper function to get file name without extension
    private String getFileNameWithoutExtension(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < fileName.length() - 1) {
            return fileName.substring(0, pos);
        } else {
            return fileName;
        }
    }

    // Helper function to get file bytes
    private byte[] getFileBytes(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private void deleteImageFile(String imagePath) {
        // Create a File object representing the image file
        File imageFile = new File(imagePath);

        // Check if the file exists before attempting to delete it
        if (imageFile.exists()) {
            // Delete the file
            boolean deleted = imageFile.delete();

            // Check if the file deletion was successful
            if (deleted) {
                // Log a message or perform any additional actions upon successful deletion
                Log.d("DeleteImage", "Image file deleted successfully: " + imagePath);
            } else {
                // Log an error message or handle the case where deletion failed
                Log.e("DeleteImage", "Failed to delete image file: " + imagePath);
            }
        } else {
            // Log a message or handle the case where the file does not exist
            Log.d("DeleteImage", "Image file does not exist: " + imagePath);
        }
    }

}